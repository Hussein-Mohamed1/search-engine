package cu.searchengine.Indexer;

import cu.searchengine.model.Documents;
import cu.searchengine.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class IndexerTest implements Runnable {
    private final cu.searchengine.service.InvertedIndexService invertedIndexService;
    private final DocumentService documentService;

    @Autowired
    public IndexerTest(cu.searchengine.service.InvertedIndexService invertedIndexService, DocumentService documentService) {
        this.invertedIndexService = invertedIndexService;
        this.documentService = documentService;
    }

    @Override
    public void run() {
//        invertedIndexService.deleteAll();
        System.out.println("Starting indexing...");
        List<Documents> documents = documentService.getAllDocuments();
        System.out.println("Total number of documents: " + documents.size());

        long startTime = System.nanoTime();
        InvertedIndex invertedIndex = new InvertedIndex(documentService, invertedIndexService);
        invertedIndex.implementThreading();

        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration / 1_000_000.0 + " ms");
    }

}
