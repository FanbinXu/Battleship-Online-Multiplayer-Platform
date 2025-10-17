package app.battleship.persist;

import app.battleship.model.GameSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameSnapshotRepository extends MongoRepository<GameSnapshot, String> {
    Optional<GameSnapshot> findTopByGameIdOrderByTurnDesc(String gameId);
}



