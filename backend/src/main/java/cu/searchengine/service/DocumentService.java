package cu.searchengine.service;

import cu.searchengine.model.Documents;
import cu.searchengine.repository.DocumentsRepository;
import org.springframework.stereotype.Service;

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

}
