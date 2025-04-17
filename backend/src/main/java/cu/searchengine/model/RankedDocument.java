package cu.searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "ranking_results")
public class RankedDocument {
    @Id
    private String docId;
    private String url;
    private String docTitle;
    private double RelevanceScore;
    private double PopularityScore;
    private double FinalScore;
    public RankedDocument() {
        // Required by Spring Data
    }
    public RankedDocument(String docId, String url, String docTitle, double relevanceScore, double popularityScore, double finalScore) {
        this.docId = docId;
        this.url = url;
        this.docTitle = docTitle;
        RelevanceScore = relevanceScore;
        PopularityScore = popularityScore;
        FinalScore = finalScore;
    }
    public RankedDocument(String url, String docTitle, double relevanceScore, double popularityScore, double finalScore) {
        this.url = url;
        this.docTitle = docTitle;
        RelevanceScore = relevanceScore;
        PopularityScore = popularityScore;
        FinalScore = finalScore;
    }

    @Override
    public String toString() {
        return "RankedDocument{" +
                "docId=" + docId +
                ", Title='" + docTitle + '\'' +
                ", URL='" + url + '\'' +
                ", Relevance Score=" + RelevanceScore +
                ", Popularity Score=" + PopularityScore +
                ", Final Score=" + FinalScore +
                '}';
    }
}
