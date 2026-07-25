package com.sda.mapper.imports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ImportResult} persistence.
 */
@Repository
public interface ImportResultRepository extends JpaRepository<ImportResult, UUID> {

    Optional<ImportResult> findBySessionId(UUID sessionId);
}
