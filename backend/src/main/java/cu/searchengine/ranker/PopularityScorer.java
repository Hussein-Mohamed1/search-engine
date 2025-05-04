package cu.searchengine.ranker;

import cu.searchengine.model.Documents;
import cu.searchengine.model.RankedDocument;
import cu.searchengine.service.DocumentService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
@Component
public class PopularityScorer {
    private static final double DAMPING = 0.85;
    private static final double EPSILON = 0.0001;
    private static final int MAX_ITERATIONS = 100;
    private final DocumentService documentService;
    private final Map<Integer, Set<Integer>> webGraph;
    private final Set<Integer> allPages;
    private final Map<Integer, List<Integer>> incomingLinks;
    private final Set<Integer> danglingNodes;
    private final int numThreads;

    public PopularityScorer(DocumentService documentService) {
        this.documentService = documentService;
        this.webGraph = documentService.getWebGraph();
        this.numThreads = 128;

        // Initialize allPages
        this.allPages = new HashSet<>();
        for (Integer page : webGraph.keySet()) {
            allPages.add(page);
            allPages.addAll(webGraph.get(page));
        }

        // Initialize danglingNodes
        this.danglingNodes = new HashSet<>();
        for (Integer page : allPages) {
            if (!webGraph.containsKey(page) || webGraph.get(page).isEmpty()) {
                danglingNodes.add(page);
            }
        }

        // Initialize incomingLinks
        this.incomingLinks = new HashMap<>();
        for (Integer page : allPages) {
            incomingLinks.put(page, new ArrayList<>());
        }
        for (Map.Entry<Integer, Set<Integer>> entry : webGraph.entrySet()) {
            Integer source = entry.getKey();
            for (Integer target : entry.getValue()) {
                incomingLinks.get(target).add(source);
            }
        }
    }

    public void calculatePopularityScores() {
        Map<Integer, Double> pageRankScores = calculatePageRank();
        int size = documentService.getNumberOfDocuments();
        System.out.println("Number of documents: " + size);
        for (Map.Entry<Integer, Double> entry : pageRankScores.entrySet()) {
            Integer docId = entry.getKey();
            Double pageRankScore = entry.getValue();
                documentService.updatePopularityScore(docId , pageRankScore);
        }

    }

    private Map<Integer, Double> calculatePageRank() {
        int n = allPages.size();
        Map<Integer, Double> currPageRank = new ConcurrentHashMap<>();
        Map<Integer, Double> nextPageRank = new ConcurrentHashMap<>();

        // Initialize with equal probability
        double initialRank = 1.0 / n;
        for (Integer page : allPages) {
            currPageRank.put(page, initialRank);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        boolean converged = false;
        int iterations = 0;

        try {
            while (!converged && iterations < MAX_ITERATIONS) {
                // Calculate dangling node contribution
                double danglingWeight = 0;
                for (Integer page : danglingNodes) {
                    danglingWeight += currPageRank.get(page);
                }
                danglingWeight = DAMPING * danglingWeight / n;

                double randomJumpProbability = (1 - DAMPING) / n;

                // Divide pages into chunks for parallel processing
                List<Integer> pageList = new ArrayList<>(allPages);
                int chunkSize = Math.max(1, pageList.size() / numThreads);
                List<List<Integer>> pageChunks = new ArrayList<>();
                for (int i = 0; i < pageList.size(); i += chunkSize) {
                    pageChunks.add(pageList.subList(i, Math.min(i + chunkSize, pageList.size())));
                }

                // Create tasks for each chunk
                double finalDanglingWeight = danglingWeight;
                List<Callable<Void>> tasks = pageChunks.stream().map(chunk -> (Callable<Void>) () -> {
                    for (Integer page : chunk) {
                        double sum = 0;
                        for (Integer sourceNode : incomingLinks.get(page)) {
                            int outDegree = webGraph.get(sourceNode).size();
                            sum += currPageRank.get(sourceNode) / outDegree;
                        }
                        nextPageRank.put(page, randomJumpProbability + DAMPING * sum + finalDanglingWeight);
                    }
                    return null;
                }).collect(Collectors.toList());

                // Execute tasks and wait for completion
                executor.invokeAll(tasks);

                // Check for convergence
                double diff = 0;
                for (Integer page : allPages) {
                    diff += Math.abs(nextPageRank.get(page) - currPageRank.get(page));
                }

                if (diff < EPSILON) {
                    converged = true;
                }

                // Update PageRank values for next iteration
                for (Integer page : allPages) {
                    currPageRank.put(page, nextPageRank.get(page));
                }

                iterations++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PageRank calculation interrupted", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Normalize PageRank values
        double sum = 0;
        for (double rank : currPageRank.values()) {
            sum += rank;
        }

        for (Integer page : currPageRank.keySet()) {
            currPageRank.put(page, currPageRank.get(page) / sum);
        }

        return currPageRank;
    }
}