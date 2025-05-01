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
    private InvertedIndexService invertedIndexService;

    public RankerController(int totalDocuments, DocumentService documentServices, InvertedIndexService invertedIndexService) {
        this.relevanceScorer = new RelevanceScorer(totalDocuments);
        this.popularityScorer = new PopularityScorer(documentServices);
        this.invertedIndexService = invertedIndexService;
    }

    public List<RankedDocument> rankDocuments(String[] wordsArray) {
        Map<Integer, RankedDocument> docScoresMap = new ConcurrentHashMap<>();

        // Create maps for faster lookup of InvertedIndexEntries and DFs by word
        Map<String, List<RankedDocument>> wordToDocsMap = new HashMap<>();
        Map<String, Integer> wordToDfMap = new HashMap<>();

        // Fetch only the needed inverted index entries for the query words
        Map<String, InvertedIndexEntry> queryWordEntries = invertedIndexService.getEntriesForWords(wordsArray);

        // Prepare data structures using only the retrieved entries
        for (String word : wordsArray) {
            InvertedIndexEntry entry = queryWordEntries.get(word);
            if (entry != null && entry.getRankedPostings() != null && !entry.getRankedPostings().isEmpty()) {
                wordToDocsMap.put(word, entry.getRankedPostings());
                wordToDfMap.put(word, entry.getDf());
            }
        }

        // Process relevance scores in parallel
        relevanceScorer.computeRelevanceScoresParallel(docScoresMap, wordsArray, wordToDocsMap, wordToDfMap);

        // Normalize relevance scores in parallel
        relevanceScorer.normalizeScoresParallel(docScoresMap);

        // Calculate popularity scores
        popularityScorer.calculatePopularityScores(docScoresMap);

        // Calculate final scores with parallel stream for better performance
        docScoresMap.values().parallelStream().forEach(doc -> {
            double finalScore = (relevanceWeight * doc.getRelevanceScore()) +
                    (popularityWeight * doc.getPopularityScore());
            doc.setFinalScore(finalScore);
        });

        // Convert to List and sort - use ArrayList for better performance
        List<RankedDocument> result = docScoresMap.values().stream()
                .sorted(Comparator.comparingDouble(RankedDocument::getFinalScore).reversed())
                .collect(Collectors.toList());

        return result;
    }

    public void shutdown() {
        relevanceScorer.shutdown();
    }
}