package app.battleship.model;
import java.time.Instant;
import java.util.Map;
public record GameEvent(long seq, String type, Map<String,Object> payload, Instant ts) {}
