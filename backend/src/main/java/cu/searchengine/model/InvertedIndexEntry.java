package cu.searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry in the inverted index, mapping a word to its document frequency
 * and a list of postings (document IDs, term frequencies, and positions).
 */
@Document(collection = "invertedIndex")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvertedIndexEntry {

    @Id
    private String word; // The term in the inverted index

    private Integer df; // Document frequency (number of documents containing the term)
    private List<IndexDocument> RankedPostings; // List of postings for documents containing the term

    // Ensure postings is never null to avoid MongoDB persistence issues
    public List<IndexDocument> getPostings() {
        if (RankedPostings == null) {
            RankedPostings = new ArrayList<>();
        }
        return RankedPostings;
    }

    public void setPostings(List<IndexDocument> existingPostings) {
        this.RankedPostings = existingPostings;
    }

    /**
     * Represents a posting entry for a specific document, containing the document ID,
     * term frequency, and positions of the term in the document.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostingEntry {
        private Integer docId; // Document ID
        private Integer tf; // Term frequency in the document
        private Integer[] priorityPositions; // Positions of the term in the document
        private String docTitle;
        private String url;

        // Ensure priorityPositions is never null to avoid MongoDB persistence issues
        public List<Integer> getPriorityPositions() {
            if (priorityPositions == null) {
                priorityPositions = new Integer[4];
            }
            return new ArrayList<>(List.of(priorityPositions));

        }
    }
}