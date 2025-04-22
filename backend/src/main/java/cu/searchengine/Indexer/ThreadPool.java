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
    private static final ConcurrentHashMap<String, Integer> wordfreq = new ConcurrentHashMap<>();
    private final InvertedIndexService invertedIndexService;
    private Boolean firstRun;

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

        List<InvertedIndexEntry> indexEntries = convertGlobalIndexToList();
        upsertInvertedIndexEntries(indexEntries); // Unified upsert method
    }

    private List<InvertedIndexEntry> convertGlobalIndexToList() {
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
                        posting.getTf()));
            }

            indexEntries.add(new InvertedIndexEntry(word, data.getDf(), postingEntries));
        }
        return indexEntries;
    }

    /**
     * Upserts a list of InvertedIndexEntry: merges with existing if present,
     * inserts if not.
     */
    public void upsertInvertedIndexEntries(List<InvertedIndexEntry> indexEntries) {
        if (indexEntries == null || indexEntries.isEmpty()) {
            System.out.println("⚠️ No index entries to upsert.");
            return;
        }

        System.out.println("🔄 Upserting " + indexEntries.size() + " entries in invertedIndex...");

        List<InvertedIndexEntry> toInsert = new ArrayList<>();
        List<InvertedIndexEntry> toUpdate = new ArrayList<>();
        int batchSize = 500;
        List<String> batchWords = new ArrayList<>(batchSize);

        for (int i = 0; i < indexEntries.size(); i++) {
            InvertedIndexEntry newEntry = indexEntries.get(i);
            batchWords.add(newEntry.getWord());

            // When batch is full or at the end, process the batch
            boolean isLast = (i == indexEntries.size() - 1);
            if (batchWords.size() == batchSize || isLast) {
                // Fetch all existing entries for this batch from DB
                List<InvertedIndexEntry> existingBatch = invertedIndexService.getByWords(batchWords);
                Map<String, InvertedIndexEntry> existingMap = new java.util.HashMap<>();
                for (InvertedIndexEntry entry : existingBatch) {
                    existingMap.put(entry.getWord(), entry);
                }

                // Process each entry in the batch
                for (int j = i - batchWords.size() + 1; j <= i; j++) {
                    InvertedIndexEntry entry = indexEntries.get(j);
                    InvertedIndexEntry existing = existingMap.get(entry.getWord());
                    if (existing == null) {
                        toInsert.add(entry);
                    } else {
                        // Merge new postings into existing
                        List<RankedDocument> existingPostings = existing.getPostings();
                        List<RankedDocument> newPostings = entry.getPostings();

                        var existingDocIds = existingPostings.stream()
                                .map(RankedDocument::getDocId)
                                .collect(java.util.stream.Collectors.toSet());

                        int added = 0;
                        for (RankedDocument newPost : newPostings) {
                            if (!existingDocIds.contains(newPost.getDocId())) {
                                existingPostings.add(newPost);
                                added++;
                            }
                        }

                        if (added > 0) {
                            existing.setDf(existing.getDf() + added);
                            existing.setPostings(existingPostings);
                            toUpdate.add(existing);
                        }
                    }
                }

                // Batch insert/update if batchSize reached or at the end
                if (!toInsert.isEmpty()) {
                    invertedIndexService.insertAll(new ArrayList<>(toInsert));
                    toInsert.clear();
                }
                if (!toUpdate.isEmpty()) {
                    invertedIndexService.saveAll(new ArrayList<>(toUpdate));
                    toUpdate.clear();
                }
                batchWords.clear();
            }
        }

        System.out.println("✅ Finished upserting inverted index.");
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
            BuildInvertedIndex localIndex = new BuildInvertedIndex(documents, new Tokenizer(), wordfreq); // Build index
                                                                                                          // for batch

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
