package com.sda.mapper.imports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for semantic audit trail entries.
 */
@Repository
public interface SemanticAuditRepository extends JpaRepository<SemanticAuditEntry, UUID> {

    List<SemanticAuditEntry> findBySessionId(UUID sessionId);

    List<SemanticAuditEntry> findBySessionIdAndSource(UUID sessionId, SemanticAuditEntry.InformationSource source);
}
