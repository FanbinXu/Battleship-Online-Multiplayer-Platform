package app.battleship.api;

import app.battleship.service.AiSuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class SuggestionController {
    
    private final AiSuggestionService aiSuggestionService;
    
    public SuggestionController(AiSuggestionService aiSuggestionService) {
        this.aiSuggestionService = aiSuggestionService;
    }
    
    @PostMapping("/{gameId}/suggest")
    public ResponseEntity<?> getSuggestion(@PathVariable String gameId, Authentication auth) {
        try {
            String playerId = (String) auth.getPrincipal();
            aiSuggestionService.generateSuggestion(gameId, playerId);
            
            return ResponseEntity.accepted().body(Map.of(
                    "message", "Suggestion request accepted",
                    "status", "processing"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}



