package cu.searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Setter
@Getter
public class IndexDocument {
    @Id
    private Integer docId;
    private String url;
    private String docTitle;
    private Integer tf;

    public IndexDocument() {
    }

    public IndexDocument(Integer docId, String url, String docTitle, Integer tf) {
        this.docId = docId;
        this.url = url;
        this.docTitle = docTitle;
        this.tf = tf;
    }

    public IndexDocument(String url, String docTitle) {
        this.url = url;
        this.docTitle = docTitle;
    }

    @Override
    public String toString() {
        return "IndexDocument{" +
                "docId=" + docId +
                ", Title='" + docTitle + '\'' +
                ", URL='" + url + '\'';
    }
}