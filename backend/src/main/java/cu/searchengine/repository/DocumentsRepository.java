package cu.searchengine.repository;

import cu.searchengine.model.Documents;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
// Add here any custom methods
public interface DocumentsRepository extends MongoRepository<Documents, Integer> {
    Optional<Documents> findByUrl(String url);
    
    // Find all documents that have not been indexed yet
    List<Documents> findByInvertedIndexProcessedFalse();
}
