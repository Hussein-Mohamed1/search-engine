package cu.searchengine.controller;

import cu.searchengine.model.Documents;
import cu.searchengine.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<Documents> createDocument(@RequestBody Documents document) {
        documentService.add(document);
        return new ResponseEntity<>(document, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Documents> getDocumentById(@PathVariable String id) {
        try {
            Documents document = documentService.getDocumentById(id);
            return new ResponseEntity<>(document, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Documents> updateDocument(@PathVariable String id, @RequestBody Documents document) {
        try {
            // Verify document exists first
            documentService.getDocumentById(id);
            // Ensure ID in path matches document ID
            if (!id.equals(document.getId())) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            documentService.update(document);
            return new ResponseEntity<>(document, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        try {
            Documents document = documentService.getDocumentById(id);
            documentService.delete(document);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Note: This method would need to be added to DocumentService
    @GetMapping
    public ResponseEntity<List<Documents>> getAllDocuments() {
        // You'll need to add a findAll method to your service
        // return new ResponseEntity<>(documentService.findAll(), HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}