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

    public void insertAll(List<InvertedIndexEntry> entries) {
        try {
            System.out.println("Saving " + entries.size() + " entries to invertedIndex collection...");
            repository.insert(entries);
            System.out.println("Successfully saved " + entries.size() + " entries to MongoDB");
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

    public List<InvertedIndexEntry> getByWords(List<String> words) {
        return repository.findAllById(words);
    }

    public void deleteByWord(String word) {
        repository.deleteById(word);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public void save(InvertedIndexEntry entry) {
        repository.save(entry);
    }

    public void saveAll(List<InvertedIndexEntry> entries) {
        repository.saveAll(entries);
    }
}
