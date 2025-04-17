package cu.searchengine.repository;

import cu.searchengine.model.Documents;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
// Add here any custom methods
public interface DocumentsRepository extends MongoRepository<Documents, String> {
}
