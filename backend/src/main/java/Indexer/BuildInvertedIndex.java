package Indexer;

import model.Document;
import java.util.*;

public class BuildInvertedIndex {
    private final Map<String, PostingData> invertedIndex = new HashMap<>();
    Tokenizer tokenizer;


    public Map<String, PostingData> getInvertedIndex() {
        return invertedIndex;
    }

    public BuildInvertedIndex(List<Document> listOfDocuments,Tokenizer tokenizer) {
        this.tokenizer=tokenizer;
        for (Document doc : listOfDocuments) {
            int docId = doc.getId();
            Map<String, Posting> tokenizedWords = new HashMap<>();

            // Tokenize each section with its priority position
            processText(doc.getTitle(), docId, 4,tokenizedWords,tokenizer);      // Title (4)
            processText(doc.getMainHeading(), docId, 3,tokenizedWords,tokenizer); // Main Heading (3)
            processText(String.join(" ", doc.getSubHeading()), docId, 2,tokenizedWords,tokenizer); // Subheading (2)
            processText(doc.getContent(), docId, 1,tokenizedWords,tokenizer);    // Content (1)

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

    private void processText(String text, int docId, int priority, Map<String, Posting> tokenizedWords,Tokenizer tokenizer) {
        if (text == null || text.isEmpty()) return;

        // Tokenize and track priority-based positions
       tokenizer.tokenizeWithPriority(text, priority,tokenizedWords);
    }

    public Map<Integer, Posting> getPostings(String word) {
        return invertedIndex.getOrDefault(word, new PostingData()).getPostings();
    }
}
