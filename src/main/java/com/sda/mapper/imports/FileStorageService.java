package com.sda.mapper.imports;

import com.sda.mapper.error.ImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Manages file content storage on the filesystem.
 * Files are organized under a configurable base directory, partitioned by session ID.
 * The PostgreSQL {@code uploaded_file} table tracks metadata including the {@code storage_path}.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.file-storage.base-path:uploads}")
    private String basePath;

    private Path baseDir;

    @PostConstruct
    void init() {
        baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
            log.info("File storage initialized at: {}", baseDir);
        } catch (IOException e) {
            throw new ImportException("Failed to create file storage directory: " + baseDir, e);
        }
    }

    /**
     * Stores an input stream to the filesystem under {@code uploads/{sessionId}/{fileId}/{filename}}.
     *
     * @param sessionId the import session ID
     * @param fileId    the uploaded file ID
     * @param filename  the original filename
     * @param input     the file content stream
     * @return the relative storage path
     */
    public String store(UUID sessionId, UUID fileId, String filename, InputStream input) {
        Path dir = resolveFileDir(sessionId, fileId);
        ensureParentDirectory(dir);
        Path target = dir.resolve(sanitizeFilename(filename));

        try (InputStream in = input) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} ({} bytes)", target, Files.size(target));
        } catch (IOException e) {
            throw new ImportException("Failed to store file: " + filename, e);
        }

        return baseDir.relativize(target).toString().replace('\\', '/');
    }

    /**
     * Opens the stored file for reading.
     *
     * @param storagePath the relative storage path returned by {@link #store}
     * @return an InputStream for the file content
     */
    public InputStream load(String storagePath) {
        Path path = baseDir.resolve(storagePath).normalize();
        if (!Files.exists(path)) {
            throw new ImportException("Stored file not found: " + storagePath);
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new ImportException("Failed to read stored file: " + storagePath, e);
        }
    }

    /**
     * Deletes the stored file and its parent directory if empty.
     *
     * @param storagePath the relative storage path
     * @return true if deleted, false if not found
     */
    public boolean delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        Path path = baseDir.resolve(storagePath).normalize();
        if (!Files.exists(path)) {
            log.debug("Stored file not found for deletion: {}", storagePath);
            return false;
        }
        try {
            Files.delete(path);
            log.info("Deleted stored file: {}", path);
            Path parent = path.getParent();
            if (parent != null && isDirectoryEmpty(parent) && !parent.equals(baseDir)) {
                Files.delete(parent);
                log.info("Removed empty session directory: {}", parent);
            }
            return true;
        } catch (IOException e) {
            throw new ImportException("Failed to delete stored file: " + storagePath, e);
        }
    }

    /**
     * Deletes all files for a given session.
     *
     * @param sessionId the session ID
     */
    public void deleteSession(UUID sessionId) {
        Path sessionDir = baseDir.resolve(sessionId.toString());
        if (!Files.exists(sessionDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(sessionDir)) {
            walk.sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            log.info("Deleted all stored files for session {}", sessionId);
        } catch (IOException e) {
            log.warn("Failed to clean up session directory: {}", sessionDir, e);
        }
    }

    /**
     * Returns the size in bytes of a stored file.
     */
    public long getFileSize(String storagePath) {
        Path path = baseDir.resolve(storagePath).normalize();
        if (!Files.exists(path)) {
            return -1;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }

    private Path resolveFileDir(UUID sessionId, UUID fileId) {
        return baseDir.resolve(sessionId.toString()).resolve(fileId.toString());
    }

    private void ensureParentDirectory(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ImportException("Failed to create directory: " + path.getParent(), e);
        }
    }

    private boolean isDirectoryEmpty(Path dir) {
        try (var entries = Files.list(dir)) {
            return entries.findFirst().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Sanitizes a filename by removing path separators and null bytes.
     */
    static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replace("\0", "")
            .trim();
    }
}
