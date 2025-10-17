package app.battleship.persist;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface EventRepository extends MongoRepository<EventDoc,String> {}
