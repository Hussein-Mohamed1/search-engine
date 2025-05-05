package cu.searchengine.service;

import cu.searchengine.model.Documents;
import cu.searchengine.repository.DocumentsRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.BlockingQueue;

@Service
public class DocumentService {
    private final DocumentsRepository documentsRepository;

    public DocumentService(DocumentsRepository documentsRepository) {
        this.documentsRepository = documentsRepository;
    }

    public void add(Documents document) {
        documentsRepository.save(document);
    }

    public void update(Documents document) {
        documentsRepository.save(document);
    }

    public void delete(Documents document) {
        documentsRepository.delete(document);
    }

    public Documents getDocumentById(int id) {
        return documentsRepository.findById(id).get();
    }

    public Optional<Documents> getDocumentByUrl(String url) {
        return documentsRepository.findByUrl(url);
    }

    public List<Documents> getAllDocuments() {

        return documentsRepository.findAll();
    }

    public int getNumberOfDocuments() {
        return (int) documentsRepository.count();
    }

    public Map<Integer, Set<Integer>> getWebGraph() {
        List<Documents> documents = documentsRepository.findAll();

        // Build URL to ID map for fast lookups
        Map<Integer, Set<Integer>> webGraph = new HashMap<>();
        for (Documents doc : documents) {
            webGraph.put(doc.getUrl().hashCode(), doc.getWebGraph());
        }

        return webGraph;
    }
    public boolean documentExists(Integer id) {
        return documentsRepository.existsById(id);
    }

    public void updatePopularityScore(Integer id, Double popularityScore) {
        documentsRepository.updatePopularityScore(id, popularityScore);
    }

    public Map<Integer, Set<Integer>> getIncomingLinks() {
        List<Documents> documents = documentsRepository.findAll();

        // Build URL to ID map for fast lookups
        Map<Integer, Set<Integer>> incomingLinks = new HashMap<>();
        for (Documents doc : documents) {
            incomingLinks.put(doc.getUrl().hashCode(), doc.getIncomingLinks());
        }

        return incomingLinks;
    }

    public void addAll(BlockingQueue<Documents> buffer) {

        documentsRepository.saveAll(buffer);
    }

    public void addAll(List<Documents> buffer) {

        documentsRepository.saveAll(buffer);
    }


    public List<Documents> getDocumentsToIndex() {

        return documentsRepository.findByInvertedIndexProcessedFalse();
    }

    public void markDocumentsAsIndexed(List<Documents> docs) {
        for (Documents doc : docs) {
            doc.setInvertedIndexProcessed(true);
        }
        documentsRepository.saveAll(docs);
    }

    public List<Documents> getDocumentsByIds(Set<Integer> docIds) {
        return documentsRepository.findAllById(docIds);
    }
}
