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
        this.numThreads = Runtime.getRuntime().availableProcessors();
    }

    public List<RankedDocument> rankDocuments(String[] wordsArray) {
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        try {
            // Fetch word entries (cached by InvertedIndexService)
            Map<String, InvertedIndexEntry> wordToEntryMap = invertedIndexService.getEntriesForWords(wordsArray);
            if (wordToEntryMap.isEmpty()) {
                return List.of(); // Return empty list immediately
            }
            // Use ConcurrentHashMap for thread-safe operations
            Map<Integer, RankedDocument> docScoresMap = new ConcurrentHashMap<>();

            // Parallelize relevance and popularity score calculations
            executorService.submit(() -> {
                Map<Integer, RankedDocument> relevanceScores = relevanceScorer.calculateRelevanceScores(wordsArray, wordToEntryMap);
                docScoresMap.putAll(relevanceScores);
            });

            executorService.submit(() -> {
                // Wait for relevance scores to be computed
                while (docScoresMap.isEmpty()) {
                    try {
                        Thread.sleep(10); // Avoid busy-waiting
                    } catch (InterruptedException e) {
                        logger.error("Error waiting for relevance scores: {}", e.getMessage());
                    }
                }
                popularityScorer.calculatePopularityScores(docScoresMap);
            });

            // Wait for all tasks to complete
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    logger.warn("Executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                logger.error("Executor interrupted: {}", e.getMessage());
            }

            // Calculate final scores (optimized loop)
            docScoresMap.forEach((docId, doc) -> {
                double finalScore = (relevanceWeight * doc.getRelevanceScore()) + (popularityWeight * doc.getPopularityScore());
                doc.setFinalScore(finalScore);
            });

            // Sort and return results
            return docScoresMap.values().stream()
                    .sorted(Comparator.comparingDouble(RankedDocument::getFinalScore).reversed())
                    .collect(Collectors.toList());

        } finally {
            // Ensure executor is reset for future calls
            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
    }
}