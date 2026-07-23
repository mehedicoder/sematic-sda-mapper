package com.sda.mapper.api;

import com.sda.mapper.error.OntologyConflictException;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.ontology.OntologyPersistenceService;
import com.sda.mapper.ontology.ServiceRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for ontology service registration.
 * Provides endpoints to register uploaded services into the ontology,
 * clear uploaded instances, and query upload status.
 */
@RestController
@RequestMapping("/api/v1/ontology")
public class OntologyRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(OntologyRegistrationController.class);

    private final ServiceRegistrationService registrationService;
    private final OntologyPersistenceService persistenceService;
    private final OntologyModelLoader ontologyModelLoader;

    public OntologyRegistrationController(
            ServiceRegistrationService registrationService,
            OntologyPersistenceService persistenceService,
            OntologyModelLoader ontologyModelLoader) {
        this.registrationService = registrationService;
        this.persistenceService = persistenceService;
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Returns the current ontology status: total triples, uploaded triples, and file existence.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        long totalTriples = ontologyModelLoader.getModel().size();
        long uploadedTriples = registrationService.getUploadedTripleCount();
        boolean uploadedFileExists = persistenceService.hasUploadedInstances();

        return ResponseEntity.ok(Map.of(
            "totalTriples", totalTriples,
            "uploadedTriples", uploadedTriples,
            "uploadedFileExists", uploadedFileExists,
            "baseOntologyTriples", totalTriples - uploadedTriples
        ));
    }

    /**
     * Clears all uploaded instances and reloads the ontology.
     */
    @DeleteMapping("/uploaded")
    public ResponseEntity<Map<String, Object>> clearUploaded() {
        log.info("Clearing all uploaded instances");
        registrationService.clearAllUploaded();
        long totalTriples = ontologyModelLoader.getModel().size();
        return ResponseEntity.ok(Map.of(
            "message", "All uploaded instances cleared",
            "totalTriples", totalTriples
        ));
    }
}
