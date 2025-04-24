package cu.searchengine.controller;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cu.searchengine.model.RankedDocument;
import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.ranker.PopularityScorer;
import cu.searchengine.ranker.RelevanceScorer;
import cu.searchengine.service.DocumentService;
import cu.searchengine.service.InvertedIndexService;

public class RankerController {
    private RelevanceScorer relevanceScorer;
    private PopularityScorer popularityScorer;
    private double relevanceWeight = 0.7;
    private double popularityWeight = 0.3;

    public List<InvertedIndexEntry> invertedIndexData;

    public RankerController(int totalDocuments, DocumentService documentServices, InvertedIndexService invertedIndexService) {
        this.relevanceScorer = new RelevanceScorer(totalDocuments);
        this.popularityScorer = new PopularityScorer(documentServices);
        this.invertedIndexData = invertedIndexService.getAll();
    }

    public List<RankedDocument> rankDocuments(String[] wordsArray) {
        Map<Integer, RankedDocument> docScoresMap = new ConcurrentHashMap<>();

        // Create maps for faster lookup of InvertedIndexEntries and DFs by word
        Map<String, List<RankedDocument>> wordToDocsMap = new HashMap<>();
        Map<String, Integer> wordToDfMap = new HashMap<>();

        // Prepare data structures for parallel processing
        for (String word : wordsArray) {
            InvertedIndexEntry entry = invertedIndexData.stream()
                    .filter(e -> e.getWord().equals(word))
                    .findFirst()
                    .orElse(null);

            if (entry != null && entry.getRankedPostings() != null && !entry.getRankedPostings().isEmpty()) {
                wordToDocsMap.put(word, entry.getRankedPostings());
                wordToDfMap.put(word, entry.getDf());
            }
        }

        // Process relevance scores in parallel
        relevanceScorer.computeRelevanceScoresParallel(docScoresMap, wordsArray, wordToDocsMap, wordToDfMap);

        // Normalize relevance scores in parallel
        relevanceScorer.normalizeScoresParallel(docScoresMap);

        // Calculate popularity scores (already multithreaded from previous implementation)
        docScoresMap = popularityScorer.calculatePopularityScores(docScoresMap);

        // Calculate final scores - this is relatively fast so keeping it sequential
        for (RankedDocument doc : docScoresMap.values()) {
            double normalizedRelevance = doc.getRelevanceScore();
            double normalizedPopularity = doc.getPopularityScore();
            double finalScore = (relevanceWeight * normalizedRelevance) + (popularityWeight * normalizedPopularity);
            doc.setFinalScore(finalScore);
        }

        // Convert HashMap values to a List and sort by FinalScore
        List<RankedDocument> result = docScoresMap.values().stream()
                .sorted(Comparator.comparingDouble(RankedDocument::getFinalScore).reversed())
                .toList();

        // Clean up thread pools
        relevanceScorer.shutdown();

        return result;
    }
}