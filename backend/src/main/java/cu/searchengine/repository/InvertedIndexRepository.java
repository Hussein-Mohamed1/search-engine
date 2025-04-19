package cu.searchengine.repository;

import cu.searchengine.model.InvertedIndexEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvertedIndexRepository extends MongoRepository<InvertedIndexEntry, String> {
    // You can add custom queries here if needed
}
