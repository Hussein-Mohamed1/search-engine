package Tests.Indexer;
import Indexer.BuildInvertedIndex;
import Indexer.Posting;
import Indexer.PostingData;
import Tests.Indexer.DocumentGenerator;


import model.Document;
import java.util.*;

public class BuildInvertedIndexTest {
    public static void main(String[] args) {
        // Create sample documents
        List<Document> documents = new ArrayList<>();
        documents.add(new Document(1, "/doc1", "Search Engine Optimization", "SEO Basics",
                Arrays.asList("Performance", "Scalability"),
                "search engine rank performance data analysis optimization algorithms indexing seo techniques.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(2, "/doc2", "Machine Learning Overview", "Deep Learning",
                Arrays.asList("Optimization", "Accuracy"),
                "machine learning deep neural network accuracy training models classification supervised unsupervised learning.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(3, "/doc3", "Cybersecurity Fundamentals", "Security Best Practices",
                Arrays.asList("Encryption", "Threats"),
                "security algorithm encryption performance attack detection malware firewall authentication cyber defense.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(4, "/doc4", "Cloud Computing Trends", "Cloud Architecture",
                Arrays.asList("Storage", "Retrieval"),
                "cloud storage data processing network speed performance virtual machines containerization cloud security.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(5, "/doc5", "Deep Learning Basics", "Neural Networks Guide",
                Arrays.asList("Neural", "Processing"),
                "deep neural processing machine learning scalability convolutional networks recurrent networks ai applications.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(6, "/doc6", "Big Data Analysis", "Data Processing",
                Arrays.asList("Volume", "Velocity"),
                "big data analytics storage systems retrieval hadoop spark distributed computing massive datasets.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(7, "/doc7", "Blockchain Technology", "Distributed Ledger",
                Arrays.asList("Consensus", "Smart Contracts"),
                "blockchain transactions validation network decentralization immutability consensus protocols mining nodes.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(8, "/doc8", "Software Engineering", "Development Lifecycle",
                Arrays.asList("Agile", "DevOps"),
                "software development practices continuous integration deployment unit testing coding standards scalability.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(9, "/doc9", "Operating Systems Concepts", "Kernel Design",
                Arrays.asList("Memory Management", "Scheduling"),
                "os kernel handles processes resource allocation scheduling multithreading interprocess communication synchronization.",
                Collections.singletonList("https://example.com")));

        documents.add(new Document(10, "/doc10", "Artificial Intelligence", "AI Ethics",
                Arrays.asList("Bias", "Fairness"),
                "artificial intelligence decision making transparency accountability ethical concerns machine learning models.",
                Collections.singletonList("https://example.com")));


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
