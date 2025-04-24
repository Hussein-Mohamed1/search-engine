package cu.searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Setter
@Getter
@Document(collection = "documents")
public class Documents implements Comparable<Documents> {
    @Id
    private Integer id; // Unique identifier for the document

    @Indexed(unique = true)
    private String url;
    private String title;
    private List<String> mainHeading;
    private List<String> subHeadings;
    private String content;
    private List<String> links;
    private boolean invertedIndexProcessed = false;

    public Documents() {
    }

    public Documents(int id, String url, String title, List<String> mainHeading, List<String> subHeading, String content, List<String> Links) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.mainHeading = mainHeading;
        this.subHeadings = subHeading;
        this.content = content;
        this.links = Links;
    }

    // Alternative constructor for auto-generating IDs
    public Documents(String url, String title, List<String> mainHeading, List<String> subHeading, String content, List<String> Links) {
        this.id = url.hashCode(); // Generate ID based on URL (ensures consistency)
        this.url = url;
        this.title = title;
        this.mainHeading = mainHeading;
        this.subHeadings = subHeading;
        this.content = content;
        this.links = Links;
    }

    public boolean isInvertedIndexProcessed() {
        return invertedIndexProcessed;
    }

    public void setInvertedIndexProcessed(boolean invertedIndexProcessed) {
        this.invertedIndexProcessed = invertedIndexProcessed;
    }

    @Override
    public int compareTo(Documents documents) {
        return this.title.compareTo(documents.getTitle());
    }
}
