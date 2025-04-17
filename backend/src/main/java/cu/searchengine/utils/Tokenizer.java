package cu.searchengine.utils;

import cu.searchengine.Indexer.Posting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.*;


public class Tokenizer {
    private static Set<String> STOP_WORDS = null;
    private final PorterStemmer stemmer = new PorterStemmer();
    Logger logger = LoggerFactory.getLogger("Tokenizer");

    public Tokenizer() {


        try {
            ResourceReader resourceReader = new ResourceReader(new DefaultResourceLoader());
            String content = resourceReader.loadResourceAsString("classpath:static/STOP_WORDS.txt");
            STOP_WORDS = Set.of(content.split("\\n"));
            logger.info("Tokenizer loaded, Stop words count: {}", STOP_WORDS.size());

        } catch (Exception ex) {
            logger.error(ex.getMessage());
            System.err.println("Error reading from classpath: " + ex.getMessage());
        }
    }

    public HashSet<String> tokenize(String text) {
        HashSet<String> tokens = new HashSet<>();
        text = text.toLowerCase().replaceAll("[^a-zA-Z0-9'\\-]", " ").trim();
        text = text.replaceAll("\\b\\d+\\b", " "); // Remove numbers
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.isEmpty() || word.length() == 1 || STOP_WORDS.contains(word)) continue;

            // Stem the word
            stemmer.setCurrent(word);
            stemmer.stem();
            word = stemmer.getCurrent();
            tokens.add(word);
        }

        return tokens;
    }


    public void tokenizeWithPriority(String text, int priority, Map<String, Posting> tokenMap) {
        text = text.toLowerCase().replaceAll("[^a-zA-Z0-9'\\-]", " ").trim();
        text = text.replaceAll("\\b\\d+\\b", " "); // Remove numbers
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (word.isEmpty() || word.length() == 1 || STOP_WORDS.contains(word)) continue;

            // Stem the word
            stemmer.setCurrent(word);
            stemmer.stem();
            word = stemmer.getCurrent();

            // Update tokenMap without reinitializing it
            tokenMap.putIfAbsent(word, new Posting());
            tokenMap.get(word).addPosition(priority); // Adds position & increments TF
        }
    }

}
