package Indexer;

import model.Document;
import java.util.*;

public class BuildInvertedIndex {
    private final Map<String, PostingData> invertedIndex = new HashMap<>();
    private final Tokenizer tokenizer = new Tokenizer();

    public Map<String, PostingData> getInvertedIndex() {
        return invertedIndex;
    }

    public BuildInvertedIndex(List<Document> listOfDocuments) {
        for (Document doc : listOfDocuments) {
            int docId = doc.getId();
            Map<String, Posting> tokenizedWords = new HashMap<>();

            // Tokenize each section with its priority position
            processText(doc.getTitle(), docId, 4,tokenizedWords);      // Title (4)
            processText(doc.getMainHeading(), docId, 3,tokenizedWords); // Main Heading (3)
            processText(String.join(" ", doc.getSubHeading()), docId, 2,tokenizedWords); // Subheading (2)
            processText(doc.getContent(), docId, 1,tokenizedWords);    // Content (1)

            for (Map.Entry<String, Posting> entry : tokenizedWords.entrySet()) {
                String word = entry.getKey();
                Posting posting = entry.getValue();

                // Add word to the inverted index if it doesn't exist
                invertedIndex.putIfAbsent(word, new PostingData());

                // Add posting (TF & Priority Positions)
                invertedIndex.get(word).addPosting(docId, posting);
            }
        }



        // Update DF after processing all documents
        for (PostingData postingData : invertedIndex.values()) {
            postingData.updateDf();
        }
    }

    private void processText(String text, int docId, int priority, Map<String, Posting> tokenizedWords) {
        if (text == null || text.isEmpty()) return;

        // Tokenize and track priority-based positions
       tokenizer.tokenizeWithPriority(text, priority,tokenizedWords);
    }

    public Map<Integer, Posting> getPostings(String word) {
        return invertedIndex.getOrDefault(word, new PostingData()).getPostings();
    }
}
