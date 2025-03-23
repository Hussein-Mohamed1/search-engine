package cu.searchengine.Indexer;


import cu.searchengine.model.Document;
import java.util.*;

import static cu.searchengine.Indexer.DocumentGenerator.generateDocuments;

public class BuildInvertedIndexTest {
    public static void main(String[] args) {
        // Create sample documents
        List<Document> documents = new ArrayList<>();

        documents.addAll( generateDocuments(10000));

        // Build the inverted index
        //calc time spend in this operation

        long startTime = System.nanoTime();

        BuildInvertedIndex indexer = new BuildInvertedIndex(documents);

        long endTime = System.nanoTime();
        long duration = endTime - startTime; // Time in nanoseconds
        System.out.println("Time taken: " + duration / 1_000_000.0 + " ms");

        System.out.println("Inverted Index built successfully with size: " + indexer.getInvertedIndex().size() + "");



        // Print all the words in the inverted index
//        printAllInvertedIndex(indexer);
    }

    private static void printAllInvertedIndex(BuildInvertedIndex indexer) {
        System.out.println("\n----- Inverted Index -----");

        // Loop through all words in the inverted index
        for (Map.Entry<String, PostingData> entry : indexer.getInvertedIndex().entrySet()) {
            String word = entry.getKey();
            PostingData postingData = entry.getValue();

            System.out.println("\nWord: " + word);
            System.out.println("  DF (Document Frequency): " + postingData.getDf());

            for (Map.Entry<Integer, Posting> docEntry : postingData.getPostings().entrySet()) {
                int docId = docEntry.getKey();
                Posting posting = docEntry.getValue();

                System.out.println("  Document ID: " + docId);
                System.out.println("    TF (Term Frequency): " + posting.getTf());
                System.out.println("    Positions: " + posting.getPositions());
            }
        }
    }
}
