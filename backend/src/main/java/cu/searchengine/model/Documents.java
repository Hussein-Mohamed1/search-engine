package cu.searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;

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

    @Getter
    @Setter
    private boolean invertedIndexProcessed = false;
    private Set<Integer> webGraph;

    @Getter
    private Set<Integer> incomingLinks;

    public Documents() {
    }

    public Documents(int id, String url, String title, List<String> mainHeading, List<String> subHeading, String content, List<String> Links, Set<Integer> webGraph) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.mainHeading = mainHeading;
        this.subHeadings = subHeading;
        this.content = content;
        this.links = Links;
        this.webGraph = webGraph;
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

    @Override
    public int compareTo(Documents documents) {
        return this.title.compareTo(documents.getTitle());
    }
}
