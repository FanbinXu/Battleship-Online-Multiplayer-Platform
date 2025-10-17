package app.battleship.persist;

import app.battleship.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends MongoRepository<Game, String> {
    Optional<Game> findByRoomId(String roomId);
}



