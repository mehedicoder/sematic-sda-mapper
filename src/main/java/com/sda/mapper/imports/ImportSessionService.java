package com.sda.mapper.imports;

import com.sda.mapper.error.ImportException;
import com.sda.mapper.ontology.OntologyModelLoader;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages import session lifecycle: create, upload files, validate,
 * analyze completeness, resolve conflicts, merge, commit.
 * Extended in Phase 16 with semantic analysis and interactive completion.
 */
@Service
public class ImportSessionService {

    private static final Logger log = LoggerFactory.getLogger(ImportSessionService.class);

    private final ImportSessionRepository sessionRepository;
    private final UploadedFileRepository fileRepository;
    private final ImportResultRepository resultRepository;
    private final FileTypeClassifier classifier;
    private final ImportValidator validator;
    private final OntologyMergeService mergeService;
    private final ConflictDetectionService conflictService;
    private final ImportReportService reportService;
    private final SemanticValidationService semanticValidationService;
    private final MissingInformationDetector missingInfoDetector;
    private final ImportRecommendationService recommendationService;
    private final ConflictResolutionService conflictResolutionService;
    private final SemanticCompletionService completionService;
    private final OntologyModelLoader ontologyModelLoader;
    private final FileStorageService fileStorageService;
    private final MeterRegistry meterRegistry;

    private final Counter sessionCreatedCounter;
    private final Counter fileUploadedCounter;
    private final Timer mergeTimer;
    private final Timer analyzeTimer;

    public ImportSessionService(ImportSessionRepository sessionRepository,
                                 UploadedFileRepository fileRepository,
                                 ImportResultRepository resultRepository,
                                 FileTypeClassifier classifier,
                                 ImportValidator validator,
                                 OntologyMergeService mergeService,
                                 ConflictDetectionService conflictService,
                                 ImportReportService reportService,
                                 SemanticValidationService semanticValidationService,
                                 MissingInformationDetector missingInfoDetector,
                                 ImportRecommendationService recommendationService,
                                 ConflictResolutionService conflictResolutionService,
                                 SemanticCompletionService completionService,
                                 OntologyModelLoader ontologyModelLoader,
                                 FileStorageService fileStorageService,
                                 MeterRegistry meterRegistry) {
        this.sessionRepository = sessionRepository;
        this.fileRepository = fileRepository;
        this.resultRepository = resultRepository;
        this.classifier = classifier;
        this.validator = validator;
        this.mergeService = mergeService;
        this.conflictService = conflictService;
        this.reportService = reportService;
        this.semanticValidationService = semanticValidationService;
        this.missingInfoDetector = missingInfoDetector;
        this.recommendationService = recommendationService;
        this.conflictResolutionService = conflictResolutionService;
        this.completionService = completionService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.fileStorageService = fileStorageService;
        this.meterRegistry = meterRegistry;
        this.sessionCreatedCounter = Counter.builder("import.sessions.created")
            .description("Number of import sessions created")
            .register(meterRegistry);
        this.fileUploadedCounter = Counter.builder("import.files.uploaded")
            .description("Number of files uploaded to import sessions")
            .register(meterRegistry);
        this.mergeTimer = Timer.builder("import.merge.duration")
            .description("Time to merge import session")
            .register(meterRegistry);
        this.analyzeTimer = Timer.builder("import.analyze.duration")
            .description("Time to analyze semantic completeness")
            .register(meterRegistry);
    }

    @Transactional
    public ImportSession createSession(String name) {
        log.info("Creating import session: {}", name);
        ImportSession session = new ImportSession(name);
        session = sessionRepository.save(session);
        sessionCreatedCounter.increment();
        return session;
    }

    @Transactional
    public UploadedFile addFile(UUID sessionId, String filename, long sizeBytes,
                                 java.io.InputStream input) {
        ImportSession session = findSession(sessionId);
        if (session.getStatus() == SessionStatus.COMMITTED) {
            throw new ImportException("Cannot add files to a committed session");
        }

        if (!validator.validateSize(sizeBytes)) {
            throw new ImportException("File size " + sizeBytes + " bytes is invalid or exceeds the 50 MB limit");
        }

        UploadedFile file = new UploadedFile(filename, FileType.UNKNOWN, sizeBytes);
        session.addFile(file);
        session.setStatus(SessionStatus.UPLOADING);
        session = sessionRepository.save(session);

        UUID fileId = file.getId();
        if (fileId == null) {
            throw new ImportException("File ID not generated after save for: " + filename);
        }

        String storagePath = fileStorageService.store(sessionId, fileId, filename, input);
        file.setStoragePath(storagePath);

        try (var storedInput = fileStorageService.load(storagePath)) {
            FileType fileType = classifier.classify(filename, storedInput);
            file.setFileType(fileType);
        } catch (ImportException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportException("Failed to classify file: " + filename, e);
        }

        fileRepository.save(file);
        fileUploadedCounter.increment();

        log.info("Added file '{}' (type={}, size={}, storage={}) to session {}",
            filename, file.getFileType(), sizeBytes, storagePath, sessionId);
        return file;
    }

    @Transactional
    public void markFileUploaded(UUID sessionId, UUID fileId, int tripleCount, String contentHash) {
        ImportSession session = findSession(sessionId);
        UploadedFile file = findFile(fileId);
        file.setTripleCount(tripleCount);
        file.setContentHash(contentHash);
        file.setStatus(UploadedFile.FileStatus.UPLOADED);
        session.setTotalTriples(session.getTotalTriples() + tripleCount);
        sessionRepository.save(session);
    }

    @Transactional
    public void markFileFailed(UUID sessionId, UUID fileId, String errorMessage) {
        UploadedFile file = findFile(fileId);
        file.setStatus(UploadedFile.FileStatus.FAILED);
        file.setErrorMessage(errorMessage);
        fileRepository.save(file);

        ImportSession session = findSession(sessionId);
        session.setValidationErrors(session.getValidationErrors() + 1);
        sessionRepository.save(session);
    }

    @Transactional
    public ImportSession validateSession(UUID sessionId) {
        log.info("Validating import session {}", sessionId);
        ImportSession session = findSession(sessionId);
        session.setStatus(SessionStatus.VALIDATING);
        sessionRepository.save(session);

        List<UploadedFile> files = fileRepository.findBySessionId(sessionId);
        int errors = 0;
        int warns = 0;

        for (UploadedFile file : files) {
            file.setStatus(UploadedFile.FileStatus.VALIDATING);
            fileRepository.save(file);

            var report = validator.validate(file.getFilename(), file.getFileType());
            file.setValidationReport(report.message());

            if (!report.valid()) {
                file.setStatus(UploadedFile.FileStatus.INVALID);
                errors++;
            } else {
                file.setStatus(UploadedFile.FileStatus.VALID);
            }
            warns += report.warnings();
            fileRepository.save(file);
        }

        session.setValidationErrors(errors);
        session.setWarnings(warns);
        session.setStatus(errors > 0 ? SessionStatus.FAILED : SessionStatus.VALIDATED);
        sessionRepository.save(session);

        log.info("Validation complete for session {}: {} errors, {} warnings", sessionId, errors, warns);
        return session;
    }

    @Transactional
    public CompletenessReport analyzeCompleteness(UUID sessionId) {
        log.info("Analyzing semantic completeness for session {}", sessionId);
        ImportSession session = findSession(sessionId);
        session.setStatus(SessionStatus.ANALYZING);
        sessionRepository.save(session);

        List<UploadedFile> files = fileRepository.findBySessionId(sessionId);
        var mergeResult = mergeService.mergeFiles(files);
        Model mergedModel = mergeResult.model();

        CompletenessReport report = analyzeTimer.record(() ->
            reportService.generateCompletenessReport(mergedModel));

        session.setStatus(SessionStatus.ANALYZED);
        sessionRepository.save(session);

        log.info("Completeness analysis for session {}: score={}%, {} missing items",
            sessionId, String.format("%.1f", report.overallScore()), report.missingItems().size());
        return report;
    }

    @Transactional
    public ImportSession resolveConflict(UUID sessionId, int conflictIndex,
                                          ConflictResolutionService.ResolutionStrategy strategy) {
        log.info("Resolving conflict {} in session {} with strategy {}", conflictIndex, sessionId, strategy);
        ImportSession session = findSession(sessionId);
        session.setStatus(SessionStatus.RESOLVING);
        sessionRepository.save(session);

        session.setStatus(SessionStatus.RESOLVED);
        sessionRepository.save(session);

        log.info("Conflict {} resolved in session {}", conflictIndex, sessionId);
        return session;
    }

    @Transactional
    public void addManualEntry(UUID sessionId, String entityUri, String fieldName, String value) {
        log.info("Adding manual entry for session {}: {}.{})", sessionId, entityUri, fieldName);
        completionService.addManualEntry(sessionId, entityUri, fieldName, value);
    }

    @Transactional
    public List<SemanticAuditEntry> getAuditTrail(UUID sessionId) {
        return completionService.getAuditTrail(sessionId);
    }

    @Transactional
    public ImportResult mergeSession(UUID sessionId) {
        log.info("Merging import session {}", sessionId);
        ImportSession session = findSession(sessionId);

        if (session.getStatus() == SessionStatus.FAILED) {
            throw new ImportException("Cannot merge a session with validation errors");
        }

        session.setStatus(SessionStatus.MERGING);
        sessionRepository.save(session);

        ImportResult result = mergeTimer.record(() -> {
            List<UploadedFile> files = fileRepository.findBySessionId(sessionId);
            var mergeResult = mergeService.mergeFiles(files);
            var conflicts = conflictService.detectConflicts(mergeResult.model());

            ImportResult importResult = reportService.generateReport(session, mergeResult, conflicts);
            resultRepository.save(importResult);
            session.setImportResult(importResult);

            return importResult;
        });

        session.setStatus(SessionStatus.MERGED);
        sessionRepository.save(session);

        log.info("Merge complete for session {}: {} assets, {} services, {} conflicts",
            sessionId, result.getAssetsFound(), result.getServicesFound(),
            result.getConflictsDetected());
        return result;
    }

    @Transactional
    public ImportSession commitSession(UUID sessionId) {
        log.info("Committing import session {}", sessionId);
        ImportSession session = findSession(sessionId);

        if (session.getStatus() != SessionStatus.MERGED) {
            throw new ImportException("Session must be merged before committing");
        }

        List<UploadedFile> files = fileRepository.findBySessionId(sessionId);
        mergeService.commitToModel(sessionId, files);
        session.setStatus(SessionStatus.COMMITTED);
        session.setCommittedAt(Instant.now());
        sessionRepository.save(session);

        log.info("Committed import session {}", sessionId);
        return session;
    }

    @Transactional(readOnly = true)
    public ImportSession findSession(UUID sessionId) {
        return sessionRepository.findByIdWithFilesAndResult(sessionId)
            .orElseThrow(() -> new ImportException("Import session not found: " + sessionId));
    }

    @Transactional(readOnly = true)
    public List<ImportSession> listSessions() {
        return sessionRepository.findAllWithFilesAndResult();
    }

    @Transactional(readOnly = true)
    public List<UploadedFile> listFiles(UUID sessionId) {
        findSession(sessionId);
        return fileRepository.findBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public UploadedFile findFile(UUID sessionId, UUID fileId) {
        findSession(sessionId);
        return fileRepository.findById(fileId)
            .filter(f -> f.getSession().getId().equals(sessionId))
            .orElseThrow(() -> new ImportException("File not found: " + fileId));
    }

    @Transactional
    public void deleteSession(UUID sessionId) {
        log.info("Deleting import session {}", sessionId);
        fileStorageService.deleteSession(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    private UploadedFile findFile(UUID fileId) {
        return fileRepository.findById(fileId)
            .orElseThrow(() -> new ImportException("Uploaded file not found: " + fileId));
    }
}
