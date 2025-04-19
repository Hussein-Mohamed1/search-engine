package cu.searchengine.Indexer ;

import cu.searchengine.model.Documents;
import cu.searchengine.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import cu.searchengine.model.WebDocument;



import java.util.ArrayList;
import java.util.List;

import static cu.searchengine.Indexer.DocumentGenerator.generateDocuments;


@Component
public class MainIndexer implements CommandLineRunner {
    private final cu.searchengine.service.InvertedIndexService invertedIndexService;
    private final DocumentService documentService;

    @Autowired
    public MainIndexer(cu.searchengine.service.InvertedIndexService invertedIndexService, DocumentService documentService) {
        this.invertedIndexService = invertedIndexService;
        this.documentService = documentService;
    }

    @Override
    public void run(String... args) throws Exception {
        // This will run automatically when the Spring application starts
        List<Documents> documents = documentService.getAllDocuments();
//        documents.addAll(generateDocuments(100)); todo:recomment it

        System.out.println("Clearing existing invertedIndex collection...");
        invertedIndexService.deleteAll();
        System.out.println("Collection cleared. Starting indexing...");

        long startTime = System.nanoTime();
        cu.searchengine.Indexer.ThreadPool threadPool = new cu.searchengine.Indexer.ThreadPool(documents, invertedIndexService);
        threadPool.implementThreading();
        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration / 1_000_000.0 + " ms");
    }

    // Remove the main method since this will no longer be a standalone application
    // It will be run by your main BackendApplication
}