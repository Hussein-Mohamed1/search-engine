package Indexer;

import java.util.*;

import java.lang.Object;
//import org.apache.
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.tartarus.snowball.ext.PorterStemmer;
//import org.apache.lucene.analysis.ar.ArabicStemmer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;


public class Tokenizer {
    private static final Set StopWords = Set.of(
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

    public Tokenizer() {
    }


    public boolean isAbbreviation(String word) {
        return word.matches("[A-Za-z]+(\\.[A-Za-z]+)+"); // Matches abbreviations like "U.S.A.", "Ph.D."
    }

    public String StemWord(String word)
    {
        stemmer.setCurrent(word);
        stemmer.stem();
        return stemmer.getCurrent();

    }

    public  List<String> tokenize(String text) {
        text = text.toLowerCase();
        text = text.replaceAll("[^a-zA-Z0-9'\\-]", " ").trim();
        text = text.replaceAll("\\b\\d+\\b", " ");
        String []words=text.split(" ");
//        System.out.println(Arrays.toString(words));
        List<String>tokens=new ArrayList<>();
        for(String word:words){
            if (!word.isEmpty() && !StopWords.contains(word) && word.length() > 1||isAbbreviation(word)) {
                word = StemWord(word).trim();
                tokens.add(word);
            }
        }
        return tokens;
    }


    //only for testing
    //todo remove this
     public static void main(String[] args) {
         String text = "Dr. O'Connor's research on AI-powered bio-tech (e.g., CRISPR-cas9) was groundbreaking! "
                 + "He stated, \"It's an era of rapid innovation—don't you agree?\" "
                 + "Meanwhile, at www.research-lab.com, they found a 10x improvement in efficiency. "
                 + "E-mails like alice@example.com & phone numbers (+1-800-555-0199) aren't easy to handle. "
                 + "Mathematical expressions (x^2 + y^2 = r^2) & chemical formulas (H2O, C6H12O6) are tricky! "
                 + "The price was $19.99, and the date was 12/31/2024. Oh, and the 'data.txt' file had weird characters: 😀🔥💡!";
        Tokenizer tokenizer = new Tokenizer();
        List<String> tokens = tokenizer.tokenize(text);
        System.out.println(tokens);
    }
}
