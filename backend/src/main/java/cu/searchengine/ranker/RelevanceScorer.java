package cu.searchengine.ranker;

import cu.searchengine.model.IndexDocument;
import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.model.RankedDocument;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

public class RelevanceScorer {
    private final int totalDocuments;

    public RelevanceScorer(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public double computeTF(int termFrequency) {
        return (double) termFrequency; // Keeping it double for possible future normalization
    }

    public double computeIDF(int documentFrequency) {
        if (documentFrequency == 0) {
            return 0; // Avoid division by zero
        }
        return Math.log(totalDocuments / (double) documentFrequency);
    }

    public Map<Integer, RankedDocument> calculateRelevanceScores(String[] wordsArray,
                                                                 Map<String, InvertedIndexEntry> wordToEntryMap) {
        // Use ConcurrentHashMap to collect postings by docId
        Map<Integer, List<PostingInfo>> docToPostings = new ConcurrentHashMap<>();
        // Use DoubleAdder for thread-safe sum accumulation
        DoubleAdder scoreSum = new DoubleAdder();

        // Step 1: Aggregate postings by docId in parallel
        Arrays.stream(wordsArray)
                .parallel()
                .filter(wordToEntryMap::containsKey)
                .forEach(word -> {
                    InvertedIndexEntry entry = wordToEntryMap.get(word);
                    if (entry == null || entry.getRankedPostings() == null) {
                        return;
                    }

                    int df = entry.getDf();
                    double idf = computeIDF(df); // Precompute IDF for this word

                    for (IndexDocument doc : entry.getRankedPostings()) {
                        Integer docId = doc.getDocId();
                        docToPostings.computeIfAbsent(docId, k -> Collections.synchronizedList(new ArrayList<>()))
                                .add(new PostingInfo(doc.getTf(), idf, doc.getUrl(), doc.getDocTitle()));
                    }
                });

        // Step 2: Compute relevance scores for each docId
        Map<Integer, RankedDocument> docScoresMap = new ConcurrentHashMap<>();
        docToPostings.forEach((docId, postings) -> {
            double relevanceScore = 0;
            String url = null;
            String title = null;

            // Sum TF-IDF contributions from all postings for this docId
            for (PostingInfo posting : postings) {
                relevanceScore += posting.tf * posting.idf;
                if (url == null) {
                    url = posting.url; // Use the first non-null URL
                    title = posting.title; // Use the first non-null title
                }
            }

            scoreSum.add(relevanceScore);
            docScoresMap.put(docId, new RankedDocument(docId, url, title, relevanceScore, 0, 0, postings.get(0).tf));
        });

        // Step 3: Normalize scores if sum is non-zero
        double totalScore = scoreSum.doubleValue();
        if (totalScore > 0) {
            docScoresMap.forEach((docId, rankedDoc) -> {
                rankedDoc.setRelevanceScore(rankedDoc.getRelevanceScore() / totalScore);
            });
        }

        return docScoresMap;
    }

    // Helper class to store posting information
    private static class PostingInfo {
        final int tf;
        final double idf;
        final String url;
        final String title;

        PostingInfo(int tf, double idf, String url, String title) {
            this.tf = tf;
            this.idf = idf;
            this.url = url;
            this.title = title;
        }
    }
}