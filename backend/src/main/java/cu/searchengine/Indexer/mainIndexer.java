package Indexer;

import Tests.Indexer.DocumentGenerator;
import model.Document;

import java.util.ArrayList;
import java.util.List;
import static Tests.Indexer.DocumentGenerator.generateDocuments;
import Indexer.ThreadPool;


public class mainIndexer {
    public static void main(String[] args) {
        List<Document> documents=new ArrayList<>(); // Load documents from storage
        documents.addAll(generateDocuments(10000));
        long startTime = System.nanoTime();
        ThreadPool threadPool=new ThreadPool(documents);
        threadPool.implementThreading();
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration / 1_000_000.0 + " ms");
    }
}
