package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSnapshot {
    @Id
    private String id;
    
    private String gameId;
    
    private int turn;
    
    private GameState state;
    
    private Instant createdAt;
    
    public GameSnapshot(String gameId, int turn, GameState state) {
        this.gameId = gameId;
        this.turn = turn;
        this.state = state;
        this.createdAt = Instant.now();
    }
}



