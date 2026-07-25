package com.sda.mapper.imports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ImportSession} persistence.
 */
@Repository
public interface ImportSessionRepository extends JpaRepository<ImportSession, UUID> {

    List<ImportSession> findByStatus(SessionStatus status);

    List<ImportSession> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT s FROM ImportSession s LEFT JOIN FETCH s.files LEFT JOIN FETCH s.importResult WHERE s.id = :id")
    Optional<ImportSession> findByIdWithFilesAndResult(@Param("id") UUID id);

    @Query("SELECT DISTINCT s FROM ImportSession s LEFT JOIN FETCH s.files LEFT JOIN FETCH s.importResult ORDER BY s.createdAt DESC")
    List<ImportSession> findAllWithFilesAndResult();
}
