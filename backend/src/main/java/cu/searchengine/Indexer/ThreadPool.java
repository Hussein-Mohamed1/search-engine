package cu.searchengine.Indexer;

import cu.searchengine.model.Documents;
import cu.searchengine.model.RankedDocument;
import cu.searchengine.model.WebDocument;
import cu.searchengine.utils.Tokenizer;
import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.service.InvertedIndexService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    private final List<Documents> docs;
    private final ConcurrentHashMap<String, PostingData> globalIndex = new ConcurrentHashMap<>();
    private final InvertedIndexService invertedIndexService;

    // Constructor now takes InvertedIndexService instead of MongoTemplate
    public ThreadPool(List<Documents> docs, InvertedIndexService invertedIndexService) {
        this.docs = docs;
        this.invertedIndexService = invertedIndexService;
    }

    // Refactor threading implementation to use InvertedIndexService
    public void implementThreading() {
        int numThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int batchSize = (int) Math.ceil((double) docs.size() / numThreads);

        // Break documents into batches for parallel processing
        for (int i = 0; i < docs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, docs.size());
            List<Documents> batch = docs.subList(i, end);
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

        // Use InvertedIndexService to bulk insert the entries
        bulkInsertInvertedIndex();
    }

    // Refactored to use InvertedIndexService for bulk insert
    public void bulkInsertInvertedIndex() {
        List<InvertedIndexEntry> indexEntries = new ArrayList<>();
        for (Map.Entry<String, PostingData> entry : globalIndex.entrySet()) {
            String word = entry.getKey();
            PostingData data = entry.getValue();
            List<RankedDocument> postingEntries = new ArrayList<>();
            for (Map.Entry<Integer, Posting> p : data.getPostings().entrySet()) {
                Posting posting = p.getValue();
                postingEntries.add(new RankedDocument(
                        p.getKey(),
                        posting.getUrl(),
                        posting.getTitle(),
                        0,
                        0,
                        0,
                        posting.getTf()
                ));
            }
            indexEntries.add(new InvertedIndexEntry(word, data.getDf(), postingEntries));
        }

        if (!indexEntries.isEmpty()) {
            System.out.println("Index entries to insert: " + indexEntries.size());
            System.out.println("Sample entry: " + indexEntries.get(0));
            try {
                invertedIndexService.saveAll(indexEntries);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
            System.out.println("✅ Inverted index inserted. Total terms: " + indexEntries.size());
            // Verify insertion
            List<InvertedIndexEntry> savedEntries = invertedIndexService.getAll();
            System.out.println("Entries in database: " + savedEntries.size());
        } else {
            System.out.println("⚠️ No index entries to insert.");
        }
    }

    // Task for processing each batch of documents
    private static class DocumentProcessorTask implements Runnable {
        private final List<Documents> documents;
        private final Map<String, PostingData> globalIndex;

        public DocumentProcessorTask(List<Documents> documents, Map<String, PostingData> globalIndex) {
            this.documents = documents;
            this.globalIndex = globalIndex;
        }

        @Override
        public void run() {
            BuildInvertedIndex localIndex = new BuildInvertedIndex(documents, new Tokenizer()); // Build index for batch

            // Synchronize access to global index
            synchronized (globalIndex) {
                for (Map.Entry<String, PostingData> entry : localIndex.getInvertedIndex().entrySet()) {
                    globalIndex.putIfAbsent(entry.getKey(), new PostingData());
                    globalIndex.get(entry.getKey()).merge(entry.getValue());
                }
            }
        }
    }
}
