package cu.searchengine.Search;

import cu.searchengine.controller.RankerController;
import cu.searchengine.model.RankedDocument;
import cu.searchengine.model.SearchResult;
import cu.searchengine.service.DocumentService;
import cu.searchengine.service.InvertedIndexService;
import cu.searchengine.service.SearchService;
import cu.searchengine.utils.Tokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final DocumentService documentService;
    private final InvertedIndexService invertedIndexService;
    private final SearchService searchService;
    private final Tokenizer tokenizer = new Tokenizer();

    @Autowired
    public SearchController(DocumentService documentService, InvertedIndexService invertedIndexService, SearchService searchService) {
        this.documentService = documentService;
        this.invertedIndexService = invertedIndexService;
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("q") String query, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            response.put("results", Collections.emptyList());
            response.put("pages", 0);
            return response;
        }
        String[] words = query.toLowerCase().split("\\s+");
        List<String> lemmatizedWords = new ArrayList<>();
        for (String word : words) {
            lemmatizedWords.addAll(tokenizer.tokenize(word));
        }
        int totalDocs = documentService.getNumberOfDocuments();
        RankerController ranker = new RankerController(totalDocs, documentService, invertedIndexService);

        List<RankedDocument> ranked = ranker.rankDocuments(lemmatizedWords.toArray(new String[0]));
        int from = Math.min(page * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        List<RankedDocument> pagedResults = ranked.subList(from, to);
        int totalPages = (int) Math.ceil((double) ranked.size() / size);
        response.put("results", pagedResults);
        response.put("pages", totalPages);
        return response;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        long start = System.nanoTime();
        int totalDocuments = documentService.getNumberOfDocuments();
        int indexSize = invertedIndexService.getCount();
        long end = System.nanoTime();
        stats.put("totalDocuments", totalDocuments);
        stats.put("invertedIndexSize", indexSize);
        stats.put("dbAccessTimeMs", (end - start) / 1_000_000.0);
        stats.put("memoryUsageMB", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
        stats.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    @PostMapping("/reset")
    public Map<String, String> resetDb() {
        documentService.getAllDocuments().forEach(documentService::delete);
        invertedIndexService.deleteAll();
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "reset complete");
        return resp;
    }

    @PostMapping("/testbench")
    public Map<String, Object> testBench(@RequestParam(value = "type", defaultValue = "default") String type) {
        Map<String, Object> result = new HashMap<>();
        long start = System.nanoTime();
        int totalDocs = documentService.getNumberOfDocuments();
        RankerController ranker = new RankerController(totalDocs, documentService, invertedIndexService);

        List<RankedDocument> ranked = Collections.emptyList();
        String[] testWords;
        switch (type.toLowerCase()) {
            case "single":
                testWords = new String[]{"the"};
                ranked = ranker.rankDocuments(testWords);
                result.put("testType", "single word");
                break;
            case "multi":
                testWords = new String[]{"machine", "learning"};
                ranked = ranker.rankDocuments(testWords);
                result.put("testType", "multi word");
                break;
            case "empty":
                testWords = new String[]{};
                ranked = ranker.rankDocuments(testWords);
                result.put("testType", "empty query");
                break;
            case "rare":
                testWords = new String[]{"zyzzyva"};
                ranked = ranker.rankDocuments(testWords);
                result.put("testType", "rare word");
                break;
            case "large":
                testWords = new String[100];
                Arrays.fill(testWords, "data");
                ranked = ranker.rankDocuments(testWords);
                result.put("testType", "large query (100x 'data')");
                break;
            default:
                testWords = new String[]{"the"};
                ranked = ranker.rankDocuments(testWords);
                result.put("testType", "default (single word)");
        }
        long end = System.nanoTime();
        result.put("testQuery", Arrays.toString(testWords));
        result.put("resultCount", ranked.size());
        result.put("elapsedMs", (end - start) / 1_000_000.0);
        return result;
    }

    @GetMapping("/example-ranker")
    public Map<String, Object> exampleRankerUsage() {
        int totalDocs = documentService.getNumberOfDocuments();
        RankerController ranker = new RankerController(totalDocs, documentService, invertedIndexService);

        String[] queryWords = {"java", "search"};
        String[] queryWords2 = {"java", "ranking"};

        List<RankedDocument> rankedResults = ranker.rankDocuments(queryWords);
        List<RankedDocument> rankedResults2 = ranker.rankDocuments(queryWords2);

        SearchResult result1 = SearchResult.builder().query("java Search").results(rankedResults).timestamp(System.currentTimeMillis()).build();
        searchService.saveSearchResult(result1);

        SearchResult result2 = SearchResult.builder().query("java ranking").results(rankedResults2).timestamp(System.currentTimeMillis()).build();
        searchService.saveSearchResult(result2);

        List<SearchResult> res = searchService.getResultsByQuery("java Search");

        Map<String, Object> response = new HashMap<>();
        response.put("savedResults", res);
        return response;
    }
}
