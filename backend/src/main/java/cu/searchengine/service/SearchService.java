package cu.searchengine.service;

import cu.searchengine.model.SearchResult;
import cu.searchengine.repository.SearchResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final SearchResultRepository searchResultRepository;

    @Autowired
    public SearchService(SearchResultRepository searchResultRepository) {
        this.searchResultRepository = searchResultRepository;
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
}
