package com.sda.mapper.imports;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit trail entry tracking the origin of semantic information.
 * Records whether information came from an uploaded file or was entered manually.
 */
@Entity
@Table(name = "semantic_audit")
public class SemanticAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "entity_uri", nullable = false, length = 512)
    private String entityUri;

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InformationSource source;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected SemanticAuditEntry() {
    }

    public SemanticAuditEntry(UUID sessionId, String entityUri, String fieldName,
                               InformationSource source, String value) {
        this.sessionId = sessionId;
        this.entityUri = entityUri;
        this.fieldName = fieldName;
        this.source = source;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getEntityUri() {
        return entityUri;
    }

    public String getFieldName() {
        return fieldName;
    }

    public InformationSource getSource() {
        return source;
    }

    public String getValue() {
        return value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Origin of semantic information.
     */
    public enum InformationSource {
        UPLOAD,
        MANUAL
    }
}
