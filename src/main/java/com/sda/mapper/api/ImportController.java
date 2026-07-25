package com.sda.mapper.api;

import com.sda.mapper.api.dto.ImportSessionResponse;
import com.sda.mapper.api.dto.ImportSessionResponse.ImportFileResponse;
import com.sda.mapper.api.dto.ImportSessionResponse.ImportResultResponse;
import com.sda.mapper.error.ImportException;
import com.sda.mapper.imports.*;
import com.sda.mapper.parser.UploadService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for multi-file import sessions.
 * Exposes endpoints for creating sessions, uploading files, validating,
 * analyzing completeness, resolving conflicts, merging, and committing.
 */
@RestController
@RequestMapping("/api/v1/import-sessions")
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private final ImportSessionService importSessionService;
    private final UploadService uploadService;
    private final FileStorageService fileStorageService;

    public ImportController(ImportSessionService importSessionService,
                            UploadService uploadService,
                            FileStorageService fileStorageService,
                            MeterRegistry meterRegistry) {
        this.importSessionService = importSessionService;
        this.uploadService = uploadService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<ImportSessionResponse> createSession(
            @RequestBody(required = false) Map<String, String> body) {
        String name = body != null ? body.getOrDefault("name", "Import Session") : "Import Session";
        ImportSession session = importSessionService.createSession(name);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toResponse(session));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addFile(@PathVariable UUID id,
                                      @RequestParam("file") MultipartFile file) {
        try {
            UploadedFile uploaded = importSessionService.addFile(
                id, file.getOriginalFilename(), file.getSize(), file.getInputStream());

            int tripleCount = 0;
            try {
                var parseResult = uploadService.parseUpload(
                    file.getOriginalFilename(), file.getInputStream());
                tripleCount = parseResult.tripleCount();
                importSessionService.markFileUploaded(id, uploaded.getId(), tripleCount, null);
            } catch (Exception e) {
                log.warn("Parse failed for file '{}': {}", file.getOriginalFilename(), e.getMessage());
                importSessionService.markFileFailed(id, uploaded.getId(), e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                "fileId", uploaded.getId().toString(),
                "filename", uploaded.getFilename(),
                "fileType", uploaded.getFileType().name(),
                "tripleCount", tripleCount,
                "storagePath", uploaded.getStoragePath() != null ? uploaded.getStoragePath() : ""
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ImportSessionResponse> validateSession(@PathVariable UUID id) {
        ImportSession session = importSessionService.validateSession(id);
        return ResponseEntity.ok(toResponse(session));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyzeCompleteness(@PathVariable UUID id) {
        try {
            CompletenessReport report = importSessionService.analyzeCompleteness(id);
            return ResponseEntity.ok(Map.of(
                "overallScore", report.overallScore(),
                "totalEntitiesAnalyzed", report.totalEntitiesAnalyzed(),
                "categoryScores", report.categoryScores(),
                "missingCounts", report.missingCounts(),
                "missingItems", report.missingItems().stream().map(item -> Map.of(
                    "category", item.category().name(),
                    "entityUri", item.entityUri(),
                    "entityLabel", item.entityLabel(),
                    "entityType", item.entityType(),
                    "fieldPath", item.fieldPath(),
                    "description", item.description(),
                    "resolvable", item.resolvable()
                )).toList(),
                "recommendedActions", report.recommendedActions()
            ));
        } catch (ImportException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveConflict(@PathVariable UUID id,
                                              @RequestBody Map<String, Object> body) {
        try {
            int conflictIndex = (int) body.get("conflictIndex");
            String strategyStr = (String) body.get("strategy");
            ConflictResolutionService.ResolutionStrategy strategy =
                ConflictResolutionService.ResolutionStrategy.valueOf(strategyStr);
            ImportSession session = importSessionService.resolveConflict(id, conflictIndex, strategy);
            return ResponseEntity.ok(toResponse(session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> addManualEntry(@PathVariable UUID id,
                                             @RequestBody Map<String, String> body) {
        try {
            String entityUri = body.get("entityUri");
            String fieldName = body.get("fieldName");
            String value = body.get("value");
            importSessionService.addManualEntry(id, entityUri, fieldName, value);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<?> getAuditTrail(@PathVariable UUID id) {
        List<SemanticAuditEntry> entries = importSessionService.getAuditTrail(id);
        return ResponseEntity.ok(entries.stream().map(e -> Map.of(
            "id", e.getId().toString(),
            "entityUri", e.getEntityUri(),
            "fieldName", e.getFieldName(),
            "source", e.getSource().name(),
            "value", e.getValue() != null ? e.getValue() : "",
            "createdAt", e.getCreatedAt().toString()
        )).toList());
    }

    @PostMapping("/{id}/merge")
    public ResponseEntity<?> mergeSession(@PathVariable UUID id) {
        try {
            ImportResult result = importSessionService.mergeSession(id);
            return ResponseEntity.ok(toResultResponse(result));
        } catch (ImportException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/commit")
    public ResponseEntity<ImportSessionResponse> commitSession(@PathVariable UUID id) {
        ImportSession session = importSessionService.commitSession(id);
        return ResponseEntity.ok(toResponse(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportSessionResponse> getSession(@PathVariable UUID id) {
        ImportSession session = importSessionService.findSession(id);
        return ResponseEntity.ok(toResponse(session));
    }

    @GetMapping
    public ResponseEntity<List<ImportSessionResponse>> listSessions() {
        List<ImportSessionResponse> sessions = importSessionService.listSessions().stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{sessionId}/files")
    public ResponseEntity<?> listFiles(@PathVariable UUID sessionId,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String search) {
        try {
            List<UploadedFile> files = importSessionService.listFiles(sessionId);
            List<UploadedFile> filtered = files.stream()
                .filter(f -> type == null || type.isBlank() || f.getFileType().name().equalsIgnoreCase(type))
                .filter(f -> search == null || search.isBlank()
                    || f.getFilename().toLowerCase().contains(search.toLowerCase()))
                .toList();
            return ResponseEntity.ok(filtered.stream().map(f -> Map.of(
                "id", f.getId().toString(),
                "filename", f.getFilename(),
                "fileType", f.getFileType().name(),
                "status", f.getStatus().name(),
                "sizeBytes", f.getSizeBytes(),
                "tripleCount", f.getTripleCount(),
                "storagePath", f.getStoragePath() != null ? f.getStoragePath() : "",
                "createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : ""
            )).toList());
        } catch (ImportException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/files/{fileId}/download")
    public ResponseEntity<?> downloadFile(@PathVariable UUID sessionId,
                                           @PathVariable UUID fileId) {
        try {
            UploadedFile file = importSessionService.findFile(sessionId, fileId);
            if (file.getStoragePath() == null || file.getStoragePath().isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File content not stored"));
            }
            InputStream content = fileStorageService.load(file.getStoragePath());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + file.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.getSizeBytes())
                .body(content);
        } catch (ImportException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        importSessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ImportException.class)
    public ResponseEntity<Map<String, String>> handleImportError(ImportException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private ImportSessionResponse toResponse(ImportSession session) {
        List<ImportFileResponse> files = session.getFiles().stream()
            .map(f -> new ImportFileResponse(
                f.getId(), f.getFilename(), f.getFileType().name(),
                f.getStatus().name(), f.getSizeBytes(), f.getTripleCount(),
                f.getValidationReport(), f.getErrorMessage()))
            .toList();

        ImportResultResponse resultResponse = null;
        if (session.getImportResult() != null) {
            resultResponse = toResultResponse(session.getImportResult());
        }

        return new ImportSessionResponse(
            session.getId(), session.getName(), session.getStatus(),
            session.getFileCount(), session.getTotalTriples(),
            session.getValidationErrors(), session.getWarnings(),
            session.getCreatedAt(), session.getUpdatedAt(), session.getCommittedAt(),
            files, resultResponse
        );
    }

    private ImportResultResponse toResultResponse(ImportResult result) {
        return new ImportResultResponse(
            result.getAssetsFound(), result.getServicesFound(),
            result.getCapabilitiesFound(), result.getSkillsFound(),
            result.getCommunicationBindingsFound(), result.getDataConnectorsFound(),
            result.getConflictsDetected(), result.getWarnings(), result.getErrors(),
            result.getCompletenessScore(), result.getSummary()
        );
    }
}
