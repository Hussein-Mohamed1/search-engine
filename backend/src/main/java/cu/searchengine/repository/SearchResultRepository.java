package cu.searchengine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import cu.searchengine.model.SearchResult;

@Repository
public interface SearchResultRepository extends MongoRepository<SearchResult, String> {
    List<SearchResult> findByQuery(String query);
}
