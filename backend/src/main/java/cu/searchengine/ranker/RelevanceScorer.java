package cu.searchengine.ranker;
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
            return 0; // Avoid division by zero, or use a small value like 0.0001
        }
        return Math.log(totalDocuments / (double) documentFrequency);
    }

    public double computeTFIDF(int termFrequency, int documentFrequency) {
        double tf = computeTF(termFrequency);
        double idf = computeIDF(documentFrequency);
        return tf * idf;
    }
}
