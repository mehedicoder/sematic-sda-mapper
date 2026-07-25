package com.sda.mapper.imports;

import com.sda.mapper.error.ImportException;
import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Allows manual entry of missing semantic fields and tracks the origin
 * of each piece of information (uploaded vs manually entered) for audit trail.
 */
@Service
public class SemanticCompletionService {

    private static final Logger log = LoggerFactory.getLogger(SemanticCompletionService.class);

    private static final String SDA_NS = "http://www.example.org/sda#";

    private final SemanticAuditRepository auditRepository;
    private final OntologyModelLoader ontologyModelLoader;

    public SemanticCompletionService(SemanticAuditRepository auditRepository,
                                      OntologyModelLoader ontologyModelLoader) {
        this.auditRepository = auditRepository;
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Records a manual field entry for a session.
     *
     * @param sessionId the import session ID
     * @param entityUri the URI of the entity being completed
     * @param fieldName the property/field name
     * @param value     the manually entered value
     */
    @Transactional
    public void addManualEntry(UUID sessionId, String entityUri, String fieldName, String value) {
        SemanticAuditEntry entry = new SemanticAuditEntry(
            sessionId, entityUri, fieldName,
            SemanticAuditEntry.InformationSource.MANUAL, value
        );
        auditRepository.save(entry);

        Model model = ontologyModelLoader.getModel();
        Resource entity = model.createResource(entityUri);
        entity.addProperty(
            model.createProperty(SDA_NS + fieldName),
            value
        );

        log.info("Manual entry: session={}, entity={}, field={}, value={}",
            sessionId, entityUri, fieldName, value);
    }

    /**
     * Records an uploaded field entry for audit tracking.
     */
    @Transactional
    public void recordUploadEntry(UUID sessionId, String entityUri, String fieldName, String value) {
        SemanticAuditEntry entry = new SemanticAuditEntry(
            sessionId, entityUri, fieldName,
            SemanticAuditEntry.InformationSource.UPLOAD, value
        );
        auditRepository.save(entry);
    }

    /**
     * Retrieves the audit trail for a session.
     */
    @Transactional(readOnly = true)
    public List<SemanticAuditEntry> getAuditTrail(UUID sessionId) {
        return auditRepository.findBySessionId(sessionId);
    }

    /**
     * Retrieves audit entries filtered by source.
     */
    @Transactional(readOnly = true)
    public List<SemanticAuditEntry> getAuditTrailBySource(UUID sessionId,
                                                            SemanticAuditEntry.InformationSource source) {
        return auditRepository.findBySessionIdAndSource(sessionId, source);
    }
}
