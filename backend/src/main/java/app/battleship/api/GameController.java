package app.battleship.api;

import app.battleship.model.AttackRequest;
import app.battleship.model.GameState;
import app.battleship.model.ShipMoveRequest;
import app.battleship.persist.EventDoc;
import app.battleship.persist.EventRepository;
import app.battleship.service.GameService;
import app.battleship.service.ViewShapingService;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/games")
public class GameController {
    
    private final GameService gameService;
    private final ViewShapingService viewShapingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final EventRepository eventRepository;
    private final StringRedisTemplate redis;
    
    public GameController(GameService gameService, ViewShapingService viewShapingService,
                         SimpMessagingTemplate messagingTemplate, EventRepository eventRepository,
                         StringRedisTemplate redis) {
        this.gameService = gameService;
        this.viewShapingService = viewShapingService;
        this.messagingTemplate = messagingTemplate;
        this.eventRepository = eventRepository;
        this.redis = redis;
    }
    
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGameState(@PathVariable String gameId, Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            GameState state = gameService.getGameState(gameId);
            
            // Return shaped view for the requesting player
            Map<String, Object> view = viewShapingService.createPlayerView(state, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("gameId", gameId);
            response.put("roomId", state.getRoomId());
            response.put("yourView", view);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{gameId}/action/attack")
    public ResponseEntity<?> attack(@PathVariable String gameId, 
                                   @Valid @RequestBody AttackRequest request,
                                   Authentication auth) {
        try {
            String attackerId = (String) auth.getPrincipal();
            
            // Check idempotency
            String idempotencyKey = "attack:" + gameId + ":" + request.actionId();
            Boolean alreadyProcessed = redis.opsForValue().setIfAbsent(idempotencyKey, "1");
            if (Boolean.FALSE.equals(alreadyProcessed)) {
                // Already processed, return cached result or current state
                GameState state = gameService.getGameState(gameId);
                Map<String, Object> view = viewShapingService.createPlayerView(state, attackerId);
                return ResponseEntity.ok(Map.of("message", "Already processed", "yourView", view));
            }
            redis.expire(idempotencyKey, 60 * 5, java.util.concurrent.TimeUnit.SECONDS);
            
            // Process attack (this modifies and saves the state)
            Map<String, Object> result = gameService.processAttack(gameId, attackerId, request);
            
            if (Boolean.FALSE.equals(result.get("success"))) {
                // Broadcast rejection
                Map<String, Object> event = Map.of(
                        "eventId", UUID.randomUUID().toString(),
                        "type", "ACTION_REJECTED",
                        "payload", Map.of(
                                "actionId", request.actionId(),
                                "reason", result.get("reason")
                        )
                );
                
                messagingTemplate.convertAndSendToUser(attackerId, "/queue/errors", event);
                return ResponseEntity.badRequest().body(result);
            }
            
            // NOTE: processAttack saves state, so we need to reload to get the saved version
            // Get fresh state from database
            GameState state = gameService.getGameState(gameId);
            
            System.out.println("[GameController] ============ AFTER ATTACK ============");
            System.out.println("[GameController] Attacker ID: " + attackerId);
            System.out.println("[GameController] Turn: " + state.getTurn());
            System.out.println("[GameController] Current player: " + state.getCurrentPlayerId());
            System.out.println("[GameController] AttacksByMeHits: " + 
                state.getPlayers().get(attackerId).getBoard().getAttacksByMeHits());
            System.out.println("[GameController] AttacksByMeMisses: " + 
                state.getPlayers().get(attackerId).getBoard().getAttacksByMeMisses());
            
            // Switch turn if not game ended
            if (state.getWinnerPlayerId() == null) {
                System.out.println("[GameController] Switching turn...");
                gameService.switchTurn(state);
                
                // Reload to get updated state after switchTurn
                state = gameService.getGameState(gameId);
                
                System.out.println("[GameController] After switchTurn:");
                System.out.println("[GameController]   Turn: " + state.getTurn());
                System.out.println("[GameController]   Current player: " + state.getCurrentPlayerId());
                System.out.println("[GameController]   AttacksByMeHits: " + 
                    state.getPlayers().get(attackerId).getBoard().getAttacksByMeHits());
            }
            
            // Save event
            Long eventSeq = redis.opsForValue().increment("game:" + gameId + ":eventSeq", 1L);
            if (eventSeq == null) eventSeq = 1L;
            
            Map<String, Object> attackPayload = new HashMap<>(result);
            attackPayload.put("gameId", gameId);
            attackPayload.put("attackerId", attackerId);
            attackPayload.put("actionId", request.actionId());
            
            EventDoc eventDoc = EventDoc.of(gameId, eventSeq, state.getTurn(), 
                    "ATTACK_PROCESSED", attackPayload);
            eventRepository.save(eventDoc);
            
            // Broadcast STATE_UPDATED to all players in room
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", eventDoc.getEventId());
            event.put("eventSeq", eventSeq);
            event.put("type", "STATE_UPDATED");
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("gameId", gameId);
            payload.put("stateVersion", state.getStateVersion());
            payload.put("turn", state.getTurn());
            payload.put("currentPlayerId", state.getCurrentPlayerId());
            
            event.put("payload", payload);
            
            messagingTemplate.convertAndSend("/topic/rooms/" + state.getRoomId(), event);
            
            // Check if game ended
            if (state.getWinnerPlayerId() != null) {
                Long endSeq = redis.opsForValue().increment("game:" + gameId + ":eventSeq", 1L);
                Map<String, Object> endEvent = Map.of(
                        "eventId", UUID.randomUUID().toString(),
                        "eventSeq", endSeq != null ? endSeq : eventSeq + 1,
                        "type", "GAME_ENDED",
                        "payload", Map.of(
                                "winnerPlayerId", state.getWinnerPlayerId(),
                                "reason", "ALL_SUNK"
                        )
                );
                messagingTemplate.convertAndSend("/topic/rooms/" + state.getRoomId(), endEvent);
            }
            
            // Return shaped view
            Map<String, Object> view = viewShapingService.createPlayerView(state, attackerId);
            Map<String, Object> response = new HashMap<>(result);
            response.put("yourView", view);
            
            System.out.println("[GameController] ============ ATTACK RESPONSE ============");
            System.out.println("[GameController] Attack result: " + result);
            System.out.println("[GameController] Returning view with opponent.revealed.attacksByMe: " + 
                ((Map<?, ?>) ((Map<?, ?>) view.get("opponent")).get("revealed")).get("attacksByMe"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{gameId}/action/move")
    public ResponseEntity<?> moveShip(@PathVariable String gameId,
                                      @Valid @RequestBody ShipMoveRequest request,
                                      Authentication auth) {
        try {
            String playerId = (String) auth.getPrincipal();
            
            // Check idempotency
            String idempotencyKey = "move:" + gameId + ":" + request.actionId();
            Boolean alreadyProcessed = redis.opsForValue().setIfAbsent(idempotencyKey, "1");
            if (Boolean.FALSE.equals(alreadyProcessed)) {
                // Already processed, return cached result or current state
                GameState state = gameService.getGameState(gameId);
                Map<String, Object> view = viewShapingService.createPlayerView(state, playerId);
                return ResponseEntity.ok(Map.of("message", "Already processed", "yourView", view));
            }
            redis.expire(idempotencyKey, 60 * 5, java.util.concurrent.TimeUnit.SECONDS);
            
            // Process move
            Map<String, Object> result = gameService.processShipMove(gameId, playerId, request);
            
            if (Boolean.FALSE.equals(result.get("success"))) {
                // Broadcast rejection
                Map<String, Object> event = Map.of(
                        "eventId", UUID.randomUUID().toString(),
                        "type", "ACTION_REJECTED",
                        "payload", Map.of(
                                "actionId", request.actionId(),
                                "reason", result.get("reason")
                        )
                );
                
                messagingTemplate.convertAndSendToUser(playerId, "/queue/errors", event);
                return ResponseEntity.badRequest().body(result);
            }
            
            // Get updated state
            GameState state = gameService.getGameState(gameId);
            
            // Switch turn after successful move
            gameService.switchTurn(state);
            state = gameService.getGameState(gameId);
            
            // Save event
            Long eventSeq = redis.opsForValue().increment("game:" + gameId + ":eventSeq", 1L);
            if (eventSeq == null) eventSeq = 1L;
            
            Map<String, Object> movePayload = new HashMap<>(result);
            movePayload.put("gameId", gameId);
            movePayload.put("playerId", playerId);
            movePayload.put("actionId", request.actionId());
            
            EventDoc eventDoc = EventDoc.of(gameId, eventSeq, state.getTurn(),
                    "SHIP_MOVED", movePayload);
            eventRepository.save(eventDoc);
            
            // Broadcast STATE_UPDATED to all players in room
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", eventDoc.getEventId());
            event.put("eventSeq", eventSeq);
            event.put("type", "STATE_UPDATED");
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("gameId", gameId);
            payload.put("stateVersion", state.getStateVersion());
            payload.put("turn", state.getTurn());
            payload.put("currentPlayerId", state.getCurrentPlayerId());
            
            event.put("payload", payload);
            
            messagingTemplate.convertAndSend("/topic/rooms/" + state.getRoomId(), event);
            
            // Return shaped view
            Map<String, Object> view = viewShapingService.createPlayerView(state, playerId);
            Map<String, Object> response = new HashMap<>(result);
            response.put("yourView", view);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

