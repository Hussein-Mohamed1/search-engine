package cu.searchengine.utils;

import org.tartarus.snowball.ext.PorterStemmer;
import cu.searchengine.Indexer.PostingData;
import cu.searchengine.Indexer.Posting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Tokenizer {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any",
            "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below",
            "between", "both", "but", "by", "can", "can't", "come", "could", "couldn't", "did",
            "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each",
            "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't",
            "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
            "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if",
            "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more",
            "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once",
            "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
            "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so",
            "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
            "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've",
            "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't",
            "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when",
            "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why",
            "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're",
            "you've", "your", "yours", "yourself", "yourselves"
    );

    private final PorterStemmer stemmer = new PorterStemmer();

    public void tokenizeWithPriority(String text, int priority, Map<String, Posting> tokenMap, String title, String url, ConcurrentHashMap<String,Integer>wordfreq) {
        text = text.toLowerCase().replaceAll("[^a-zA-Z0-9'\\-]", " ").trim();
        text = text.replaceAll("\\b\\d+\\b", " "); // Remove numbers
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.isEmpty() || STOP_WORDS.contains(word) || word.length() <= 1) continue;

            // Stem the word
            stemmer.setCurrent(word);
            stemmer.stem();
            word = stemmer.getCurrent();

            wordfreq.putIfAbsent(word,1);

            // Update tokenMap without reinitializing it
            tokenMap.putIfAbsent(word, new Posting());
            tokenMap.get(word).addPosition(priority); // Adds position & increments TF
            tokenMap.get(word).setTitle(title);
            tokenMap.get(word).setUrl(url);
        }
    }

}
