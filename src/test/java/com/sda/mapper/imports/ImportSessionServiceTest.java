package com.sda.mapper.imports;

import com.sda.mapper.error.ImportException;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.parser.ParseResult;
import com.sda.mapper.parser.UploadService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Import Session Service")
class ImportSessionServiceTest {

    @Mock
    private ImportSessionRepository sessionRepository;
    @Mock
    private UploadedFileRepository fileRepository;
    @Mock
    private ImportResultRepository resultRepository;
    @Mock
    private FileTypeClassifier classifier;
    @Mock
    private ImportValidator validator;
    @Mock
    private OntologyMergeService mergeService;
    @Mock
    private ConflictDetectionService conflictService;
    @Mock
    private ImportReportService reportService;
    private SimpleMeterRegistry meterRegistry;
    @Mock
    private OntologyModelLoader ontologyModelLoader;

    @Mock
    private SemanticValidationService semanticValidationService;
    @Mock
    private MissingInformationDetector missingInfoDetector;
    @Mock
    private ImportRecommendationService recommendationService;
    @Mock
    private ConflictResolutionService conflictResolutionService;
    @Mock
    private SemanticCompletionService completionService;

    @Mock
    private FileStorageService fileStorageService;

    private ImportSessionService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        service = new ImportSessionService(
            sessionRepository, fileRepository, resultRepository,
            classifier, validator, mergeService, conflictService,
            reportService, semanticValidationService, missingInfoDetector,
            recommendationService, conflictResolutionService,
            completionService, ontologyModelLoader, fileStorageService, meterRegistry
        );
    }

    @Test
    @DisplayName("Should create import session")
    void shouldCreateSession() {
        when(sessionRepository.save(any(ImportSession.class))).thenAnswer(i -> i.getArgument(0));

        ImportSession session = service.createSession("Test Import");

        assertNotNull(session);
        assertEquals("Test Import", session.getName());
        assertEquals(SessionStatus.CREATED, session.getStatus());
        verify(sessionRepository).save(any(ImportSession.class));
    }

    @Test
    @DisplayName("Should add file to session")
    void shouldAddFileToSession() {
        ImportSession session = new ImportSession("Test");
        UUID sessionId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(sessionRepository.findByIdWithFilesAndResult(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ImportSession.class))).thenAnswer(i -> {
            ImportSession s = i.getArgument(0);
            for (UploadedFile f : s.getFiles()) {
                if (f.getId() == null) {
                    f.setId(fileId);
                }
            }
            return s;
        });
        when(validator.validateSize(anyLong())).thenReturn(true);
        when(fileStorageService.store(any(UUID.class), any(UUID.class), anyString(), any(InputStream.class)))
            .thenReturn("uploads/test/file.xml");
        when(fileStorageService.load(anyString()))
            .thenReturn(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
        when(fileRepository.save(any(UploadedFile.class))).thenAnswer(i -> i.getArgument(0));

        InputStream input = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        UploadedFile file = service.addFile(sessionId, "service.xml", 100, input);

        assertNotNull(file);
        assertEquals("service.xml", file.getFilename());
        assertNotNull(file.getStoragePath());
        assertEquals("uploads/test/file.xml", file.getStoragePath());
        verify(fileStorageService).store(eq(sessionId), eq(fileId), eq("service.xml"), any(InputStream.class));
    }

    @Test
    @DisplayName("Should reject file exceeding size limit")
    void shouldRejectOversizedFile() {
        ImportSession session = new ImportSession("Test");
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdWithFilesAndResult(sessionId)).thenReturn(Optional.of(session));
        when(validator.validateSize(anyLong())).thenReturn(false);

        InputStream input = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        assertThrows(ImportException.class,
            () -> service.addFile(sessionId, "service.xml", 60_000_000, input));
    }

    @Test
    @DisplayName("Should reject adding files to committed session")
    void shouldRejectAddingFilesToCommittedSession() {
        ImportSession session = new ImportSession("Test");
        session.setStatus(SessionStatus.COMMITTED);
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdWithFilesAndResult(sessionId)).thenReturn(Optional.of(session));

        InputStream input = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        assertThrows(ImportException.class,
            () -> service.addFile(sessionId, "service.xml", 100, input));
    }

    @Test
    @DisplayName("Should mark file as uploaded with triple count")
    void shouldMarkFileUploaded() {
        ImportSession session = new ImportSession("Test");
        UUID sessionId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UploadedFile file = new UploadedFile("test.xml", FileType.MTP_AML, 100);
        session.addFile(file);

        when(sessionRepository.findByIdWithFilesAndResult(sessionId)).thenReturn(Optional.of(session));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(sessionRepository.save(any(ImportSession.class))).thenAnswer(i -> i.getArgument(0));

        service.markFileUploaded(sessionId, fileId, 42, "hash123");

        assertEquals(42, file.getTripleCount());
        assertEquals("hash123", file.getContentHash());
    }

    @Test
    @DisplayName("Should mark file as failed with error message")
    void shouldMarkFileFailed() {
        ImportSession session = new ImportSession("Test");
        UUID sessionId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UploadedFile file = new UploadedFile("test.xml", FileType.MTP_AML, 100);
        session.addFile(file);

        when(sessionRepository.findByIdWithFilesAndResult(sessionId)).thenReturn(Optional.of(session));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(sessionRepository.save(any(ImportSession.class))).thenAnswer(i -> i.getArgument(0));

        service.markFileFailed(sessionId, fileId, "Parse error");

        assertEquals(UploadedFile.FileStatus.FAILED, file.getStatus());
        assertEquals("Parse error", file.getErrorMessage());
    }

    @Test
    @DisplayName("Should throw when session not found")
    void shouldThrowWhenSessionNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(sessionRepository.findByIdWithFilesAndResult(nonExistent)).thenReturn(Optional.empty());

        assertThrows(ImportException.class,
            () -> service.findSession(nonExistent));
    }

    @Test
    @DisplayName("Should list all sessions ordered by creation date")
    void shouldListSessions() {
        when(sessionRepository.findAllWithFilesAndResult()).thenReturn(Collections.emptyList());

        var sessions = service.listSessions();

        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }

    @Test
    @DisplayName("Should delete session")
    void shouldDeleteSession() {
        UUID sessionId = UUID.randomUUID();

        service.deleteSession(sessionId);

        verify(sessionRepository).deleteById(sessionId);
    }
}
