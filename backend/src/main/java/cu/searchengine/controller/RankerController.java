package cu.searchengine.controller;

import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.model.RankedDocument;
import cu.searchengine.ranker.PopularityScorer;
import cu.searchengine.ranker.RelevanceScorer;
import cu.searchengine.service.DocumentService;
import cu.searchengine.service.InvertedIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RankerController {
    private static final Logger logger = LoggerFactory.getLogger(RankerController.class);
    private final RelevanceScorer relevanceScorer;
    private final PopularityScorer popularityScorer;
    private final double relevanceWeight = 0.7;
    private final double popularityWeight = 0.3;
    private final InvertedIndexService invertedIndexService;
    private final int numThreads;

    public RankerController(int totalDocuments, DocumentService documentServices, InvertedIndexService invertedIndexService) {
        this.relevanceScorer = new RelevanceScorer(totalDocuments);
        this.popularityScorer = new PopularityScorer(documentServices);
        this.invertedIndexService = invertedIndexService;
        this.numThreads = 128;
    }

    public List<RankedDocument> rankDocuments(String[] wordsArray) {
        // Fetch word entries (cached by InvertedIndexService)
        Map<String, InvertedIndexEntry> wordToEntryMap = invertedIndexService.getEntriesForWords(wordsArray);
        if (wordToEntryMap.isEmpty()) {
            return List.of(); // Return empty list immediately
        }

        // Step 1: Calculate relevance scores (already parallelized internally)
        Map<Integer, RankedDocument> docScoresMap = relevanceScorer.calculateRelevanceScores(wordsArray, wordToEntryMap);

        // Step 2: Calculate popularity scores (if needed)
        popularityScorer.calculatePopularityScores(docScoresMap);

        // Step 3: Calculate final scores and sort
        docScoresMap.forEach((docId, doc) -> {
            double finalScore = (relevanceWeight * doc.getRelevanceScore()) + (popularityWeight * doc.getPopularityScore());
            doc.setFinalScore(finalScore);
        });

        return docScoresMap.values().stream()
                .sorted(Comparator.comparingDouble(RankedDocument::getFinalScore).reversed())
                .collect(Collectors.toList());
    }
}