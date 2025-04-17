package cu.searchengine.Indexer;

import cu.searchengine.model.WebDocument;
import cu.searchengine.utils.Tokenizer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



public class ThreadPool {
    private final List<WebDocument> docs;
    private final ConcurrentHashMap<String, PostingData> globalIndex = new ConcurrentHashMap<>();



    public ThreadPool(List<WebDocument> docs) {
        this.docs = docs;
    }

    public void implementThreading() {
        int numThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int batchSize = (int) Math.ceil((double) docs.size() / numThreads);

        for (int i = 0; i < docs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, docs.size());
            List<WebDocument> batch = docs.subList(i, end);
            executor.execute(new DocumentProcessorTask(batch, globalIndex));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("All threads have finished execution!");

        System.out.println("Indexing Complete! Final Index Size: " + globalIndex.size());
    }

    private static class DocumentProcessorTask implements Runnable {
        private final List<WebDocument> documents;
        private final Map<String, PostingData> globalIndex;
        public DocumentProcessorTask(List<WebDocument> documents, Map<String, PostingData> globalIndex) {
            this.documents = documents;
            this.globalIndex = globalIndex;

        }

        @Override
        public void run() {
            BuildInvertedIndex localIndex = new BuildInvertedIndex(documents,new Tokenizer()); // Build index for batch

            synchronized (globalIndex) {
                for (Map.Entry<String, PostingData> entry : localIndex.getInvertedIndex().entrySet()) {
                    globalIndex.putIfAbsent(entry.getKey(), new PostingData());
                    globalIndex.get(entry.getKey()).merge(entry.getValue());
                }
            }
        }
    }
}
