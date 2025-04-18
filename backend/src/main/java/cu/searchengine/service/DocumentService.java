package cu.searchengine.service;

import cu.searchengine.model.Documents;
import cu.searchengine.repository.DocumentsRepository;
import org.springframework.stereotype.Service;

import java.util.*;

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
    public Optional<Documents> getDocumentByUrl(String url) {return documentsRepository.findByUrl(url);}
    public List<Documents> getAllDocuments() {return documentsRepository.findAll();}
    public Map<Integer, List<Integer>> getWebGraph() {
        List<Documents> documents = documentsRepository.findAll();

        // Build URL to ID map for fast lookups
        Map<String, Integer> urlToIdMap = new HashMap<>();
        for (Documents doc : documents) {
            urlToIdMap.put(doc.getUrl(), doc.getId());
        }

        // Now build the graph using the map
        Map<Integer, List<Integer>> webGraph = new HashMap<>();
        for (Documents doc : documents) {
            List<Integer> linkedDocIds = new ArrayList<>();

            for (String url : doc.getLinks()) {
                if (urlToIdMap.containsKey(url)) {
                    linkedDocIds.add(urlToIdMap.get(url));
                }
            }

            webGraph.put(doc.getId(), linkedDocIds);
        }

        return webGraph;
    }


}
