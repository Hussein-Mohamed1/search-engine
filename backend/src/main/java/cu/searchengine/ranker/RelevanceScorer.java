package cu.searchengine.ranker;

import cu.searchengine.model.IndexDocument;
import cu.searchengine.model.RankedDocument;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class RelevanceScorer {
    private final int totalDocuments;
    private final int numThreads;
    private final ExecutorService executorService;

    public RelevanceScorer(int totalDocuments) {
        this.totalDocuments = totalDocuments;
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(numThreads);
    }

    public RelevanceScorer(int totalDocuments, int numThreads) {
        this.totalDocuments = totalDocuments;
        this.numThreads = numThreads > 0 ? numThreads : Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(this.numThreads);
    }

    public double computeTFIDF(int tf, int df) {
        if (df == 0) return 0;
        double idf = Math.log10((double) totalDocuments / df);
        return tf * idf;
    }

    public void computeRelevanceScoresParallel(Map<Integer, RankedDocument> docScoresMap,
                                               String[] wordsArray,
                                               Map<String, List<IndexDocument>> wordToDocsMap,
                                               Map<String, Integer> wordToDfMap) {
        // Create partitions of words to process
        List<List<String>> wordPartitions = partitionArray(wordsArray, numThreads);
        CountDownLatch wordsLatch = new CountDownLatch(wordPartitions.size());

        // Process each partition in a separate thread
        for (List<String> wordPartition : wordPartitions) {
            executorService.submit(() -> {
                try {
                    // Process all words in this partition
                    for (String word : wordPartition) {
                        List<IndexDocument> docsList = wordToDocsMap.get(word);
                        Integer df = wordToDfMap.get(word);

                        if (docsList == null || df == null || docsList.isEmpty()) {
                            continue;
                        }

                        // Calculate scores for each document for this word
                        Map<Integer, Double> partialScores = new HashMap<>();

                        for (IndexDocument doc : docsList) {
                            Integer docId = doc.getDocId();
                            int tf = doc.getTf();

                            double relevanceScore = computeTFIDF(tf, df);
                            partialScores.put(docId, relevanceScore);
                        }

                        // Update the shared map with synchronization
                        synchronized (docScoresMap) {
                            for (Map.Entry<Integer, Double> entry : partialScores.entrySet()) {
                                Integer docId = entry.getKey();
                                Double score = entry.getValue();

                                docScoresMap.compute(docId, (key, rankedDoc) -> {
                                    if (rankedDoc == null) {
                                        IndexDocument doc = docsList.stream()
                                                .filter(d -> d.getDocId().equals(docId))
                                                .findFirst()
                                                .orElse(null);

                                        if (doc != null) {
                                            return new RankedDocument(docId, doc.getUrl(), doc.getDocTitle(),
                                                    score, 0, 0, doc.getTf());
                                        }
                                        return null;
                                    } else {
                                        rankedDoc.setRelevanceScore(rankedDoc.getRelevanceScore() + score);
                                        return rankedDoc;
                                    }
                                });
                            }
                        }
                    }
                } finally {
                    wordsLatch.countDown();
                }
            });
        }

        try {
            wordsLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while computing relevance scores", e);
        }
    }

    public void normalizeScoresParallel(Map<Integer, RankedDocument> docScoresMap) {
        // Calculate sum in parallel
        AtomicReference<Double> sumRef = new AtomicReference<>(0.0);
        List<Map.Entry<Integer, RankedDocument>> entries = new ArrayList<>(docScoresMap.entrySet());

        // Partition the entries
        List<List<Map.Entry<Integer, RankedDocument>>> partitions = partitionEntries(entries, numThreads);
        CountDownLatch sumLatch = new CountDownLatch(partitions.size());

        for (List<Map.Entry<Integer, RankedDocument>> partition : partitions) {
            executorService.submit(() -> {
                try {
                    double partialSum = 0;
                    for (Map.Entry<Integer, RankedDocument> entry : partition) {
                        partialSum += entry.getValue().getRelevanceScore();
                    }

                    // Add to the total sum atomically
                    synchronized (sumRef) {
                        sumRef.set(sumRef.get() + partialSum);
                    }
                } finally {
                    sumLatch.countDown();
                }
            });
        }

        try {
            sumLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while calculating sum for normalization", e);
        }

        final double sum = sumRef.get();
        if (sum <= 0) {
            return; // Skip normalization if sum is zero
        }

        // Normalize scores in parallel
        CountDownLatch normalizeLatch = new CountDownLatch(partitions.size());

        for (List<Map.Entry<Integer, RankedDocument>> partition : partitions) {
            executorService.submit(() -> {
                try {
                    for (Map.Entry<Integer, RankedDocument> entry : partition) {
                        RankedDocument doc = entry.getValue();
                        doc.setRelevanceScore(doc.getRelevanceScore() / sum);
                    }
                } finally {
                    normalizeLatch.countDown();
                }
            });
        }

        try {
            normalizeLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while normalizing scores", e);
        }
    }

    private <T> List<List<T>> partitionEntries(List<T> entries, int numPartitions) {
        List<List<T>> partitions = new ArrayList<>(numPartitions);

        for (int i = 0; i < numPartitions; i++) {
            partitions.add(new ArrayList<>());
        }

        for (int i = 0; i < entries.size(); i++) {
            partitions.get(i % numPartitions).add(entries.get(i));
        }

        return partitions;
    }

    private List<List<String>> partitionArray(String[] array, int numPartitions) {
        List<List<String>> partitions = new ArrayList<>(numPartitions);

        for (int i = 0; i < numPartitions; i++) {
            partitions.add(new ArrayList<>());
        }

        for (int i = 0; i < array.length; i++) {
            partitions.get(i % numPartitions).add(array[i]);
        }

        return partitions;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}