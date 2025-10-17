package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    private String id;
    
    private RoomStatus status;
    
    private List<String> playerIds = new ArrayList<>();
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    private Instant lastEmptyAt;
    
    private String gameId;
    
    public Room(String id) {
        this.id = id;
        this.status = RoomStatus.WAITING;
        this.playerIds = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public boolean isFull() {
        return playerIds.size() >= 2;
    }
    
    public boolean isEmpty() {
        return playerIds.isEmpty();
    }
    
    public enum RoomStatus {
        WAITING, FULL, IN_GAME, EMPTY
    }
}



