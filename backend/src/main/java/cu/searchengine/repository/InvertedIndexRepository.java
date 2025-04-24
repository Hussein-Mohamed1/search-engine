package cu.searchengine.repository;

import cu.searchengine.model.InvertedIndexEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InvertedIndexRepository extends MongoRepository<InvertedIndexEntry, String> {
    InvertedIndexEntry findByWord(String word);
    List<InvertedIndexEntry> findByWordIn(Collection<String> words);
    // You can add custom queries here if needed
}
