package cu.searchengine.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Document(collection = "search_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {

    @Id
    private String id;

    private String query;

    private List<RankedDocument> results;

    private long timestamp;
}
