package app.battleship.persist;

import app.battleship.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByStatus(Room.RoomStatus status);
    List<Room> findByStatusAndLastEmptyAtBefore(Room.RoomStatus status, Instant before);
}



