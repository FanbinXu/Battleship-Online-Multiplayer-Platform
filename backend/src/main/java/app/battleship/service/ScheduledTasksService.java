package app.battleship.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasksService {
    
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksService.class);
    
    private final RoomService roomService;
    
    @Value("${room.empty.ttl:60}")
    private int roomEmptyTtl;
    
    public ScheduledTasksService(RoomService roomService) {
        this.roomService = roomService;
    }
    
    @Scheduled(fixedDelayString = "${room.cleanup.interval:30000}")
    public void cleanupEmptyRooms() {
        try {
            log.debug("Running empty room cleanup task");
            roomService.cleanupEmptyRooms(roomEmptyTtl);
        } catch (Exception e) {
            log.error("Error during room cleanup", e);
        }
    }
}



