package cu.searchengine.service;

import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.repository.InvertedIndexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class InvertedIndexService {
    private final InvertedIndexRepository repository;
    @Autowired
    public InvertedIndexService(InvertedIndexRepository repository) {
        this.repository = repository;
    }

    public void saveAll(List<InvertedIndexEntry> entries) {
        try {
            System.out.println("Entries in the repo: " + repository.count());
            System.out.println("Saving " + entries.size() + " entries to invertedIndex collection...");
            repository.saveAll(entries);
            System.out.println("Successfully saved " + entries.size() + " entries to MongoDB");
            List<InvertedIndexEntry> savedEntries = repository.findAll();
            System.out.println("Entries in database after save: " + savedEntries.size());
        } catch (Exception e) {
            System.err.println("Error saving entries to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<InvertedIndexEntry> getAll() {
        return repository.findAll();
    }

    public InvertedIndexEntry getByWord(String word) {
        return repository.findById(word).orElse(null);
    }

    public void deleteByWord(String word) {
        repository.deleteById(word);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
