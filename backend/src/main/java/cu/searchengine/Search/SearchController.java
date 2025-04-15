package cu.searchengine.Search;

//import cu.searchengine.utils.Lemmatizer;

import cu.searchengine.utils.Tokenizer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final SearchService1 searchService1 = new SearchService1();
    private final Logger logger = LoggerFactory.getLogger("SearchController");

//    public ResponseEntity<List<Documents>> search() {
//        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
//    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, @RequestParam(required = false, defaultValue = "0") int page, @RequestParam(required = false, defaultValue = "10") int size) {

        return tokenizer.tokenize(q).toString();

//        return lemmatizer.lemmatize(q).toString();
    }
}
