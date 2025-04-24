
package cu.searchengine.ranker;

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

    public double computeTF(int termFrequency) {
        return (double) termFrequency; // Keeping it double for possible future normalization
    }

    public double computeIDF(int documentFrequency) {
        if (documentFrequency == 0) {
            return 0; // Avoid division by zero, or use a small value like 0.0001
        }
        return Math.log(totalDocuments / (double) documentFrequency);
    }

    public double computeTFIDF(int termFrequency, int documentFrequency) {
        double tf = computeTF(termFrequency);
        double idf = computeIDF(documentFrequency);
        return tf * idf;
    }
    public void computeRelevanceScoresParallel(Map<Integer, RankedDocument> docScoresMap,
                                               String[] wordsArray,
                                               Map<String, List<RankedDocument>> wordToDocsMap,
                                               Map<String, Integer> wordToDfMap) {
        // Process each word in parallel
        CountDownLatch wordsLatch = new CountDownLatch(wordsArray.length);

        for (String word : wordsArray) {
            executorService.submit(() -> {
                try {
                    List<RankedDocument> docsList = wordToDocsMap.get(word);
                    Integer df = wordToDfMap.get(word);

                    if (docsList == null || df == null || docsList.isEmpty()) {
                        return;
                    }

                    // Process each document for this word
                    synchronized (docScoresMap) {
                        for (RankedDocument doc : docsList) {
                            Integer docId = doc.getDocId();
                            int tf = doc.getTf();

                            double relevanceScore = computeTFIDF(tf, df);
                            docScoresMap.compute(docId, (key, rankedDoc) -> {
                                if (rankedDoc == null) {
                                    return new RankedDocument(docId, doc.getUrl(), doc.getDocTitle(), relevanceScore, 0, 0, tf);
                                } else {
                                    rankedDoc.setRelevanceScore(rankedDoc.getRelevanceScore() + relevanceScore);
                                    return rankedDoc;
                                }
                            });
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
