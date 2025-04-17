package com.searchengine.service;

import com.searchengine.model.RankedDocument;
import com.searchengine.repository.RankingResultRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RankingService {

    private final RankingResultRepository repository;

    public RankingService(RankingResultRepository repository) {
        this.repository = repository;
    }

    // Function to save ranking results (NO API)
    public void saveRanking(String url, String title, double RS , Double PS , double finalScore) {
        RankedDocument result = new RankedDocument(url, title, RS , PS , finalScore);
        repository.save(result);
        System.out.println("Saved ranking: " + url + " - " + finalScore);
    }

    // Function to get top-ranked results
    public List<RankedDocument> getTopResults() {
        return repository.findTop10ByOrderByFinalScoreDesc();
    }
}
