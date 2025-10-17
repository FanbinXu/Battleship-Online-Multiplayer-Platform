package app.battleship.service;

import app.battleship.model.Game;
import app.battleship.model.Room;
import app.battleship.persist.GameRepository;
import app.battleship.persist.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    
    public RoomService(RoomRepository roomRepository, GameRepository gameRepository,
                      GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }
    
    public List<Room> getWaitingRooms() {
        return roomRepository.findByStatus(Room.RoomStatus.WAITING);
    }
    
    public Room createRoom(String userId) {
        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId);
        room.getPlayerIds().add(userId);
        room.setStatus(Room.RoomStatus.WAITING);
        
        return roomRepository.save(room);
    }
    
    public Room joinRoom(String roomId, String userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        
        if (room.isFull()) {
            throw new IllegalArgumentException("Room is full");
        }
        
        if (room.getPlayerIds().contains(userId)) {
            throw new IllegalArgumentException("Already in room");
        }
        
        room.getPlayerIds().add(userId);
        room.setUpdatedAt(Instant.now());
        
        if (room.isFull()) {
            room.setStatus(Room.RoomStatus.FULL);
            room = roomRepository.save(room);
            
            // Start game
            startGame(room);
        } else {
            room = roomRepository.save(room);
        }
        
        return room;
    }
    
    private void startGame(Room room) {
        String gameId = UUID.randomUUID().toString();
        String firstPlayerId = room.getPlayerIds().get(0);
        String secondPlayerId = room.getPlayerIds().get(1);
        
        // Randomly choose who goes first
        if (new Random().nextBoolean()) {
            String temp = firstPlayerId;
            firstPlayerId = secondPlayerId;
            secondPlayerId = temp;
        }
        
        // Create game entity
        Game game = new Game(gameId, room.getId(), firstPlayerId, secondPlayerId);
        gameRepository.save(game);
        
        // Initialize game state with ship placement
        gameService.initializeGame(gameId, room.getId(), firstPlayerId, secondPlayerId);
        
        // Update room
        room.setStatus(Room.RoomStatus.IN_GAME);
        room.setGameId(gameId);
        room.setUpdatedAt(Instant.now());
        roomRepository.save(room);
        
        // Broadcast GAME_STARTED event
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventSeq", 1,
                "type", "GAME_STARTED",
                "payload", Map.of(
                        "gameId", gameId,
                        "roomId", room.getId(),
                        "firstPlayerId", firstPlayerId
                )
        );
        
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), event);
    }
    
    public Room leaveRoom(String roomId, String userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        
        room.getPlayerIds().remove(userId);
        room.setUpdatedAt(Instant.now());
        
        if (room.isEmpty()) {
            room.setStatus(Room.RoomStatus.EMPTY);
            room.setLastEmptyAt(Instant.now());
        }
        
        return roomRepository.save(room);
    }
    
    public void cleanupEmptyRooms(int ttlSeconds) {
        Instant threshold = Instant.now().minusSeconds(ttlSeconds);
        List<Room> emptyRooms = roomRepository.findByStatusAndLastEmptyAtBefore(
                Room.RoomStatus.EMPTY, threshold
        );
        
        roomRepository.deleteAll(emptyRooms);
    }
}

