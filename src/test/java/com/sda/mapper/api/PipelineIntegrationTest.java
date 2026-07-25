package com.sda.mapper.api;

import com.sda.mapper.api.dto.PipelineResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("Pipeline Integration Test — HTTP Layer")
class PipelineIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .build();
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    @DisplayName("POST /api/v1/pipeline/execute with MTP AML should return 200 with pipeline result")
    void shouldReturn200WithMtpAmlFile() throws Exception {
        byte[] content = getClass().getClassLoader()
            .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")
            .readAllBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file", "req002-mtp-fermentation-skid.xml", "application/xml", content);

        MvcResult result = mockMvc.perform(multipart("/api/v1/pipeline/execute").file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andReturn();

        PipelineResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), PipelineResponse.class);

        assertEquals("MTP-AML", response.fileType());
        assertTrue(response.parsedTripleCount() > 0);
        assertTrue(response.liftedTripleCount() > 0);
        assertFalse(response.matches().isEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute with OPC UA datasheet should return 200")
    void shouldReturn200WithUaDatasheet() throws Exception {
        byte[] content = getClass().getClassLoader()
            .getResourceAsStream("examples/req003-uaserver-ahu01.json")
            .readAllBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file", "req003-uaserver-ahu01.json", "application/json", content);

        MvcResult result = mockMvc.perform(multipart("/api/v1/pipeline/execute").file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andReturn();

        PipelineResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), PipelineResponse.class);

        assertEquals("OPC-UA-Datasheet", response.fileType());
        assertTrue(response.parsedTripleCount() > 0);
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute with empty file should return 400")
    void shouldReturn400WithEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.xml", "application/xml", new byte[0]);

        mockMvc.perform(multipart("/api/v1/pipeline/execute").file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("file"));
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute with unsupported file should return 400")
    void shouldReturn400WithUnsupportedFile() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
            "file", "readme.txt", "text/plain", "just text".getBytes());

        mockMvc.perform(multipart("/api/v1/pipeline/execute").file(badFile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("file"));
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute with no file parameter should return 400")
    void shouldReturn400WithNoFileParam() throws Exception {
        mockMvc.perform(multipart("/api/v1/pipeline/execute"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute should include manifests for matched services")
    void shouldIncludeManifestsForMatchedServices() throws Exception {
        byte[] content = getClass().getClassLoader()
            .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")
            .readAllBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file", "req002-mtp-fermentation-skid.xml", "application/xml", content);

        MvcResult result = mockMvc.perform(multipart("/api/v1/pipeline/execute").file(file))
            .andExpect(status().isOk())
            .andReturn();

        PipelineResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), PipelineResponse.class);

        boolean anyMatched = response.matches().values().stream()
            .anyMatch(PipelineResponse.RequirementMatch::matched);
        assertTrue(anyMatched, "At least one requirement should match MTP AML input");

        response.matches().values().stream()
            .filter(PipelineResponse.RequirementMatch::matched)
            .forEach(match -> {
                assertNotNull(match.dockerComposeManifest(),
                    "Matched service should have Docker Compose manifest");
                assertNotNull(match.kubernetesManifest(),
                    "Matched service should have Kubernetes manifest");
                assertTrue(match.dockerComposeManifest().contains("version"),
                    "Docker Compose should have version field");
                assertTrue(match.kubernetesManifest().contains("apiVersion"),
                    "Kubernetes manifest should have apiVersion");
            });
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute should return a match result")
    void shouldReturnMatchResult() throws Exception {
        byte[] content = getClass().getClassLoader()
            .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")
            .readAllBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file", "req002-mtp-fermentation-skid.xml", "application/xml", content);

        MvcResult result = mockMvc.perform(multipart("/api/v1/pipeline/execute").file(file))
            .andExpect(status().isOk())
            .andReturn();

        PipelineResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), PipelineResponse.class);

        assertFalse(response.matches().isEmpty(), "Should have at least one match result");
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute should report elapsed time and triple counts")
    void shouldReportMetadata() throws Exception {
        byte[] content = getClass().getClassLoader()
            .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")
            .readAllBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file", "req002-mtp-fermentation-skid.xml", "application/xml", content);

        MvcResult result = mockMvc.perform(multipart("/api/v1/pipeline/execute").file(file))
            .andExpect(status().isOk())
            .andReturn();

        PipelineResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), PipelineResponse.class);

        assertTrue(response.elapsedMs() >= 0, "Elapsed time should be non-negative");
        assertTrue(response.parsedTripleCount() > 0, "Parsed triple count should be positive");
        assertNotNull(response.warnings(), "Warnings list should not be null");
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute should populate match details for matched services")
    void shouldPopulateMatchDetails() throws Exception {
        byte[] content = getClass().getClassLoader()
            .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")
            .readAllBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file", "req002-mtp-fermentation-skid.xml", "application/xml", content);

        MvcResult result = mockMvc.perform(multipart("/api/v1/pipeline/execute").file(file))
            .andExpect(status().isOk())
            .andReturn();

        PipelineResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), PipelineResponse.class);

        response.matches().values().stream()
            .filter(PipelineResponse.RequirementMatch::matched)
            .forEach(match -> {
                assertNotNull(match.serviceName(), "serviceName should not be null");
                assertNotNull(match.explanation(), "explanation should not be null");
                assertFalse(match.serviceName().isBlank(), "serviceName should not be blank");
            });
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/execute with PROFINET params should match fermentation skid")
    void shouldMatchWithProfinetParams() throws Exception {
        byte[] content = getClass().getClassLoader()
            .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")
            .readAllBytes();

        MockMultipartFile file = new MockMultipartFile(
            "file", "req002-mtp-fermentation-skid.xml", "application/xml", content);

        MvcResult result = mockMvc.perform(
                multipart("/api/v1/pipeline/execute")
                    .file(file)
                    .param("protocol", "PROFINET")
                    .param("accessMode", "read-write")
                    .param("safetyIntegrityLevel", "SIL-2")
                    .param("determinismClass", "HardRealTime"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andReturn();

        PipelineResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), PipelineResponse.class);

        assertFalse(response.matches().isEmpty(), "Should match with PROFINET params");
    }
}
