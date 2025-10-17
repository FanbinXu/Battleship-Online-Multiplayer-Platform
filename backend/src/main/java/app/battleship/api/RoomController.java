package app.battleship.api;

import app.battleship.model.Room;
import app.battleship.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoomController {
    
    private final RoomService roomService;
    
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
    
    @GetMapping("/rooms")
    public List<Room> getRooms() {
        return roomService.getWaitingRooms();
    }
    
    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            Room room = roomService.createRoom(userId);
            return ResponseEntity.ok(Map.of("roomId", room.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId, Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            Room room = roomService.joinRoom(roomId, userId);
            return ResponseEntity.ok(room);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId, Authentication auth) {
        try {
            String userId = (String) auth.getPrincipal();
            Room room = roomService.leaveRoom(roomId, userId);
            return ResponseEntity.ok(room);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

