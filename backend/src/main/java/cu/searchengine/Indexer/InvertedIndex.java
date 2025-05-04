package cu.searchengine.Indexer;

import cu.searchengine.model.Documents;
import cu.searchengine.model.IndexDocument;
import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.service.DocumentService;
import cu.searchengine.service.InvertedIndexService;
import cu.searchengine.utils.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class InvertedIndex {
    private static final Logger logger = LoggerFactory.getLogger(InvertedIndex.class);
    private final DocumentService documentService;
    private final InvertedIndexService invertedIndexService;
    private static final ConcurrentHashMap<String, PostingData> globalIndex = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> wordfreq = new ConcurrentHashMap<>();

    // Only keep the constructor that takes DocumentService and InvertedIndexService
    public InvertedIndex(DocumentService documentService, InvertedIndexService invertedIndexService) {
        this.documentService = documentService;
        this.invertedIndexService = invertedIndexService;
    }

    // Always fetch fresh documents from DB
    public void implementThreading() {
        List<Documents> currentDocs = documentService.getDocumentsToIndex();
        if (currentDocs == null || currentDocs.isEmpty()) {
            logger.warn("No new documents to index. Skipping indexing run.");
            return;
        }

        // Mark as indexed before processing to avoid race conditions
        documentService.markDocumentsAsIndexed(currentDocs);

        int numThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int batchSize = (int) Math.ceil((double) currentDocs.size() / numThreads);

        logger.info("Starting indexing with {} threads, batch size: {}", numThreads, batchSize);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < currentDocs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, currentDocs.size());
            List<Documents> batch = currentDocs.subList(i, end);
            logger.debug("Submitting batch [{}-{}) to thread pool", i, end);
            executor.execute(new DocumentProcessorTask(batch, globalIndex, i, end));
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                logger.error("Thread pool did not terminate within timeout. Forcing shutdown...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread pool interrupted during awaitTermination", e);
            executor.shutdownNow();
        }
        logger.info("All threads have finished execution!");
        logger.info("Indexing Complete! Final Index Size: {}", globalIndex.size());
        logger.info("Indexing took {} ms", System.currentTimeMillis() - startTime);

        List<InvertedIndexEntry> indexEntries = convertGlobalIndexToList();
        upsertInvertedIndexEntries(indexEntries);
    }

    private List<InvertedIndexEntry> convertGlobalIndexToList() {
        List<InvertedIndexEntry> indexEntries = new ArrayList<>();
        for (Map.Entry<String, PostingData> entry : globalIndex.entrySet()) {
            String word = entry.getKey();
            PostingData data = entry.getValue();

            List<IndexDocument> postingEntries = new ArrayList<>();
            for (Map.Entry<Integer, Posting> p : data.getPostings().entrySet()) {
                Posting posting = p.getValue();
                postingEntries.add(new IndexDocument(
                        p.getKey(),
                        posting.getUrl(),
                        posting.getTitle(),
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
            logger.warn("⚠️ No index entries to upsert.");
            return;
        }

        logger.info("🔄 Upserting {} entries in invertedIndex...", indexEntries.size());

        List<InvertedIndexEntry> toInsert = new ArrayList<>();
        List<InvertedIndexEntry> toUpdate = new ArrayList<>();
        int batchSize = 500;
        List<String> batchWords = new ArrayList<>(batchSize);

        long upsertStart = System.currentTimeMillis();

        for (int i = 0; i < indexEntries.size(); i++) {
            InvertedIndexEntry newEntry = indexEntries.get(i);
            batchWords.add(newEntry.getWord());

            boolean isLast = (i == indexEntries.size() - 1);
            if (batchWords.size() == batchSize || isLast) {
                logger.debug("Processing upsert batch [{}-{})", i - batchWords.size() + 1, i + 1);

                List<InvertedIndexEntry> existingBatch = invertedIndexService.getByWords(batchWords);
                Map<String, InvertedIndexEntry> existingMap = new java.util.HashMap<>();
                for (InvertedIndexEntry entry : existingBatch) {
                    existingMap.put(entry.getWord(), entry);
                }

                for (int j = i - batchWords.size() + 1; j <= i; j++) {
                    InvertedIndexEntry entry = indexEntries.get(j);
                    InvertedIndexEntry existing = existingMap.get(entry.getWord());
                    if (existing == null) {
                        toInsert.add(entry);
                    } else {
                        List<IndexDocument> existingPostings = existing.getPostings();
                        List<IndexDocument> newPostings = entry.getPostings();

                        var existingDocIds = existingPostings.stream()
                                .map(IndexDocument::getDocId)
                                .collect(java.util.stream.Collectors.toSet());

                        int added = 0;
                        for (IndexDocument newPost : newPostings) {
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

                if (!toInsert.isEmpty()) {
                    logger.debug("Inserting {} new index entries...", toInsert.size());
                    invertedIndexService.insertAll(new ArrayList<>(toInsert));
                    toInsert.clear();
                }
                if (!toUpdate.isEmpty()) {
                    logger.debug("Updating {} existing index entries...", toUpdate.size());
                    invertedIndexService.saveAll(new ArrayList<>(toUpdate));
                    toUpdate.clear();
                }
                batchWords.clear();
            }
        }

        logger.info("✅ Finished upserting inverted index. Took {} ms", System.currentTimeMillis() - upsertStart);
    }

    // Task for processing each batch of documents
    private static class DocumentProcessorTask implements Runnable {
        private final List<Documents> documents;
        private final Map<String, PostingData> globalIndex;
        private final int batchStart;
        private final int batchEnd;

        public DocumentProcessorTask(List<Documents> documents, Map<String, PostingData> globalIndex, int batchStart,

                                     int batchEnd) {
            this.documents = documents;
            this.globalIndex = globalIndex;
            this.batchStart = batchStart;
            this.batchEnd = batchEnd;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            long start = System.currentTimeMillis();
            Logger logger = LoggerFactory.getLogger(DocumentProcessorTask.class);

            logger.debug("[{}] Processing documents batch [{}-{})", threadName, batchStart, batchEnd);

            BuildInvertedIndex localIndex = new BuildInvertedIndex(documents, new Tokenizer(), wordfreq);

            synchronized (globalIndex) {
                for (Map.Entry<String, PostingData> entry : localIndex.getInvertedIndex().entrySet()) {
                    globalIndex.putIfAbsent(entry.getKey(), new PostingData());
                    globalIndex.get(entry.getKey()).merge(entry.getValue());
                }
            }


            logger.debug("[{}] Finished batch [{}-{}) in {} ms", threadName, batchStart, batchEnd,
                    System.currentTimeMillis() - start);
        }
    }
}
