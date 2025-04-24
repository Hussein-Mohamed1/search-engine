package cu.searchengine.ranker;

import cu.searchengine.model.RankedDocument;
import cu.searchengine.service.DocumentService;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.util.concurrent.AtomicDouble;


public class PopularityScorer {
    private static final double DAMPING = 0.85;
    private static final double EPSILON = 0.0001;
    private static final int MAX_ITERATIONS = 100;
    private final DocumentService documentService;
    // Number of threads to use - adjust based on available cores
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService;

    public PopularityScorer(DocumentService documentService) {
        this.documentService = documentService;
        this.executorService = Executors.newFixedThreadPool(NUM_THREADS);
    }

    public Map<Integer, RankedDocument> calculatePopularityScores(Map<Integer, RankedDocument> documents) {
        // build the web graph from the documents
        Map<Integer, Set<Integer>> webGraph = documentService.getWebGraph();

        Map<Integer, Double> pageRankScores = calculatePageRank(webGraph);

        for (Map.Entry<Integer, Double> entry : pageRankScores.entrySet()) {
            Integer docId = entry.getKey();
            Double pageRankScore = entry.getValue();

            if (documents.containsKey(docId)) {
                // Update the document's popularity score with its PageRank
                documents.get(docId).setPopularityScore(pageRankScore);
            }
        }

        // Shutdown the executor service after we're done
        executorService.shutdown();

        return documents;
    }

    private Map<Integer, Double> calculatePageRank(Map<Integer, Set<Integer>> links) {
        // Get all unique pages
        Set<Integer> allPages = new HashSet<>();
        for (Integer page : links.keySet()) {
            allPages.add(page);
            allPages.addAll(links.get(page));
        }

        int n = allPages.size();

        ConcurrentHashMap<Integer, Double> currPageRank = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Double> nextPageRank = new ConcurrentHashMap<>();

        // Initialize with equal probability
        double initialRank = 1.0 / n;
        for (Integer page : allPages) {
            currPageRank.put(page, initialRank);
        }

        // Identify pages with no outgoing links (dangling nodes)
        Set<Integer> danglingNodes = new HashSet<>();
        for (Integer page : allPages) {
            if (!links.containsKey(page) || links.get(page).isEmpty()) {
                danglingNodes.add(page);
            }
        }

        // Create reverse Graph
        Map<Integer, List<Integer>> incomingLinks = new HashMap<>();
        for (Integer page : allPages) {
            incomingLinks.put(page, new ArrayList<>());
        }

        for (Map.Entry<Integer, Set<Integer>> entry : links.entrySet()) {
            Integer source = entry.getKey();
            for (Integer target : entry.getValue()) {
                incomingLinks.get(target).add(source);
            }
        }

        boolean converged = false;
        int iterations = 0;

        // Lists to hold pages for each thread to process
        List<List<Integer>> partitions = partitionPages(new ArrayList<>(allPages), NUM_THREADS);

        while (!converged && iterations < MAX_ITERATIONS) {
            // Calculate dangling node contribution (can be done in parallel)
            AtomicDouble danglingWeightTotal = new AtomicDouble(0);

            // Calculate dangling weight in parallel
            CountDownLatch danglingLatch = new CountDownLatch(1);
            executorService.submit(() -> {
                double weight = 0;
                for (Integer page : danglingNodes) {
                    weight += currPageRank.get(page);
                }
                danglingWeightTotal.set(DAMPING * weight / n);
                danglingLatch.countDown();
            });

            try {
                danglingLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while calculating dangling weight", e);
            }

            double danglingWeight = danglingWeightTotal.get();
            double randomJumpProbability = (1 - DAMPING) / n;

            // Use CountDownLatch to wait for all threads to finish their calculations
            CountDownLatch iterationLatch = new CountDownLatch(partitions.size());

            // Process each partition in parallel
            for (List<Integer> partition : partitions) {
                executorService.submit(() -> {
                    try {
                        for (Integer page : partition) {
                            double sum = 0;
                            for (Integer sourceNode : incomingLinks.get(page)) {
                                // Contribution from incoming links
                                if (links.containsKey(sourceNode)) {
                                    int outDegree = links.get(sourceNode).size();
                                    sum += currPageRank.get(sourceNode) / outDegree;
                                }
                            }
                            nextPageRank.put(page, randomJumpProbability + DAMPING * sum + danglingWeight);
                        }
                    } finally {
                        iterationLatch.countDown();
                    }
                });
            }

            try {
                iterationLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while calculating page ranks", e);
            }

            // Check for convergence in parallel
            AtomicDouble diff = new AtomicDouble(0);
            CountDownLatch convergenceLatch = new CountDownLatch(partitions.size());

            for (List<Integer> partition : partitions) {
                executorService.submit(() -> {
                    try {
                        double partialDiff = 0;
                        for (Integer page : partition) {
                            partialDiff += Math.abs(nextPageRank.get(page) - currPageRank.get(page));
                        }
                        diff.addAndGet(partialDiff);
                    } finally {
                        convergenceLatch.countDown();
                    }
                });
            }

            try {
                convergenceLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while checking convergence", e);
            }

            if (diff.get() < EPSILON) {
                converged = true;
            }

            // Update PageRank values for next iteration
            for (Integer page : allPages) {
                currPageRank.put(page, nextPageRank.get(page));
            }

            iterations++;
        }

        // Normalize PageRank values
        AtomicDouble sum = new AtomicDouble(0);
        CountDownLatch normalizationLatch = new CountDownLatch(partitions.size());

        // Calculate sum in parallel
        for (List<Integer> partition : partitions) {
            executorService.submit(() -> {
                try {
                    double partialSum = 0;
                    for (Integer page : partition) {
                        partialSum += currPageRank.get(page);
                    }
                    sum.addAndGet(partialSum);
                } finally {
                    normalizationLatch.countDown();
                }
            });
        }

        try {
            normalizationLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while normalizing", e);
        }

        double totalSum = sum.get();

        // Apply normalization in parallel
        CountDownLatch applyNormalizationLatch = new CountDownLatch(partitions.size());
        for (List<Integer> partition : partitions) {
            executorService.submit(() -> {
                try {
                    for (Integer page : partition) {
                        currPageRank.put(page, currPageRank.get(page) / totalSum);
                    }
                } finally {
                    applyNormalizationLatch.countDown();
                }
            });
        }

        try {
            applyNormalizationLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while applying normalization", e);
        }

        return new HashMap<>(currPageRank);
    }

    /**
     * Splits the list of pages into roughly equal partitions for parallel processing
     */
    private List<List<Integer>> partitionPages(List<Integer> pages, int numPartitions) {
        List<List<Integer>> partitions = new ArrayList<>(numPartitions);

        for (int i = 0; i < numPartitions; i++) {
            partitions.add(new ArrayList<>());
        }

        for (int i = 0; i < pages.size(); i++) {
            partitions.get(i % numPartitions).add(pages.get(i));
        }

        return partitions;
    }
}