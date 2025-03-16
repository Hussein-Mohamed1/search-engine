package Tests.Indexer;
import Indexer.BuildInvertedIndex;
import Indexer.Posting;
import Indexer.PostingData;


import model.Document;
import java.util.*;

public class BuildInvertedIndexTest {
    public static void main(String[] args) {
        // Create sample documents
        List<Document> documents = new ArrayList<>();

        documents.add(new Document(
                1,
                "https://example.com/doc1",
                "Search Engine Optimization model",  // Title
                "SEO Basics",                 // Main Heading
                Arrays.asList("Introduction", "Best Practices"), // Subheadings
                "Search engines analyze web content to rank pages.", // Content
                Arrays.asList("https://seo.com")
        ));

        documents.add(new Document(
                2,
                "https://example.com/doc2",
                "Machine Learning and AI",
                "Deep Learning Fundamentals model",
                Arrays.asList("Neural Networks", "Training model model"),
                "AI models models improve search engines over time.",
                Arrays.asList("https://ai.com")
        ));

        // Build the inverted index
        BuildInvertedIndex indexer = new BuildInvertedIndex(documents);

        // Print all the words in the inverted index
        printAllInvertedIndex(indexer);
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
