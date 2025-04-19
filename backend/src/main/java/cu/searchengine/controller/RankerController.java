package cu.searchengine.controller;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private List<InvertedIndexEntry> invertedIndexData;

    public RankerController(int totalDocuments, DocumentService documentServices, InvertedIndexService invertedIndexService) {
        this.relevanceScorer = new RelevanceScorer(totalDocuments);
        this.popularityScorer = new PopularityScorer(documentServices);
        this.invertedIndexData = invertedIndexService.getAll();
    }

    public List<RankedDocument> rankDocuments(String[] wordsArray) {
        Map<Integer, RankedDocument> docScoresMap = new HashMap<>();

        // Create a map for faster lookup of InvertedIndexEntries by word
        Map<String, InvertedIndexEntry> wordToEntryMap = invertedIndexData.stream()
                .collect(Collectors.toMap(InvertedIndexEntry::getWord, entry -> entry));

        for (String word : wordsArray) {
            // Get the inverted index entry from the map
            InvertedIndexEntry entry = wordToEntryMap.get(word);

            if (entry == null || entry.getRankedPostings() == null || entry.getRankedPostings().isEmpty()) {
                continue;
            }

            List<RankedDocument> docsList = entry.getRankedPostings();
            int df = entry.getDf();

            for (RankedDocument doc : docsList) {
                Integer docId = doc.getDocId();
                // Use the tf value directly from the RankedDocument
                int tf = doc.getTf();

                double relevanceScore = relevanceScorer.computeTFIDF(tf, df);
                docScoresMap.compute(docId, (key, rankedDoc) -> {
                    if (rankedDoc == null) {
                        return new RankedDocument(docId, doc.getUrl(), doc.getDocTitle(), relevanceScore, 0, 0, tf);
                    } else {
                        rankedDoc.setRelevanceScore(rankedDoc.getRelevanceScore() + relevanceScore);
                        return rankedDoc;
                    }
                });
            }

            // Normalize Relevance score
            double[] sum = {0};
            docScoresMap.forEach((key, rankedDoc) -> sum[0] += rankedDoc.getRelevanceScore());
            if (sum[0] != 0) {
                docScoresMap.forEach((key, rankedDoc) -> {
                    rankedDoc.setRelevanceScore(rankedDoc.getRelevanceScore() / sum[0]);
                });
            }
        }

        // Calculate popularity scores
        docScoresMap = popularityScorer.calculatePopularityScores(docScoresMap);

        // Calculate final scores
        for (RankedDocument doc : docScoresMap.values()) {
            double normalizedRelevance = doc.getRelevanceScore();
            double normalizedPopularity = doc.getPopularityScore();
            double finalScore = (relevanceWeight * normalizedRelevance) + (popularityWeight * normalizedPopularity);
            doc.setFinalScore(finalScore);
        }

        // Convert HashMap values to a List and sort by FinalScore
        return docScoresMap.values().stream()
                .sorted(Comparator.comparingDouble(RankedDocument::getFinalScore).reversed())
                .toList();
    }
}