package cu.searchengine.service;

import cu.searchengine.model.InvertedIndexEntry;
import cu.searchengine.repository.InvertedIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

public class InvertedIndexService {
    private static final Logger logger = LoggerFactory.getLogger(InvertedIndexService.class);
    private final InvertedIndexRepository repository;

    @Autowired
    public InvertedIndexService(InvertedIndexRepository repository) {
        this.repository = repository;
    }

    public void insertAll(List<InvertedIndexEntry> entries) {
        try {
            logger.debug("Saving {} entries to invertedIndex collection...", entries.size());
            repository.insert(entries);
            logger.debug("Successfully saved {} entries to MongoDB", entries.size());
        } catch (Exception e) {
            logger.error("Error saving entries to MongoDB: {}", e.getMessage());
        }
    }

    public int getCount() {
        return (int) repository.count();
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


    public Map<String, InvertedIndexEntry> getEntriesForWords(String[] words) {
        // Get all entries in a single query
        List<InvertedIndexEntry> entries = repository.findByWordIn(Arrays.asList(words));

        // Map the results by word for easy lookup
        Map<String, InvertedIndexEntry> result = new HashMap<>();
        for (InvertedIndexEntry entry : entries) {
            result.put(entry.getWord(), entry);
        }

        return result;
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