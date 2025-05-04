package cu.searchengine.service;

import cu.searchengine.model.SearchQuery;
import cu.searchengine.model.SearchResult;
import cu.searchengine.repository.SearchQueryRepository;
import cu.searchengine.repository.SearchResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final SearchResultRepository searchResultRepository;
    private final SearchQueryRepository searchQueryRepository;

    @Autowired
    public SearchService(SearchResultRepository searchResultRepository, SearchQueryRepository searchQueryRepository) {
        this.searchResultRepository = searchResultRepository;
        this.searchQueryRepository = searchQueryRepository;
    }

    public List<SearchResult> getResultsByQuery(String query) {
        return searchResultRepository.findByQuery(query);
    }

    public void saveSearchResult(SearchResult result) {
        searchResultRepository.save(result);
    }

    public void saveAll(List<SearchResult> results) {
        searchResultRepository.saveAll(results);
    }

    public void saveSearchQuery(String query) {
        SearchQuery existingQuery = searchQueryRepository.findByQueryIgnoreCase(query);
        if (existingQuery != null) {
            // Update existing query
            existingQuery.setFrequency(existingQuery.getFrequency() + 1);
            existingQuery.setTimestamp(System.currentTimeMillis());
            searchQueryRepository.save(existingQuery);
        } else {
            // Create new query
            SearchQuery newQuery = SearchQuery.builder()
                .query(query)
                .timestamp(System.currentTimeMillis())
                .frequency(1)
                .build();
            searchQueryRepository.save(newQuery);
        }
    }

    public List<String> getSuggestions(String partialQuery) {
        if (partialQuery == null || partialQuery.trim().isEmpty()) {
            // Return most frequent searches if no partial query
            return searchQueryRepository.findTop10ByOrderByFrequencyDesc()
                .stream()
                .map(SearchQuery::getQuery)
                .collect(Collectors.toList());
        }

        // Get suggestions that start with the partial query
        List<String> prefixMatches = searchQueryRepository.findByQueryStartingWithIgnoreCaseOrderByFrequencyDesc(partialQuery)
            .stream()
            .map(SearchQuery::getQuery)
            .collect(Collectors.toList());

        // If we don't have enough prefix matches, add some containing matches
        if (prefixMatches.size() < 5) {
            List<String> containingMatches = searchQueryRepository.findByQueryContainingIgnoreCaseOrderByFrequencyDesc(partialQuery)
                .stream()
                .map(SearchQuery::getQuery)
                .filter(q -> !prefixMatches.contains(q)) // Avoid duplicates
                .limit(5 - prefixMatches.size())
                .collect(Collectors.toList());
            prefixMatches.addAll(containingMatches);
        }

        return prefixMatches.stream()
            .distinct() // Remove any duplicates
            .limit(5) // Limit to 5 suggestions
            .collect(Collectors.toList());
    }
}
