package com.searchengine.model;

public class RankedDocument {
    private int docId;
    private String url;
    private String docTitle;
    private double RelevanceScore;
    private double PopularityScore;
    private double FinalScore;

    public RankedDocument(int docId, String url, String docTitle, double relevanceScore, double popularityScore, double finalScore) {
        this.docId = docId;
        this.url = url;
        this.docTitle = docTitle;
        RelevanceScore = relevanceScore;
        PopularityScore = popularityScore;
        FinalScore = finalScore;
    }

    public int getDocId() {
        return docId;
    }

    public String getUrl() {
        return url;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public double getPopularityScore() {
        return PopularityScore;
    }

    public double getRelevanceScore() {
        return RelevanceScore;
    }

    public double getFinalScore() {
        return FinalScore;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public void setRelevanceScore(double relevanceScore) {
        RelevanceScore = relevanceScore;
    }

    public void setFinalScore(double finalScore) {
        FinalScore = finalScore;
    }

    public void setPopularityScore(double popularityScore) {
        PopularityScore = popularityScore;
    }
}
