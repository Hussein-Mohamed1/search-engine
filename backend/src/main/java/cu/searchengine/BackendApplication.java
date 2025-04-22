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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Bean;

import cu.searchengine.controller.RankerController;
import cu.searchengine.model.RankedDocument;
import cu.searchengine.service.RankingService;

@SpringBootApplication(scanBasePackages = {"cu.searchengine"})
@EnableMongoRepositories(basePackages = "cu.searchengine.repository")
@EnableScheduling // Enable scheduling for periodic/background tasks
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

	@Autowired
	private cu.searchengine.Crawler.Crawler crawler; // Make sure Crawler is a @Component or @Service

	@Autowired
	private cu.searchengine.Indexer.ThreadPool threadPool; // Make sure ThreadPool is a @Component or @Service

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// No need to start crawler/indexer here, handled by @Scheduled methods
	}

	// Run the crawler continuously (every 10 seconds, adjust as needed)
	@Scheduled(fixedDelay = 10000)
	public void runCrawler() {
		try {
			crawler.crawl();
		} catch (Exception e) {
			System.err.println("Crawler error: " + e.getMessage());
		}
	}

	// Run the indexer periodically (every 5 minutes, adjust as needed)
	@Scheduled(fixedDelay = 3000)
	public void runIndexer() {
		try {
			threadPool.implementThreading();
		} catch (Exception e) {
			System.err.println("Indexer error: " + e.getMessage());
		}
	}
}