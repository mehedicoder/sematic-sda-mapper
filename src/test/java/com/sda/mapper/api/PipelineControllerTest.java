package com.sda.mapper.api;

import com.sda.mapper.api.dto.PipelineResponse;
import com.sda.mapper.reasoning.EndToEndPipelineService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Pipeline Controller")
class PipelineControllerTest {

    @Autowired
    private PipelineController controller;

    @Autowired
    private EndToEndPipelineService pipelineService;

    @Test
    @DisplayName("Should execute full pipeline with MTP AML file and return matches")
    void shouldExecutePipelineWithMtpAml() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input, "Test resource req002-mtp-fermentation-skid.xml should exist");

            MockMultipartFile file = new MockMultipartFile(
                "file", "req002-mtp-fermentation-skid.xml", "application/xml", input);

            var response = controller.execute(file, null, null, null, null, null, null, null, null, null);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());

            var body = (PipelineResponse) response.getBody();
            assertEquals("MTP-AML", body.fileType());
            assertTrue(body.parsedTripleCount() > 0, "Should parse triples from MTP AML");
            assertTrue(body.liftedTripleCount() > 0, "Lifted triples should be present");
            assertTrue(body.inferredTripleCount() > 0, "Inferred triples should be present");
            assertFalse(body.matches().isEmpty(), "Should have match results");

            for (var match : body.matches().entrySet()) {
                assertNotNull(match.getKey(), "Requirement ID should not be null");
                assertNotNull(match.getValue(), "Match result should not be null");
            }
        }
    }

    @Test
    @DisplayName("Should execute full pipeline with OPC UA datasheet and return matches")
    void shouldExecutePipelineWithUaDatasheet() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req003-uaserver-ahu01.json")) {
            assertNotNull(input, "Test resource req003-uaserver-ahu01.json should exist");

            MockMultipartFile file = new MockMultipartFile(
                "file", "req003-uaserver-ahu01.json", "application/json", input);

            var response = controller.execute(file, null, null, null, null, null, null, null, null, null);
            assertEquals(200, response.getStatusCode().value());

            var body = (PipelineResponse) response.getBody();
            assertEquals("OPC-UA-Datasheet", body.fileType());
            assertTrue(body.parsedTripleCount() > 0);
            assertFalse(body.matches().isEmpty());
        }
    }

    @Test
    @DisplayName("Should generate Docker Compose and Kubernetes manifests for matched services")
    void shouldGenerateManifestsForMatchedServices() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);

            MockMultipartFile file = new MockMultipartFile(
                "file", "req002-mtp-fermentation-skid.xml", "application/xml", input);

            var response = controller.execute(file, null, null, null, null, null, null, null, null, null);
            var body = (PipelineResponse) response.getBody();

            boolean anyMatched = body.matches().values().stream()
                .anyMatch(PipelineResponse.RequirementMatch::matched);
            assertTrue(anyMatched, "At least one requirement should match");

            body.matches().values().stream()
                .filter(PipelineResponse.RequirementMatch::matched)
                .forEach(match -> {
                    assertNotNull(match.dockerComposeManifest(),
                        "Docker Compose manifest should be present for matched service");
                    assertNotNull(match.kubernetesManifest(),
                        "Kubernetes manifest should be present for matched service");
                    assertTrue(match.dockerComposeManifest().contains("services"),
                        "Docker Compose should contain services key");
                    assertTrue(match.kubernetesManifest().contains("kind"),
                        "Kubernetes manifest should contain kind");
                });
        }
    }

    @Test
    @DisplayName("Should return 400 when file is empty")
    void shouldReturn400WhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "test.xml", "application/xml", new byte[0]);

        var response = controller.execute(emptyFile, null, null, null, null, null, null, null, null, null);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Should return 400 when file has unsupported extension")
    void shouldReturn400ForUnsupportedFileType() {
        MockMultipartFile unsupportedFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "hello world".getBytes());

        var response = controller.execute(unsupportedFile, null, null, null, null, null, null, null, null, null);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Should include warnings in pipeline response when present")
    void shouldIncludeWarningsInResponse() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);

            MockMultipartFile file = new MockMultipartFile(
                "file", "req002-mtp-fermentation-skid.xml", "application/xml", input);

            var response = controller.execute(file, null, null, null, null, null, null, null, null, null);
            var body = (PipelineResponse) response.getBody();
            assertNotNull(body.warnings());
        }
    }

    @Test
    @DisplayName("Should report elapsed time in pipeline response")
    void shouldReportElapsedTime() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);

            MockMultipartFile file = new MockMultipartFile(
                "file", "req002-mtp-fermentation-skid.xml", "application/xml", input);

            var response = controller.execute(file, null, null, null, null, null, null, null, null, null);
            var body = (PipelineResponse) response.getBody();
            assertTrue(body.elapsedMs() >= 0, "Elapsed time should be non-negative");
        }
    }

    @Test
    @DisplayName("Should filter pipeline to specific requirement IDs when provided")
    void shouldFilterByRequirementIds() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);

            MockMultipartFile file = new MockMultipartFile(
                "file", "req002-mtp-fermentation-skid.xml", "application/xml", input);

            var response = controller.execute(file, "REQ-002", null, null, null, null, null, null, null, null);
            assertEquals(200, response.getStatusCode().value());

            var body = (PipelineResponse) response.getBody();
            assertTrue(body.matches().containsKey("REQ-002"), "Should contain REQ-002");
            assertFalse(body.matches().containsKey("REQ-001"), "Should not contain REQ-001");
            assertFalse(body.matches().containsKey("REQ-003"), "Should not contain REQ-003");
        }
    }

    @Test
    @DisplayName("Should filter pipeline to multiple specific requirement IDs")
    void shouldFilterByMultipleRequirementIds() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);

            MockMultipartFile file = new MockMultipartFile(
                "file", "req002-mtp-fermentation-skid.xml", "application/xml", input);

            var response = controller.execute(file, "REQ-001,REQ-003", null, null, null, null, null, null, null, null);
            assertEquals(200, response.getStatusCode().value());

            var body = (PipelineResponse) response.getBody();
            assertTrue(body.matches().containsKey("REQ-001"), "Should contain REQ-001");
            assertTrue(body.matches().containsKey("REQ-003"), "Should contain REQ-003");
            assertFalse(body.matches().containsKey("REQ-002"), "Should not contain REQ-002");
        }
    }
}
