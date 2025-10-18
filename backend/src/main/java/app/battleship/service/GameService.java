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
                GameState state = objectMapper.readValue(json, GameState.class);
                
                // Debug: Verify what we loaded
                System.out.println("[GameService] ========== LOADED FROM REDIS ==========");
                System.out.println("[GameService] Key: " + key);
                for (PlayerState player : state.getPlayers().values()) {
                    System.out.println("[GameService] Player " + player.getPlayerId() + ":");
                    System.out.println("[GameService]   AttacksByMeHits: " + player.getBoard().getAttacksByMeHits());
                    System.out.println("[GameService]   AttacksByMeMisses: " + player.getBoard().getAttacksByMeMisses());
                    System.out.println("[GameService]   Ships count: " + player.getBoard().getShips().size());
                    System.out.println("[GameService]   SunkShips count: " + player.getBoard().getSunkShips().size());
                }
                
                return state;
            } catch (Exception e) {
                System.err.println("[GameService] ERROR loading from Redis: " + e.getMessage());
                e.printStackTrace();
                // Fall through to load from snapshot
            }
        } else {
            System.out.println("[GameService] No Redis data found for key: " + key);
        }
        
        // Load from latest snapshot
        System.out.println("[GameService] Loading from MongoDB snapshot...");
        return snapshotRepository.findTopByGameIdOrderByTurnDesc(gameId)
                .map(GameSnapshot::getState)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));
    }
    
    private void saveGameState(GameState state) {
        String key = "game:" + state.getGameId() + ":state";
        try {
            String json = objectMapper.writeValueAsString(state);
            redis.opsForValue().set(key, json);
            
            // Debug: Verify what we saved
            System.out.println("[GameService] ========== SAVED TO REDIS ==========");
            System.out.println("[GameService] Key: " + key);
            for (PlayerState player : state.getPlayers().values()) {
                System.out.println("[GameService] Player " + player.getPlayerId() + ":");
                System.out.println("[GameService]   AttacksByMeHits: " + player.getBoard().getAttacksByMeHits());
                System.out.println("[GameService]   AttacksByMeMisses: " + player.getBoard().getAttacksByMeMisses());
                System.out.println("[GameService]   Ships count: " + player.getBoard().getShips().size());
                System.out.println("[GameService]   SunkShips count: " + player.getBoard().getSunkShips().size());
            }
        } catch (Exception e) {
            System.err.println("[GameService] ERROR saving state: " + e.getMessage());
            e.printStackTrace();
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
        
        System.out.println("[GameService] ============ PROCESSING ATTACK ============");
        System.out.println("[GameService] Target: " + target);
        System.out.println("[GameService] IsHit: " + isHit);
        System.out.println("[GameService] Attacker ID: " + attackerId);
        System.out.println("[GameService] AttacksByMeHits BEFORE: " + attacker.getBoard().getAttacksByMeHits());
        System.out.println("[GameService] AttacksByMeMisses BEFORE: " + attacker.getBoard().getAttacksByMeMisses());
        
        // Remove from previous records if this is a re-attack
        boolean wasHit = attacker.getBoard().getAttacksByMeHits().remove(target);
        boolean wasMiss = attacker.getBoard().getAttacksByMeMisses().remove(target);
        if (wasHit || wasMiss) {
            System.out.println("[GameService] RE-ATTACK! Previous was: " + (wasHit ? "HIT" : "MISS"));
        }
        
        if (isHit) {
            // Record hit on defender's board (only if not already there)
            if (!defender.getBoard().getHits().contains(target)) {
                defender.getBoard().getHits().add(target);
            }
            // Record this attack as a hit in attacker's record
            attacker.getBoard().getAttacksByMeHits().add(target);
            
            System.out.println("[GameService] HIT! Added to defender hits and attacker attacksByMeHits");
            System.out.println("[GameService] AttacksByMeHits AFTER: " + attacker.getBoard().getAttacksByMeHits());
            
            // Mark the hit on the ship itself (record relative position)
            Ship hitShip = markShipHit(defender.getBoard(), target);
            
            // Check if ship is sunk
            Ship sunkShip = null;
            if (hitShip != null && hitShip.isFullyDamaged()) {
                hitShip.setSunk(true);
                sunkShip = hitShip;
                // Move sunk ship from active ships to sunkShips list
                defender.getBoard().getShips().remove(hitShip);
                defender.getBoard().getSunkShips().add(hitShip);
                System.out.println("[GameService] Ship SUNK and moved to sunkShips: " + hitShip.getKind());
                System.out.println("[GameService] Active ships remaining: " + defender.getBoard().getShips().size());
            }
            
            // Check for win (all ships removed from board = all sunk)
            boolean allSunk = defender.getBoard().getShips().isEmpty();
            if (allSunk) {
                state.setWinnerPlayerId(attackerId);
                Game game = gameRepository.findById(gameId).orElseThrow();
                game.setStatus(Game.GameStatus.ENDED);
                game.setWinnerPlayerId(attackerId);
                gameRepository.save(game);
            }
            
            // Save updated state
            saveGameState(state);
            
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
            // DO NOT record miss on defender's board - opponent should not see where you missed
            // Only record in attacker's record
            attacker.getBoard().getAttacksByMeMisses().add(target);
            
            System.out.println("[GameService] MISS! Added to attacker attacksByMeMisses only (not visible to opponent)");
            System.out.println("[GameService] AttacksByMeMisses AFTER: " + attacker.getBoard().getAttacksByMeMisses());
            
            // Save updated state
            saveGameState(state);
            
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
        
        // Allow re-attacking - no duplicate check
        // This allows players to verify if opponent moved ships
        
        return new ValidationResult(true, null);
    }
    
    private boolean checkHit(Board defenderBoard, Coord target) {
        return defenderBoard.getShips().stream()
                .anyMatch(ship -> ship.getCells().contains(target));
    }
    
    /**
     * Mark a ship as hit at the target position, recording the relative index
     * Returns the hit ship, or null if no ship at target
     */
    private Ship markShipHit(Board board, Coord target) {
        for (Ship ship : board.getShips()) {
            int index = ship.getCells().indexOf(target);
            if (index >= 0) {
                // Record the relative position as damaged
                ship.getHitIndices().add(index);
                System.out.println("[GameService] Ship " + ship.getKind() + " hit at relative index " + index);
                System.out.println("[GameService] Ship hitIndices: " + ship.getHitIndices());
                System.out.println("[GameService] Ship fully damaged: " + ship.isFullyDamaged());
                return ship;
            }
        }
        return null;
    }
    
    public Map<String, Object> processShipMove(String gameId, String playerId, ShipMoveRequest request) {
        GameState state = getGameState(gameId);
        
        // Validate move
        ValidationResult validation = validateShipMove(state, playerId, request);
        if (!validation.isValid()) {
            return Map.of(
                    "success", false,
                    "reason", validation.getReason()
            );
        }
        
        // Process move
        PlayerState player = state.getPlayers().get(playerId);
        Ship ship = player.getBoard().getShips().stream()
                .filter(s -> s.getId().equals(request.shipId()))
                .findFirst()
                .orElseThrow();
        
        System.out.println("[GameService] ============ MOVING SHIP ============");
        System.out.println("[GameService] Ship: " + ship.getKind() + " (" + ship.getId() + ")");
        System.out.println("[GameService] Old cells: " + ship.getCells());
        System.out.println("[GameService] HitIndices: " + ship.getHitIndices());
        
        // Remove old damaged cells from board.hits
        List<Coord> oldDamagedCells = ship.getDamagedCells();
        player.getBoard().getHits().removeAll(oldDamagedCells);
        System.out.println("[GameService] Removed old damaged cells from board.hits: " + oldDamagedCells);
        
        // Calculate new cells based on new position and orientation
        List<Coord> newCells = new ArrayList<>();
        int length = ship.getKind().getLength();
        
        for (int i = 0; i < length; i++) {
            if (request.isHorizontal()) {
                newCells.add(new Coord(request.newPosition().getR(), request.newPosition().getC() + i));
            } else {
                newCells.add(new Coord(request.newPosition().getR() + i, request.newPosition().getC()));
            }
        }
        
        // Update ship cells
        ship.setCells(newCells);
        
        // Add new damaged cells to board.hits (based on preserved hitIndices)
        List<Coord> newDamagedCells = ship.getDamagedCells();
        player.getBoard().getHits().addAll(newDamagedCells);
        System.out.println("[GameService] New cells: " + newCells);
        System.out.println("[GameService] New damaged cells added to board.hits: " + newDamagedCells);
        
        // Save state
        saveGameState(state);
        
        return Map.of(
                "success", true,
                "ship", ship
        );
    }
    
    private ValidationResult validateShipMove(GameState state, String playerId, ShipMoveRequest request) {
        if (!playerId.equals(state.getCurrentPlayerId())) {
            return new ValidationResult(false, "NOT_YOUR_TURN");
        }
        
        if (state.getTurn() != request.turnNumber()) {
            return new ValidationResult(false, "INVALID_TURN_NUMBER");
        }
        
        if (state.getWinnerPlayerId() != null) {
            return new ValidationResult(false, "GAME_ENDED");
        }
        
        PlayerState player = state.getPlayers().get(playerId);
        Ship ship = player.getBoard().getShips().stream()
                .filter(s -> s.getId().equals(request.shipId()))
                .findFirst()
                .orElse(null);
        
        if (ship == null) {
            return new ValidationResult(false, "SHIP_NOT_FOUND");
        }
        
        if (ship.isSunk()) {
            return new ValidationResult(false, "SHIP_ALREADY_SUNK");
        }
        
        // Calculate new cells
        List<Coord> newCells = new ArrayList<>();
        int length = ship.getKind().getLength();
        
        for (int i = 0; i < length; i++) {
            Coord newCell;
            if (request.isHorizontal()) {
                newCell = new Coord(request.newPosition().getR(), request.newPosition().getC() + i);
            } else {
                newCell = new Coord(request.newPosition().getR() + i, request.newPosition().getC());
            }
            
            // Check bounds
            if (!shipPlacementService.isValidCoord(newCell)) {
                return new ValidationResult(false, "OUT_OF_BOUNDS");
            }
            
            newCells.add(newCell);
        }
        
        // Check for overlaps with other ships
        for (Ship otherShip : player.getBoard().getShips()) {
            if (otherShip.getId().equals(ship.getId())) {
                continue; // Skip self
            }
            
            for (Coord otherCell : otherShip.getCells()) {
                if (newCells.contains(otherCell)) {
                    return new ValidationResult(false, "OVERLAPS_WITH_OTHER_SHIP");
                }
            }
        }
        
        return new ValidationResult(true, null);
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

