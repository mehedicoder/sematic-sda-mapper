package com.sda.mapper.imports;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a multi-file import session.
 * Tracks the lifecycle of uploading, validating, and merging multiple
 * heterogeneous device description files into the shared ontology model.
 */
@Entity
@Table(name = "import_session")
public class ImportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status = SessionStatus.CREATED;

    @Column(nullable = false)
    private int fileCount = 0;

    @Column(nullable = false)
    private int totalTriples = 0;

    @Column(nullable = false)
    private int validationErrors = 0;

    @Column(nullable = false)
    private int warnings = 0;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "committed_at")
    private Instant committedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UploadedFile> files = new ArrayList<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ImportResult importResult;

    protected ImportSession() {
    }

    public ImportSession(String name) {
        this.name = name;
        this.status = SessionStatus.CREATED;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getTotalTriples() {
        return totalTriples;
    }

    public void setTotalTriples(int totalTriples) {
        this.totalTriples = totalTriples;
    }

    public int getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(int validationErrors) {
        this.validationErrors = validationErrors;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public List<UploadedFile> getFiles() {
        return files;
    }

    public void setFiles(List<UploadedFile> files) {
        this.files = files;
    }

    public ImportResult getImportResult() {
        return importResult;
    }

    public void setImportResult(ImportResult importResult) {
        this.importResult = importResult;
    }

    public void addFile(UploadedFile file) {
        files.add(file);
        file.setSession(this);
        fileCount++;
        updatedAt = Instant.now();
    }

    public void removeFile(UploadedFile file) {
        files.remove(file);
        file.setSession(null);
        fileCount--;
        updatedAt = Instant.now();
    }
}
