//package cu.searchengine.utils;
//
//import edu.stanford.nlp.ling.*;
//import edu.stanford.nlp.pipeline.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.io.DefaultResourceLoader;
//
//import java.util.*;
//
//public class Lemmatizer {
//    Logger logger = LoggerFactory.getLogger("Lemmatizer");
//    private static Set<String> STOP_WORDS = null;
//
//    public Lemmatizer() {
//        try {
//            ResourceReader resourceReader = new ResourceReader(new DefaultResourceLoader());
//            String content = resourceReader.loadResourceAsString("classpath:static/STOP_WORDS.txt");
//            STOP_WORDS = Set.of(content.split("\\n"));
//            logger.info("Lemmatizer loaded, Stop words count: {}", STOP_WORDS.size());
//
//        } catch (Exception ex) {
//            logger.error(ex.getMessage());
//            System.err.println("Error reading from classpath: " + ex.getMessage());
//        }
//    }
//
//    public HashSet<String> lemmatize(String word) {
//        // set up pipeline properties
//        Properties props = new Properties();
//        // set the list of annotators to run
//        props.setProperty("annotators", "tokenize,pos,lemma");
//        // build pipeline
//        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//        // create a document object
//        CoreDocument document = pipeline.processToCoreDocument(word);
//
//        HashSet<String> lemmas = new HashSet<>();
//
//        // display tokens
//        for (CoreLabel tok : document.tokens()) {
//            if (tok.word().isEmpty() || tok.word().length() == 1 || STOP_WORDS.contains(tok.word())) continue;
//
//            lemmas.add(tok.lemma());
//            System.out.printf("%s\t%s%n", tok.word(), tok.lemma());
//        }
//        return lemmas;
//    }
//}
//
//
