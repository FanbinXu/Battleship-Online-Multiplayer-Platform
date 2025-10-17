package app.battleship.service;

import app.battleship.model.*;
import app.battleship.persist.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameService {
    
    private final StringRedisTemplate redis;
    private final GameRepository gameRepository;
    private final GameSnapshotRepository snapshotRepository;
    private final ShipPlacementService shipPlacementService;
    private final ObjectMapper objectMapper;
    
    public GameService(StringRedisTemplate redis, GameRepository gameRepository, 
                      GameSnapshotRepository snapshotRepository,
                      ShipPlacementService shipPlacementService,
                      ObjectMapper objectMapper) {
        this.redis = redis;
        this.gameRepository = gameRepository;
        this.snapshotRepository = snapshotRepository;
        this.shipPlacementService = shipPlacementService;
        this.objectMapper = objectMapper;
    }
    
    public GameState initializeGame(String gameId, String roomId, String firstPlayerId, String secondPlayerId) {
        GameState state = new GameState(gameId, roomId, firstPlayerId, secondPlayerId);
        
        // Place ships for both players
        List<Ship> firstPlayerShips = shipPlacementService.placeShipsRandomly();
        List<Ship> secondPlayerShips = shipPlacementService.placeShipsRandomly();
        
        state.getPlayers().get(firstPlayerId).getBoard().setShips(firstPlayerShips);
        state.getPlayers().get(secondPlayerId).getBoard().setShips(secondPlayerShips);
        
        // Save to Redis and create snapshot
        saveGameState(state);
        createSnapshot(state);
        
        return state;
    }
    
    public GameState getGameState(String gameId) {
        // Try to get from Redis first
        String key = "game:" + gameId + ":state";
        String json = redis.opsForValue().get(key);
        
        if (json != null) {
            try {
                return objectMapper.readValue(json, GameState.class);
            } catch (Exception e) {
                // Fall through to load from snapshot
            }
        }
        
        // Load from latest snapshot
        return snapshotRepository.findTopByGameIdOrderByTurnDesc(gameId)
                .map(GameSnapshot::getState)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));
    }
    
    private void saveGameState(GameState state) {
        String key = "game:" + state.getGameId() + ":state";
        try {
            String json = objectMapper.writeValueAsString(state);
            redis.opsForValue().set(key, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save game state", e);
        }
    }
    
    private void createSnapshot(GameState state) {
        GameSnapshot snapshot = new GameSnapshot(state.getGameId(), state.getTurn(), state);
        snapshotRepository.save(snapshot);
    }
    
    public Map<String, Object> processAttack(String gameId, String attackerId, AttackRequest request) {
        GameState state = getGameState(gameId);
        
        // Validate attack
        ValidationResult validation = validateAttack(state, attackerId, request);
        if (!validation.isValid()) {
            return Map.of(
                    "success", false,
                    "reason", validation.getReason()
            );
        }
        
        // Process attack
        Coord target = request.target();
        String defenderId = state.getPlayers().keySet().stream()
                .filter(id -> !id.equals(attackerId))
                .findFirst()
                .orElseThrow();
        
        PlayerState defender = state.getPlayers().get(defenderId);
        PlayerState attacker = state.getPlayers().get(attackerId);
        
        boolean isHit = checkHit(defender.getBoard(), target);
        
        if (isHit) {
            defender.getBoard().getHits().add(target);
            attacker.getBoard().getAttackedByMe().add(target);
            
            // Check if ship is sunk
            Ship sunkShip = checkAndMarkSunkShip(defender.getBoard(), target);
            
            // Check for win
            boolean allSunk = defender.getBoard().getShips().stream().allMatch(Ship::isSunk);
            if (allSunk) {
                state.setWinnerPlayerId(attackerId);
                Game game = gameRepository.findById(gameId).orElseThrow();
                game.setStatus(Game.GameStatus.ENDED);
                game.setWinnerPlayerId(attackerId);
                gameRepository.save(game);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("isHit", true);
            result.put("target", target);
            if (sunkShip != null) {
                result.put("sunkShip", Map.of("kind", sunkShip.getKind(), "length", sunkShip.getKind().getLength()));
            }
            if (allSunk) {
                result.put("gameEnded", true);
                result.put("winner", attackerId);
            }
            
            return result;
        } else {
            defender.getBoard().getMisses().add(target);
            attacker.getBoard().getAttackedByMe().add(target);
            
            return Map.of(
                    "success", true,
                    "isHit", false,
                    "target", target
            );
        }
    }
    
    private ValidationResult validateAttack(GameState state, String attackerId, AttackRequest request) {
        if (!attackerId.equals(state.getCurrentPlayerId())) {
            return new ValidationResult(false, "NOT_YOUR_TURN");
        }
        
        if (state.getTurn() != request.turnNumber()) {
            return new ValidationResult(false, "INVALID_TURN_NUMBER");
        }
        
        if (state.getWinnerPlayerId() != null) {
            return new ValidationResult(false, "GAME_ENDED");
        }
        
        if (!shipPlacementService.isValidCoord(request.target())) {
            return new ValidationResult(false, "OUT_OF_BOUNDS");
        }
        
        // Check if already attacked
        PlayerState attacker = state.getPlayers().get(attackerId);
        if (attacker.getBoard().getAttackedByMe().contains(request.target())) {
            return new ValidationResult(false, "DUPLICATE_ATTACK");
        }
        
        return new ValidationResult(true, null);
    }
    
    private boolean checkHit(Board defenderBoard, Coord target) {
        return defenderBoard.getShips().stream()
                .anyMatch(ship -> ship.getCells().contains(target));
    }
    
    private Ship checkAndMarkSunkShip(Board board, Coord target) {
        for (Ship ship : board.getShips()) {
            if (!ship.isSunk() && ship.getCells().contains(target)) {
                boolean allHit = ship.getCells().stream()
                        .allMatch(cell -> board.getHits().contains(cell));
                if (allHit) {
                    ship.setSunk(true);
                    return ship;
                }
            }
        }
        return null;
    }
    
    public void switchTurn(GameState state) {
        String nextPlayerId = state.getPlayers().keySet().stream()
                .filter(id -> !id.equals(state.getCurrentPlayerId()))
                .findFirst()
                .orElseThrow();
        
        state.setCurrentPlayerId(nextPlayerId);
        state.setTurn(state.getTurn() + 1);
        state.setStateVersion(state.getStateVersion() + 1);
        
        saveGameState(state);
        
        // Create snapshot periodically
        if (state.getTurn() % 5 == 0) {
            createSnapshot(state);
        }
    }
    
    private static class ValidationResult {
        private final boolean valid;
        private final String reason;
        
        ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getReason() {
            return reason;
        }
    }
}

