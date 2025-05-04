package cu.searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "search_queries")
public class SearchQuery {
    @Id
    private String id;
    private String query;
    private long timestamp;
    private int frequency; // Track how often this query is used
} 