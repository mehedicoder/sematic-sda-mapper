package com.sda.mapper.imports;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an uploaded file within an import session.
 * Stores file metadata, classification, and validation status.
 */
@Entity
@Table(name = "uploaded_file")
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ImportSession session;

    @Column(nullable = false, length = 500)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FileType fileType = FileType.UNKNOWN;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileStatus status = FileStatus.UPLOADED;

    @Column(name = "triple_count", nullable = false)
    private int tripleCount = 0;

    @Column(name = "validation_report", columnDefinition = "TEXT")
    private String validationReport;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "storage_path", length = 1024)
    private String storagePath;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected UploadedFile() {
    }

    public UploadedFile(String filename, FileType fileType, long sizeBytes) {
        this.filename = filename;
        this.fileType = fileType;
        this.sizeBytes = sizeBytes;
        this.status = FileStatus.UPLOADED;
    }

    public UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    public ImportSession getSession() {
        return session;
    }

    public void setSession(ImportSession session) {
        this.session = session;
    }

    public String getFilename() {
        return filename;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public int getTripleCount() {
        return tripleCount;
    }

    public void setTripleCount(int tripleCount) {
        this.tripleCount = tripleCount;
    }

    public String getValidationReport() {
        return validationReport;
    }

    public void setValidationReport(String validationReport) {
        this.validationReport = validationReport;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Status of an uploaded file within an import session.
     */
    public enum FileStatus {
        UPLOADED,
        VALIDATING,
        VALID,
        INVALID,
        PARSED,
        MERGED,
        FAILED
    }
}
