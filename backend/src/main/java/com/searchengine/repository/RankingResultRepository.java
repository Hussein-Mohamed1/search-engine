package com.searchengine.repository;
import com.searchengine.model.RankedDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface RankingResultRepository extends MongoRepository<RankedDocument, String> {
    // Custom query to get top-ranked results
    List<RankedDocument> findTop10ByOrderByFinalScoreDesc();
}
