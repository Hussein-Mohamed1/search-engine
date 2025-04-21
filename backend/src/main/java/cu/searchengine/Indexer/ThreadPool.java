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
    private static final ConcurrentHashMap<String,Integer> wordfreq=new ConcurrentHashMap<>();
    private final InvertedIndexService invertedIndexService;
    private Boolean firstRun;

    // Constructor now takes InvertedIndexService instead of MongoTemplate
    public ThreadPool(List<Documents> docs, InvertedIndexService invertedIndexService) {
        this.docs = docs;
        this.invertedIndexService = invertedIndexService;
        this.firstRun = true;
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
        if (firstRun) {
            bulkInsertInvertedIndex(indexEntries);
        } else {
            updateDocumentsWithInvertedIndex(indexEntries);
        }
        firstRun = false;
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
                        posting.getTf()
                ));
            }

            indexEntries.add(new InvertedIndexEntry(word, data.getDf(), postingEntries));
        }
        return indexEntries;
    }


    public void updateDocumentsWithInvertedIndex(List<InvertedIndexEntry> indexEntries) {
        if (indexEntries == null || indexEntries.isEmpty()) {
            System.out.println("⚠️ No index entries to update.");
            return;
        }

        System.out.println("🔄 Updating " + indexEntries.size() + " entries in invertedIndex...");

        for (InvertedIndexEntry newEntry : indexEntries) {
            try {
                String word = newEntry.getWord();
                Integer freq = ThreadPool.wordfreq.getOrDefault(word, 0); // default to 0 if not found
                if (freq == 0) { //todo:comment this when testing updating only
                    // Word has never appeared before → insert it
                    invertedIndexService.insertAll(List.of(newEntry));
                    continue;
                }

                // Word exists in global frequency → check DB
                InvertedIndexEntry existing = invertedIndexService.getByWord(word);

                if (existing == null) {
                    // Fallback: doesn't exist in DB yet → insert
                    invertedIndexService.insertAll(List.of(newEntry));
                    continue;
                }

                // Merge new postings into existing
                List<RankedDocument> existingPostings = existing.getPostings();
                List<RankedDocument> newPostings = newEntry.getPostings();

                // Prevent duplicate docIds
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
                    invertedIndexService.save(existing); // Add this method or use repository.save
                }

            } catch (Exception e) {
                System.err.println("❌ Failed to update word: " + newEntry.getWord());
                e.printStackTrace();
            }
        }

        System.out.println("✅ Finished updating inverted index.");
    }



    // Refactored to use InvertedIndexService for bulk insert
    public void bulkInsertInvertedIndex(List<InvertedIndexEntry> indexEntries) {
        if (!indexEntries.isEmpty()) {
            System.out.println("Index entries to insert: " + indexEntries.size());
            int batchSize=500;
            for(int i=0;i<indexEntries.size();i+=batchSize)
            {
                long start=System.currentTimeMillis();
                int end=Math.min(i+batchSize,indexEntries.size());
                List<InvertedIndexEntry> batch=indexEntries.subList(i,end);
                invertedIndexService.insertAll(batch);
                System.out.println("Batch " + i + " inserted in " + (System.currentTimeMillis() - start) + "ms");
            }
            System.out.println("✅ Inverted index inserted. Total terms: " + indexEntries.size());
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
            BuildInvertedIndex localIndex = new BuildInvertedIndex(documents, new Tokenizer(),wordfreq); // Build index for batch

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
