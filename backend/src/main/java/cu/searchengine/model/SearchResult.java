package cu.searchengine.model;

import java.util.Comparator;
import java.util.PriorityQueue;

public class SearchResult {
    private final PriorityQueue<RankedDocument> rankedDocuments = new PriorityQueue<>(Comparator.comparingDouble(RankedDocument::getFinalScore).reversed());

    public boolean addDocument(RankedDocument document) {
        return rankedDocuments.offer(document);
    }

    public RankedDocument getTopDocument() {
        return rankedDocuments.poll();
    }

    public PriorityQueue<RankedDocument> getRankedDocuments() {
        return rankedDocuments;
    }
}
