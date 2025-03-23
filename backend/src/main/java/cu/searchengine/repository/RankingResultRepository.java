package cu.searchengine.repository;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import cu.searchengine.model.RankedDocument;
@Repository
public interface RankingResultRepository extends MongoRepository<RankedDocument, String> {
    // Custom query to get top-ranked results
    List<RankedDocument> findTop10ByOrderByFinalScoreDesc();
}
