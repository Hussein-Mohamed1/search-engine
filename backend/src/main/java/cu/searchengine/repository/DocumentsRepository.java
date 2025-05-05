package cu.searchengine.repository;

import cu.searchengine.model.Documents;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.Query;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
// Add here any custom methods
public interface DocumentsRepository extends MongoRepository<Documents, Integer> {
    Optional<Documents> findByUrl(String url);
    
    // Find all documents that have not been indexed yet
    List<Documents> findByInvertedIndexProcessedFalse();
    @Query("{ 'id': ?0 }")
    @Update("{ '$set': { 'popularityScore': ?1 } }")
    void updatePopularityScore(Integer id, Double score);
    boolean existsById(Integer id);

}
