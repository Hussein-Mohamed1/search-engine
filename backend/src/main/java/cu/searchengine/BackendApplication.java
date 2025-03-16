package cu.searchengine.backend;

import com.searchengine.controller.RankerController;
import com.searchengine.model.RankedDocument;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class BackendApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

	@Override
	public void run(String... args) throws Exception {
		// Initialize RankerController with a total document count of 100
		RankerController ranker = new RankerController(100);

		// Define query words
		String[] queryWords = {"java", "search"};

		// Get ranked results
		List<RankedDocument> rankedResults = ranker.rankDocuments(queryWords);

		// Print the ranked documents
		System.out.println("Ranked Documents:");
		for (RankedDocument doc : rankedResults) {
			System.out.println(doc.getDocTitle() + " - Score: " + doc.getRelevanceScore());
		}
	}
}
