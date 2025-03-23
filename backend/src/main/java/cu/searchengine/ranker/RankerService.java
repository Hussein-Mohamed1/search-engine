package cu.searchengine.ranker;

public class RankerService {
    private final RelevanceScorer  relevanceScorer;
    private final PopularityScorer popularityScorer;
    public RankerService(int totalDocs) {
        this.relevanceScorer = new RelevanceScorer(totalDocs);
        this.popularityScorer = new PopularityScorer(); // now it is a plase holder
    }
    public double rankDocument(int termFrequency, int documentFrequency , double pageRank) {
        double relevanceScore = relevanceScorer.computeTFIDF(termFrequency, documentFrequency);
        double popularityScore = 0;
//       double popularityScore = popularityScorer.computeScore(pageRank); todo
        return 0.7 * relevanceScore + 0.3 * popularityScore;
    }

}
