package cu.searchengine.repository;

import cu.searchengine.model.SearchQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchQueryRepository extends MongoRepository<SearchQuery, String> {
    // Find queries that start with the given prefix
    List<SearchQuery> findByQueryStartingWithIgnoreCaseOrderByFrequencyDesc(String prefix);
    
    // Find queries that contain the given text
    @Query("{ 'query' : { $regex: ?0, $options: 'i' } }")
    List<SearchQuery> findByQueryContainingIgnoreCaseOrderByFrequencyDesc(String text);
    
    // Find the most frequent queries
    List<SearchQuery> findTop10ByOrderByFrequencyDesc();
    
    // Find a query by exact match
    SearchQuery findByQueryIgnoreCase(String query);
} 