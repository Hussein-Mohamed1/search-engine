package cu.searchengine.Indexer;

import java.util.HashMap;
import java.util.Map;

public class PostingData {
    private int df; // Document Frequency (DF)
    private Map<Integer, Posting> postings; // DocID -> Posting

    public PostingData() {
        this.df = 0;
        this.postings = new HashMap<>();
    }

    public int getDf() { return df; }
    public Map<Integer, Posting> getPostings() { return postings; }

    public void addPosting(int docId, Posting posting) {
        postings.putIfAbsent(docId, posting);
    }

    public void updateDf() {
        this.df = postings.size(); // DF = Number of unique documents
    }

    public void merge(PostingData other) {
        for (Map.Entry<Integer, Posting> entry : other.getPostings().entrySet()) {
            postings.putIfAbsent(entry.getKey(), entry.getValue()); // Merge postings
        }
        updateDf(); // Update document frequency after merging
    }
}

