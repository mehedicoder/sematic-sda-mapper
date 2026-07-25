package com.sda.mapper.imports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link UploadedFile} persistence.
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    List<UploadedFile> findBySessionId(UUID sessionId);

    List<UploadedFile> findBySessionIdAndStatus(UUID sessionId, UploadedFile.FileStatus status);

    long countBySessionId(UUID sessionId);

    long countBySessionIdAndFileType(UUID sessionId, FileType fileType);
}
