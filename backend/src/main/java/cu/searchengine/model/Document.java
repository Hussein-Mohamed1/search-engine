package cu.searchengine.model;

import java.util.List;

public class Document implements Comparable<Document> {
    private int id; // Unique identifier for the document
    private String url;
    private String title;
    private String mainHeading;
    private List<String> subHeading;
    private String content;
    private List<String> Links;

    public Document(int id, String url, String title, String mainHeading, List<String> subHeading, String content, List<String> Links) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.mainHeading = mainHeading;
        this.subHeading = subHeading;
        this.content = content;
        this.Links = Links;
    }

    // Alternative constructor for auto-generating IDs
    public Document(String url, String title, String mainHeading, List<String> subHeading, String content, List<String> Links) {
        this.id = url.hashCode(); // Generate ID based on URL (ensures consistency)
        this.url = url;
        this.title = title;
        this.mainHeading = mainHeading;
        this.subHeading = subHeading;
        this.content = content;
        this.Links = Links;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMainHeading() {
        return mainHeading;
    }

    public void setMainHeading(String mainHeading) {
        this.mainHeading = mainHeading;
    }

    public List<String> getSubHeading() {
        return subHeading;
    }

    public void setSubHeading(List<String> subHeading) {
        this.subHeading = subHeading;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getLinks() {
        return Links;
    }

    public void setLinks(List<String> links) {
        Links = links;
    }

    @Override
    public int compareTo(Document o) {
        return this.title.compareTo(o.getTitle());//sort by title
    }
}
