package com.searchengine.controller;

import com.searchengine.model.RankedDocument;
import com.searchengine.ranker.PopularityScorer;
import com.searchengine.ranker.RelevanceScorer;

import java.util.*;

public class RankerController {
    private RelevanceScorer relevanceScorer;
    private PopularityScorer popularityScorer;

    // Dummy Inverted Index Data for Testing
    private Map<String, List<RankedDocument>> invertedIndexData;

    public RankerController(int totalDocuments) {
        relevanceScorer = new RelevanceScorer(totalDocuments);
        // popularityScorer = new PopularityScorer(); // TODO

        // Initialize Dummy Data
        invertedIndexData = new HashMap<>();
        initializeDummyData();
    }

    private void initializeDummyData() {
        invertedIndexData.put("java", Arrays.asList(
                new RankedDocument(1, "http://example.com/doc1", "Java Basics", 3, 10, 0),
                new RankedDocument(2, "http://example.com/doc2", "Advanced Java", 2, 15, 0)
        ));

        invertedIndexData.put("search", Arrays.asList(
                new RankedDocument(1, "http://example.com/doc1", "Java Basics", 5, 12, 0),
                new RankedDocument(3, "http://example.com/doc3", "Search Algorithms", 4, 20, 0)
        ));

        invertedIndexData.put("ranking", Arrays.asList(
                new RankedDocument(2, "http://example.com/doc2", "Advanced Java", 1, 5, 0),
                new RankedDocument(3, "http://example.com/doc3", "Search Algorithms", 6, 8, 0)
        ));
    }
    public List<RankedDocument> rankDocuments(String[] wordsArray) {
        Map<Integer, RankedDocument> docScoresMap = new HashMap<>();

        for (String word : wordsArray) {
            if (!invertedIndexData.containsKey(word)) continue;

            List<RankedDocument> docsList = invertedIndexData.get(word);

            for (RankedDocument doc : docsList) {
                int docId = doc.getDocId();
                int tf = (int)doc.getRelevanceScore();
                int df = (int)doc.getPopularityScore();

                double relevanceScore = relevanceScorer.computeTFIDF(tf , df);
                docScoresMap.compute(docId , (key , rankedDoc)-> {
                    if (rankedDoc == null) {
                        return new RankedDocument(docId, doc.getUrl(), doc.getDocTitle(), relevanceScore, 0, 0);
                    } else {
                        rankedDoc.setRelevanceScore(rankedDoc.getRelevanceScore() + relevanceScore);
                        return rankedDoc;
                    }
                });
            }
        }

        // Convert HashMap values to a List and sort by FinalScore
        return docScoresMap.values().stream()
                .sorted(Comparator.comparingDouble(RankedDocument::getFinalScore).reversed())
                .toList();
    }

}
