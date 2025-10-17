package app.battleship.service;

import app.battleship.model.Coord;
import app.battleship.model.GameState;
import app.battleship.model.PlayerState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiSuggestionService {
    
    private static final Logger log = LoggerFactory.getLogger(AiSuggestionService.class);
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;
    private final StringRedisTemplate redis;
    
    public AiSuggestionService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                              SimpMessagingTemplate messagingTemplate, GameService gameService,
                              StringRedisTemplate redis) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.gameService = gameService;
        this.redis = redis;
    }
    
    @Async
    public void generateSuggestion(String gameId, String playerId) {
        try {
            GameState state = gameService.getGameState(gameId);
            
            Map<String, Object> suggestion;
            if (openaiApiKey == null || openaiApiKey.isBlank()) {
                log.info("OpenAI API key not configured, using local heuristic");
                suggestion = generateLocalHeuristic(state, playerId);
            } else {
                try {
                    suggestion = generateOpenAiSuggestion(state, playerId);
                } catch (Exception e) {
                    log.warn("OpenAI API failed, falling back to local heuristic", e);
                    suggestion = generateLocalHeuristic(state, playerId);
                }
            }
            
            // Broadcast SUGGESTION_READY
            Long eventSeq = redis.opsForValue().increment("game:" + gameId + ":eventSeq", 1L);
            if (eventSeq == null) eventSeq = 1L;
            
            Map<String, Object> event = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventSeq", eventSeq,
                    "type", "SUGGESTION_READY",
                    "payload", Map.of(
                            "gameId", gameId,
                            "turn", state.getTurn(),
                            "suggestion", suggestion
                    )
            );
            
            messagingTemplate.convertAndSend("/topic/rooms/" + state.getRoomId(), event);
            
        } catch (Exception e) {
            log.error("Failed to generate suggestion", e);
        }
    }
    
    private Map<String, Object> generateLocalHeuristic(GameState state, String playerId) {
        PlayerState myState = state.getPlayers().get(playerId);
        List<Coord> alreadyAttacked = myState.getBoard().getAttackedByMe();
        
        // Find all available coordinates
        List<Coord> available = new ArrayList<>();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                Coord coord = new Coord(r, c);
                if (!alreadyAttacked.contains(coord)) {
                    available.add(coord);
                }
            }
        }
        
        if (available.isEmpty()) {
            throw new RuntimeException("No available coordinates");
        }
        
        // Simple heuristic: prefer coordinates adjacent to hits
        List<Coord> hits = alreadyAttacked.stream()
                .filter(coord -> {
                    String opponentId = state.getPlayers().keySet().stream()
                            .filter(id -> !id.equals(playerId))
                            .findFirst()
                            .orElseThrow();
                    PlayerState opponent = state.getPlayers().get(opponentId);
                    return opponent.getBoard().getShips().stream()
                            .anyMatch(ship -> ship.getCells().contains(coord));
                })
                .collect(Collectors.toList());
        
        Coord target;
        if (!hits.isEmpty()) {
            // Find unsunk hit and attack adjacent
            Coord lastHit = hits.get(hits.size() - 1);
            List<Coord> adjacents = getAdjacentCoords(lastHit);
            List<Coord> availableAdjacents = adjacents.stream()
                    .filter(available::contains)
                    .collect(Collectors.toList());
            
            if (!availableAdjacents.isEmpty()) {
                target = availableAdjacents.get(new Random().nextInt(availableAdjacents.size()));
            } else {
                target = available.get(new Random().nextInt(available.size()));
            }
        } else {
            // Random attack
            target = available.get(new Random().nextInt(available.size()));
        }
        
        return Map.of(
                "type", "ATTACK",
                "confidence", 0.5,
                "detail", Map.of("target", target)
        );
    }
    
    private List<Coord> getAdjacentCoords(Coord coord) {
        List<Coord> adjacents = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        for (int[] dir : directions) {
            int r = coord.getR() + dir[0];
            int c = coord.getC() + dir[1];
            if (r >= 0 && r < 10 && c >= 0 && c < 10) {
                adjacents.add(new Coord(r, c));
            }
        }
        
        return adjacents;
    }
    
    private Map<String, Object> generateOpenAiSuggestion(GameState state, String playerId) throws Exception {
        PlayerState myState = state.getPlayers().get(playerId);
        
        // Build prompt with game state
        String prompt = buildPrompt(myState);
        
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", 
                                "You are a Battleship game AI. Standard rules: 10x10 board, fleet 5/4/3/3/2, " +
                                "no movement, 1 attack per turn, no duplicate attacks. " +
                                "Output ONLY valid JSON with this exact format: " +
                                "{\"type\":\"ATTACK\",\"confidence\":0.0,\"detail\":{\"target\":{\"r\":0,\"c\":0}}}"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7
        );
        
        String response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + openaiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        if (response == null) {
            throw new RuntimeException("Empty response from OpenAI");
        }
        
        // Parse response
        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        
        // Extract JSON from content (might have markdown wrapping)
        String jsonContent = content.trim();
        if (jsonContent.startsWith("```")) {
            int start = jsonContent.indexOf("{");
            int end = jsonContent.lastIndexOf("}") + 1;
            jsonContent = jsonContent.substring(start, end);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> suggestion = objectMapper.readValue(jsonContent, Map.class);
        
        // Validate suggestion format
        if (!"ATTACK".equals(suggestion.get("type"))) {
            throw new RuntimeException("Invalid suggestion type");
        }
        
        return suggestion;
    }
    
    private String buildPrompt(PlayerState myState) {
        List<Coord> attacked = myState.getBoard().getAttackedByMe();
        
        return String.format(
                "I have attacked these coordinates: %s. " +
                "Suggest the next best attack coordinate (row 0-9, col 0-9). " +
                "Return only the JSON object.",
                attacked.isEmpty() ? "none yet" : attacked.toString()
        );
    }
}



