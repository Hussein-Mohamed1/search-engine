package cu.searchengine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import cu.searchengine.model.SearchResult;

@Repository
public interface SearchResultRepository extends MongoRepository<SearchResult, String> {
    List<SearchResult> findByQuery(String query);
    
    // Find queries that start with the given prefix
    List<SearchResult> findByQueryStartingWithIgnoreCase(String prefix);
    
    // Find queries that contain the given text
    @Query("{ 'query' : { $regex: ?0, $options: 'i' } }")
    List<SearchResult> findByQueryContainingIgnoreCase(String text);
    
    // Find the most recent queries
    List<SearchResult> findTop10ByOrderByTimestampDesc();
}
