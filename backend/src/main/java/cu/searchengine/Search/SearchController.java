package cu.searchengine.Search;

import cu.searchengine.controller.RankerController;
import cu.searchengine.model.Documents;
import cu.searchengine.model.RankedDocument;
import cu.searchengine.model.SearchResult;
import cu.searchengine.service.DocumentService;
import cu.searchengine.service.InvertedIndexService;
import cu.searchengine.service.SearchService;
import cu.searchengine.utils.Tokenizer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final DocumentService documentService;
    private final InvertedIndexService invertedIndexService;
    private final SearchService searchService;
    private final Tokenizer tokenizer = new Tokenizer();
    private RankerController ranker;

    @Autowired
    public SearchController(DocumentService documentService, InvertedIndexService invertedIndexService, SearchService searchService) {
        this.documentService = documentService;
        this.invertedIndexService = invertedIndexService;
        this.searchService = searchService;
    }

    @PostConstruct
    public void init() {
        // Initialize the ranker once after all dependencies are injected
        int totalDocs = documentService.getNumberOfDocuments();
        this.ranker = new RankerController(totalDocs, documentService, invertedIndexService);
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("q") String query, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            response.put("results", Collections.emptyList());
            response.put("pages", 0);
            response.put("resultCount", 0);
            response.put("elapsedMs", 0);
            return response;
        }

        // Save the search query for suggestions asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                searchService.saveSearchQuery(query);
            } catch (Exception e) {
                System.err.println("Error saving search query: " + e.getMessage());
            }
        });

        long start = System.nanoTime();

        // Process the query with the enhanced QueryProcessor
        QueryProcessor queryProcessor = new QueryProcessor(query, tokenizer);
        List<String> lemmatizedWords = queryProcessor.getLemmatizedWords();
        List<QueryProcessor.LogicalOperation> operations = queryProcessor.getOperations();

        // Get ranked documents
        List<RankedDocument> ranked = ranker.rankDocuments(lemmatizedWords.toArray(new String[0]));

        // Calculate the range of documents we need to process
        int validPage = Math.max(0, page);
        int requiredMatches = (validPage + 1) * size; // For page 2, we need 30 matches

        // Only process the documents we need for this page
        List<RankedDocument> pagedResults;
        if (!operations.isEmpty()) {
     
            List<RankedDocument> filteredResults = filterByLogicalOperations(ranked, operations);

            // Get the correct page of results
            int from = validPage * size;
            int to = Math.min(from + size, filteredResults.size());
            pagedResults = from < filteredResults.size() ? filteredResults.subList(from, to) : Collections.emptyList();

            // Update response with filtered results count
            response.put("resultCount", filteredResults.size());
            response.put("pages", (int) Math.ceil((double) filteredResults.size() / size));
        } else {
            // For non-phrase searches, just get the page directly
            int from = validPage * size;
            int to = Math.min(from + size, ranked.size());
            pagedResults = from < ranked.size() ? ranked.subList(from, to) : Collections.emptyList();

            // Update response with total ranked count
            response.put("resultCount", ranked.size());
            response.put("pages", (int) Math.ceil((double) ranked.size() / size));
        }

        // Batch fetch all needed documents for snippet generation
        Set<Integer> docIds = pagedResults.stream().map(RankedDocument::getDocId).collect(Collectors.toSet());
        Map<Integer, Documents> docsById = new HashMap<>();
        if (!docIds.isEmpty()) {
            // Batch fetch all documents by their IDs (use a batch method)
            List<Documents> docs = documentService.getDocumentsByIds(docIds);
            for (Documents d : docs) {
                docsById.put(d.getId(), d);
            }
        }

        // Generate snippets in parallel
        Set<String> queryWordsSet = new HashSet<>(lemmatizedWords);
        pagedResults.parallelStream().forEach(doc -> {
            Documents fullDoc = docsById.get(doc.getDocId());
            if (fullDoc != null) {
                String snippet = generateSnippet(fullDoc.getContent(), queryWordsSet);
                doc.setSnippet(snippet);
            }
        });

        long end = System.nanoTime();
        response.put("elapsedMs", (end - start) / 1_000_000.0);
        response.put("results", pagedResults);
        return response;
    }

    // Helper to generate a snippet containing the query words, with some context
    private String generateSnippet(String content, Set<String> queryWords) {
        if (content == null || content.isEmpty() || queryWords.isEmpty()) return "";
        String lowerContent = content.toLowerCase();
        int snippetLen = 160;
        for (String word : queryWords) {
            int idx = lowerContent.indexOf(word.toLowerCase());
            if (idx != -1) {
                int start = Math.max(0, idx - 40);
                int end = Math.min(content.length(), idx + word.length() + 120);
                String snippet = content.substring(start, end).replaceAll("\\s+", " ");
                // Optionally escape HTML
                return StringEscapeUtils.escapeHtml4(snippet);
            }
        }
        // fallback: start of content
        return StringEscapeUtils.escapeHtml4(content.substring(0, Math.min(snippetLen, content.length())));
    }

    private List<RankedDocument> filterByLogicalOperations(List<RankedDocument> rankedDocs, List<QueryProcessor.LogicalOperation> operations) {
        return rankedDocs.parallelStream()
                .filter(doc -> {
                    try {
                        Documents fullDoc = documentService.getDocumentById(doc.getDocId());
                        if (fullDoc == null) return false;

                        String content = fullDoc.getContent();
                        String title = fullDoc.getTitle();

                        if (content == null || title == null) return false;

                        // Convert to lowercase once for case-insensitive comparison
                        String contentLower = content.toLowerCase();
                        String titleLower = title.toLowerCase();

                        // Check all logical operations
                        boolean matches = true;
                        for (QueryProcessor.LogicalOperation operation : operations) {
                            boolean operationMatch = false;

                            switch (operation.getType()) {
                                case MUST_CONTAIN:
                                    // Document must contain ALL of these terms (AND)
                                    operationMatch = operation.getTerms().stream().allMatch(phrase -> {
                                        String phraseStr = String.join(" ", phrase).toLowerCase();
                                        return contentLower.contains(phraseStr) || titleLower.contains(phraseStr);
                                    });
                                    if (!operationMatch) matches = false;
                                    break;

                                case SHOULD_CONTAIN:
                                    // Document should contain ANY of these terms (OR)
                                    operationMatch = operation.getTerms().stream().anyMatch(phrase -> {
                                        String phraseStr = String.join(" ", phrase).toLowerCase();
                                        return contentLower.contains(phraseStr) || titleLower.contains(phraseStr);
                                    });
                                    // For OR, we only need at least one match across all OR operations
                                    if (operationMatch) matches = true;
                                    break;

                                case MUST_NOT_CONTAIN:
                                    // Document must NOT contain these terms
                                    operationMatch = operation.getTerms().stream().noneMatch(phrase -> {
                                        String phraseStr = String.join(" ", phrase).toLowerCase();
                                        return contentLower.contains(phraseStr) || titleLower.contains(phraseStr);
                                    });
                                    if (!operationMatch) matches = false;
                                    break;
                            }

                            // Short-circuit if no match possible
                            if (!matches && operation.getType() != QueryProcessor.OperationType.SHOULD_CONTAIN) {
                                return false;
                            }
                        }

                        return matches;
                    } catch (Exception e) {
                        System.err.println("Error in logical operation matching for doc " + doc.getDocId() + ": " + e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    // Remaining methods unchanged...
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

    @GetMapping("/suggestions")
    public Map<String, Object> getSuggestions(@RequestParam(value = "q", required = false) String query) {
        Map<String, Object> response = new HashMap<>();
        List<String> suggestions = searchService.getSuggestions(query);
        response.put("suggestions", suggestions);
        return response;
    }

    @Getter
    public class QueryProcessor {
        private final List<String> lemmatizedWords = new ArrayList<>();
        private final List<LogicalOperation> operations = new ArrayList<>();
        private final Tokenizer tokenizer;

        public QueryProcessor(String query, Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            processQuery(query);
        }

        private void processQuery(String query) {
            // Check if the query contains logical operators
            if (query.toUpperCase().contains(" AND ") || query.toUpperCase().contains(" OR ") || query.toUpperCase().contains(" NOT ")) {
                processLogicalQuery(query);
            } else {
                // If no logical operators, use the original phrase processing
                processSingleQuery(query);
            }
        }

        private void processSingleQuery(String query) {
            // Pattern to find phrases in quotes
            Pattern phrasePattern = Pattern.compile("\"([^\"]*)\"");
            Matcher phraseMatcher = phrasePattern.matcher(query);

            // Extract phrases and replace them with empty strings
            StringBuilder remainingQuery = new StringBuilder();
            List<String[]> phrases = new ArrayList<>();

            while (phraseMatcher.find()) {
                String phrase = phraseMatcher.group(1).toLowerCase();
                if (!phrase.trim().isEmpty()) {
                    String[] phraseWords = phrase.split("\\s+");
                    // Add each phrase word to lemmatizedWords (to ensure we get documents with these words)
                    for (String word : phraseWords) {
                        lemmatizedWords.addAll(tokenizer.tokenize(word));
                    }
                    // Store the original phrase for exact matching
                    phrases.add(phraseWords);
                }
                // Replace the phrase with empty string
                phraseMatcher.appendReplacement(remainingQuery, "");
            }
            phraseMatcher.appendTail(remainingQuery);

            // Process remaining individual words
            String[] words = remainingQuery.toString().toLowerCase().split("\\s+");
            for (String word : words) {
                if (!word.trim().isEmpty()) {
                    lemmatizedWords.addAll(tokenizer.tokenize(word));
                }
            }

            // If there are phrases, create a single MUST_CONTAIN operation
            if (!phrases.isEmpty()) {
                operations.add(new LogicalOperation(OperationType.MUST_CONTAIN, phrases, null));
            }
        }

        private void processLogicalQuery(String query) {
            // First, identify the logical operators in the query (limited to 2 as per requirements)
            List<String> operators = new ArrayList<>();
            Pattern operatorPattern = Pattern.compile("\\s+(AND|OR|NOT)\\s+", Pattern.CASE_INSENSITIVE);
            Matcher operatorMatcher = operatorPattern.matcher(query);

            while (operatorMatcher.find() && operators.size() < 2) {
                operators.add(operatorMatcher.group(1).toUpperCase());
            }

            // Split the query by operators (limited to max 3 parts due to 2 operators constraint)
            String[] parts = query.split("\\s+(AND|OR|NOT)\\s+", 3);

            // Process each part separately
            for (int i = 0; i < parts.length && i <= operators.size(); i++) {
                processPart(parts[i], i > 0 ? operators.get(i-1) : null);
            }
        }

        private void processPart(String part, String operator) {
            // Extract phrases from this part
            List<String[]> phrases = new ArrayList<>();
            Pattern phrasePattern = Pattern.compile("\"([^\"]*)\"");
            Matcher phraseMatcher = phrasePattern.matcher(part);

            StringBuilder remainingWords = new StringBuilder();

            while (phraseMatcher.find()) {
                String phrase = phraseMatcher.group(1).toLowerCase();
                if (!phrase.trim().isEmpty()) {
                    String[] phraseWords = phrase.split("\\s+");
                    phrases.add(phraseWords);

                    // Add all words from phrases to lemmatizedWords for initial filtering
                    for (String word : phraseWords) {
                        lemmatizedWords.addAll(tokenizer.tokenize(word));
                    }
                }
                phraseMatcher.appendReplacement(remainingWords, "");
            }
            phraseMatcher.appendTail(remainingWords);

            // Process remaining individual words in this part
            String[] words = remainingWords.toString().toLowerCase().trim().split("\\s+");
            List<String[]> individualWords = new ArrayList<>();

            for (String word : words) {
                if (!word.trim().isEmpty()) {
                    individualWords.add(new String[]{word});
                    lemmatizedWords.addAll(tokenizer.tokenize(word));
                }
            }

            // Combine phrases and individual words
            List<String[]> allTerms = new ArrayList<>(phrases);
            allTerms.addAll(individualWords);

            // If there are any terms in this part, add an operation
            if (!allTerms.isEmpty()) {
                OperationType type = OperationType.MUST_CONTAIN; // Default

                if (operator != null) {
                    switch (operator) {
                        case "AND":
                            type = OperationType.MUST_CONTAIN;
                            break;
                        case "OR":
                            type = OperationType.SHOULD_CONTAIN;
                            break;
                        case "NOT":
                            type = OperationType.MUST_NOT_CONTAIN;
                            break;
                    }
                }

                operations.add(new LogicalOperation(type, allTerms, part.trim()));
            }
        }

        /**
         * Returns all phrases from all operations for backward compatibility
         */
        public List<String[]> getPhrases() {
            List<String[]> allPhrases = new ArrayList<>();
            for (LogicalOperation operation : operations) {
                // Only include MUST_CONTAIN phrases for backward compatibility
                if (operation.getType() == OperationType.MUST_CONTAIN) {
                    allPhrases.addAll(operation.getTerms());
                }
            }
            return allPhrases;
        }

        public enum OperationType {
            MUST_CONTAIN,     // AND
            SHOULD_CONTAIN,   // OR
            MUST_NOT_CONTAIN  // NOT
        }

        @Getter
        public static class LogicalOperation {
            private final OperationType type;
            private final List<String[]> terms;
            private final String originalText;

            public LogicalOperation(OperationType type, List<String[]> terms, String originalText) {
                this.type = type;
                this.terms = terms;
                this.originalText = originalText;
            }
        }
    }
}
