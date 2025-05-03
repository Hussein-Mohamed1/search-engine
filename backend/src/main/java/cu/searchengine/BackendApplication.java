package cu.searchengine;

import cu.searchengine.Crawler.Crawler;
import cu.searchengine.Indexer.InvertedIndex;
import cu.searchengine.service.DocumentService;
import cu.searchengine.service.InvertedIndexService;
import cu.searchengine.service.RankingService;
import cu.searchengine.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication(scanBasePackages = {"cu.searchengine"})
@EnableMongoRepositories(basePackages = "cu.searchengine.repository")
@EnableScheduling // Enable scheduling for periodic/background tasks
public class BackendApplication implements CommandLineRunner {
    private final RankingService rankingService;
    private final SearchService searchService;
    private final DocumentService documentService;
    private final InvertedIndexService invertedIndexService;
    private static final Logger logger = LoggerFactory.getLogger(BackendApplication.class);
    private final Crawler crawler; // Make sure Crawler is a @Component or @Service
    private final InvertedIndex invertedIndex; // Make sure ThreadPool is a @Component or @Service

    @Autowired
    public BackendApplication(RankingService rankingService, SearchService searchService, DocumentService documentService, InvertedIndexService invertedIndexService, Crawler crawler, InvertedIndex invertedIndex) {
        this.rankingService = rankingService;
        this.searchService = searchService;
        this.documentService = documentService;
        this.invertedIndexService = invertedIndexService;
        this.crawler = crawler;
        this.invertedIndex = invertedIndex;
    }


    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        runCrawler();
    }

    //     Run the crawler continuously (every 10 seconds, adjust as needed)
//    @Scheduled(fixedDelay = 10000) // Commented out for now, we'll keep it running forever
    public void runCrawler() {
        try {
            crawler.crawl();
        } catch (Exception e) {
            logger.error("Crawler error: {}", e.getMessage());
        }
    }

    // Run the indexer periodically (every 5 minutes, adjust as needed)
//    @Scheduled(fixedDelay = 60000)
    public void runIndexer() {
        try {
            invertedIndex.implementThreading();
        } catch (Exception e) {
            logger.error("Indexer error: {}", e.getMessage());
        }
    }
}