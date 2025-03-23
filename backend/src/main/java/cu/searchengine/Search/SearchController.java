package cu.searchengine.Search;

//import cu.searchengine.utils.Lemmatizer;
import cu.searchengine.utils.Tokenizer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RestController
@Component
public class SearchController {
    private final Tokenizer tokenizer = new Tokenizer();
//    private final Lemmatizer lemmatizer = new Lemmatizer();
    private final SearchService searchService = new SearchService();
    private final Logger logger = LoggerFactory.getLogger("SearchController");

    @RequestMapping("/api/search")
    public String search(@RequestParam String q) {

        return tokenizer.tokenize(q).toString();

//        return lemmatizer.lemmatize(q).toString();
    }
}
