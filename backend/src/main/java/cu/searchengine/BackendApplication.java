package cu.searchengine;

import cu.searchengine.Indexer.MainIndexer;  // Note the capital M in MainIndexer
import cu.searchengine.service.InvertedIndexService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "cu.searchengine.repository")
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner indexerRunner(InvertedIndexService invertedIndexService) {
		return new MainIndexer(invertedIndexService);  // Updated to MainIndexer
	}
}