package cu.searchengine;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import cu.searchengine.controller.RankerController;
import cu.searchengine.model.RankedDocument;
import cu.searchengine.service.RankingService;

@SpringBootApplication(scanBasePackages = {"cu.searchengine"})
@EnableMongoRepositories(basePackages = "cu.searchengine.repository")
public class BackendApplication implements CommandLineRunner {
	private final RankingService rankingService;

	@Autowired
	public BackendApplication(RankingService rankingService) {
		this.rankingService = rankingService;
	}

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
			System.out.println(doc.getDocTitle() + " - Score: " + doc.getPopularityScore());
			rankingService.saveRanking(doc.getUrl() , doc.getDocTitle() , doc.getRelevanceScore() , doc.getPopularityScore() , doc.getFinalScore());
		}
	}
}
