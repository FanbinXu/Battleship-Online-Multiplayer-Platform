package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    @Id
    private String id;
    
    private String roomId;
    
    private GameStatus status;
    
    private String firstPlayerId;
    
    private String secondPlayerId;
    
    private int turn;
    
    private String currentPlayerId;
    
    private String winnerPlayerId;
    
    private Instant createdAt;
    
    private Instant endedAt;
    
    public Game(String id, String roomId, String firstPlayerId, String secondPlayerId) {
        this.id = id;
        this.roomId = roomId;
        this.status = GameStatus.ACTIVE;
        this.firstPlayerId = firstPlayerId;
        this.secondPlayerId = secondPlayerId;
        this.turn = 1;
        this.currentPlayerId = firstPlayerId;
        this.createdAt = Instant.now();
    }
    
    public enum GameStatus {
        PENDING, ACTIVE, ENDED
    }
}



