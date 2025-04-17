package cu.searchengine.Indexer;

import cu.searchengine.model.WebDocument;

import java.util.*;

public class DocumentGenerator {
    private static final String[] TITLES = {
            "Introduction to AI", "Search Engine Optimization", "Deep Learning Basics",
            "Machine Learning Overview", "Big Data Analysis", "Cloud Computing Trends",
            "Blockchain Technology", "Cybersecurity Fundamentals", "Software Engineering Principles",
            "Operating Systems Concepts"
    };

    private static final String[] HEADINGS = {
            "Getting Started", "Advanced Concepts", "Best Practices",
            "Case Studies", "Future Trends", "Industry Applications"
    };

    private static final String[] SUBHEADINGS = {
            "Overview", "Technical Details", "Real-World Examples", "Implementation Guide",
            "Challenges and Solutions", "Performance Optimization"
    };

    private static final String[] CONTENT_WORDS = {
            "data", "analysis", "performance", "scalability", "machine", "learning",
            "security", "algorithm", "framework", "optimization", "storage", "retrieval",
            "network", "speed", "accuracy", "deep", "neural", "processing", "search", "query"
    };

    public static List<WebDocument> generateDocuments(int count) {
        List<WebDocument> documents = new ArrayList<>();
        Random random = new Random();

        for (int i = 1; i <= count; i++) {
            String title = TITLES[random.nextInt(TITLES.length)];
            String mainHeading = HEADINGS[random.nextInt(HEADINGS.length)];
            List<String> subHeadings = Arrays.asList(
                    SUBHEADINGS[random.nextInt(SUBHEADINGS.length)],
                    SUBHEADINGS[random.nextInt(SUBHEADINGS.length)]
            );

            String content = generateRandomContent(random, 10000); // Ensure 500 words per document

            documents.add(new WebDocument(i, "https://example.com/doc" + i, title, mainHeading, subHeadings, content,
                    Collections.singletonList("https://example.com")));
        }

        return documents;
    }

    private static String generateRandomContent(Random random, int wordCount) {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            content.append(CONTENT_WORDS[random.nextInt(CONTENT_WORDS.length)]).append(" ");
        }
        return content.toString().trim();
    }

    public static void main(String[] args) {
        List<WebDocument> documents = generateDocuments(200);
        System.out.println("Generated " + documents.size() + " documents.");

        // Print first 5 documents for verification
        for (int i = 0; i < 5; i++) {
            System.out.println("Doc " + (i + 1) + " Title: " + documents.get(i).getTitle());
            System.out.println("Content (first 100 chars): " + documents.get(i).getContent().substring(0, 100) + "...");
            System.out.println("------------");
        }
    }
}
