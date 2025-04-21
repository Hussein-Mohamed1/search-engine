package cu.searchengine;

import java.util.List;

import cu.searchengine.Crawler.Crawler;
import cu.searchengine.model.SearchResult;
import cu.searchengine.service.DocumentService;
import cu.searchengine.service.InvertedIndexService;
import cu.searchengine.service.SearchService;
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
	private final SearchService searchService;
	private final DocumentService documentService;
	private final InvertedIndexService  invertedIndexService;

	@Autowired
	public BackendApplication(RankingService rankingService, SearchService searchService, DocumentService documentService, InvertedIndexService invertedIndexService) {
		this.rankingService = rankingService;
		this.searchService = searchService;
		this.documentService = documentService;
        this.invertedIndexService = invertedIndexService;
    }

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Initialize RankerController with a total document count of 100
		RankerController ranker = new RankerController(100 , documentService , invertedIndexService);

		// Define query words
		String[] queryWords = {"java", "search"};
		String[] queryWords2 = {"java", "ranking"};

//		 Get ranked results
		List<RankedDocument> rankedResults = ranker.rankDocuments(queryWords);
		List<RankedDocument> rankedResults2 = ranker.rankDocuments(queryWords2);
		SearchResult result1 = SearchResult.builder()
				.query("java Search")
				.results(rankedResults)
				.timestamp(System.currentTimeMillis())
				.build();
		searchService.saveSearchResult(result1);
		SearchResult result2 = SearchResult.builder()
				.query("java ranking")
				.results(rankedResults2)
				.timestamp(System.currentTimeMillis())
				.build();
		searchService.saveSearchResult(result2);
//		 Print the ranked documents
		List<SearchResult> res = searchService.getResultsByQuery("java Search");
		System.out.println("size : " + res.size());
		for (SearchResult result : res) {
			System.out.println("Query: " + result.getQuery());
			System.out.println("Timestamp: " + new java.util.Date(result.getTimestamp()));
			System.out.println("Results:");

			for (RankedDocument doc : result.getResults()) {
				System.out.println("   └ Title: " + doc.getDocTitle());
				System.out.println("     URL: " + doc.getUrl());
				System.out.println("     Score: " + doc.getFinalScore());
				System.out.println();
			}

			System.out.println("------------------------------------------------------------");
		}

		// todo increase number of threads when hosted on the server max(400)
//		int numberOfThreads = 50;
//		Crawler crawler = new Crawler("nemo", 50, numberOfThreads, 1000, documentService);
//		crawler.crawl();

	}
}