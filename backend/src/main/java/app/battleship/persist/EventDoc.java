package app.battleship.persist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document("events")
@CompoundIndex(name = "game_seq_idx", def = "{'gameId': 1, 'eventSeq': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDoc {
    @Id 
    private String id;
    
    private String eventId;
    
    private String gameId;
    
    private long eventSeq;
    
    private int turn;
    
    private String eventType;
    
    private Map<String, Object> payload;
    
    private Instant createdAt;
    
    public static EventDoc of(String gameId, long eventSeq, int turn, String eventType, Map<String, Object> payload) {
        EventDoc doc = new EventDoc();
        doc.eventId = UUID.randomUUID().toString();
        doc.gameId = gameId;
        doc.eventSeq = eventSeq;
        doc.turn = turn;
        doc.eventType = eventType;
        doc.payload = payload;
        doc.createdAt = Instant.now();
        return doc;
    }
}
