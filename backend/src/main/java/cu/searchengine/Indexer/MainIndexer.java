package cu.searchengine.Indexer ;

import cu.searchengine.model.Documents;
import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.model.RankedDocument;
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
//        invertedIndexService.deleteAll();
        System.out.println("Starting indexing...");
        List<Documents> documents = documentService.getAllDocuments();
        System.out.println("Total number of documents: " + documents.size());
//        documents.addAll(generateDocuments(100)); todo:recomment it

        long startTime = System.nanoTime();
        cu.searchengine.Indexer.ThreadPool threadPool = new cu.searchengine.Indexer.ThreadPool(documentService, invertedIndexService);
        threadPool.implementThreading();

        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration / 1_000_000.0 + " ms");
    }

    // Remove the main method since this will no longer be a standalone application
    // It will be run by your main BackendApplication
}

//for testing
/*
        List<InvertedIndexEntry> testEntries = new ArrayList<>();

// Entry for word "cancel"
        List<RankedDocument> cancelPostings = new ArrayList<>();
        cancelPostings.add(new RankedDocument(101, "https://example.com/page1", "Cancel Culture Explained", 0, 0, 0, 5));
        cancelPostings.add(new RankedDocument(102, "https://example.com/page2", "Why We Cancel", 0, 0, 0, 3));
        InvertedIndexEntry cancelEntry = new InvertedIndexEntry("cancel", cancelPostings.size(), cancelPostings);

// Entry for word "rerais"
        List<RankedDocument> reraisPostings = new ArrayList<>();
        reraisPostings.add(new RankedDocument(103, "https://poker.com/article", "How to Rerais in Poker", 0, 0, 0, 4));
        reraisPostings.add(new RankedDocument(104, "https://strategyhub.net/rerais", "Rerais Strategy", 0, 0, 0, 2));
        InvertedIndexEntry reraisEntry = new InvertedIndexEntry("rerais", reraisPostings.size(), reraisPostings);
 */