package cu.searchengine.Indexer;

//import cu.searchengine.model.WebDocument;
import cu.searchengine.utils.Tokenizer;

import cu.searchengine.model.Documents;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuildInvertedIndex {
    private final Map<String, PostingData> invertedIndex = new HashMap<>();
    private final ConcurrentHashMap<String,Integer> wordfreq=new ConcurrentHashMap<>();
    Tokenizer tokenizer;


    public Map<String, PostingData> getInvertedIndex() {
        return invertedIndex;
    }

    public BuildInvertedIndex(List<Documents> listOfDocuments, Tokenizer tokenizer,ConcurrentHashMap<String,Integer> wordfreq) {
        this.tokenizer=tokenizer;
        for (Documents doc : listOfDocuments) {
            int docId = doc.getId();
            String title = doc.getTitle();
            String url = doc.getUrl();
            double popularity=doc.getPopularityScore();
//            System.out.println("Popularity: "+popularity);
            Map<String, Posting> tokenizedWords = new HashMap<>();

            // Tokenize each section with its priority position
            processText(doc.getTitle(), docId, 4,tokenizedWords,tokenizer,title,url,popularity,wordfreq);      // Title (4)
            processText(String.join(" ",doc.getMainHeading()), docId, 3,tokenizedWords,tokenizer,title,url,popularity,wordfreq); // Main Heading (3)
            processText(String.join(" ", doc.getSubHeadings()), docId, 2,tokenizedWords,tokenizer,title,url,popularity,wordfreq); // Subheading (2)
            processText(doc.getContent(), docId, 1,tokenizedWords,tokenizer,title,url,popularity,wordfreq);    // Content (1)

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

    private void processText(String text, int docId, int priority, Map<String, Posting> tokenizedWords,Tokenizer tokenizer,String title,String url,double popularity,ConcurrentHashMap<String,Integer> wordfreq) {
        if (text == null || text.isEmpty()) return;

        // Tokenize and track priority-based positions
       tokenizer.tokenizeWithPriority(text, priority,tokenizedWords,title,url,popularity,wordfreq);
    }

    public Map<Integer, Posting> getPostings(String word) {
        return invertedIndex.getOrDefault(word, new PostingData()).getPostings();
    }
}
