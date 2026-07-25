package com.sda.mapper.api;

import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.api.dto.MatchRequirementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Requirement Matching Controller")
class RequirementMatchingControllerTest {

    @Autowired
    private RequirementMatchingController controller;

    private String persistedRequirementId;

    @BeforeEach
    void setUp() {
        var persistRequest = new MatchRequirementRequest(
            null, "FlowRate", "OPC-UA", "read-only", "m\u00B3/h",
            "SIL-0", "OT-Zone-1", "SoftRealTime", null, null, 0.0, 100.0
        );
        var persistResponse = controller.persistRequirement(persistRequest);
        var body = (Map<String, String>) persistResponse.getBody();
        persistedRequirementId = body.get("requirementId");
    }

    @Test
    @DisplayName("Should return 200 with matched service when requirementId is provided")
    void shouldMatchByRequirementId() {
        var request = new MatchRequirementRequest(
            persistedRequirementId, null, null, null, null, null, null, null, null, null, null, null
        );
        var response = controller.match(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        var body = (MatchRequirementResponse) response.getBody();
        assertTrue(body.matched());
        assertNotNull(body.serviceName());
    }

    @Test
    @DisplayName("Should include connector and deploymentContext in Mode A response")
    void shouldReturnConnectorAndDeploymentContextInModeA() {
        var request = new MatchRequirementRequest(
            persistedRequirementId, null, null, null, null, null, null, null, null, null, null, null
        );
        var response = controller.match(request);
        var body = (MatchRequirementResponse) response.getBody();
        assertNotNull(body.connector(), "connector should be populated");
        assertNotNull(body.deploymentContext(), "deploymentContext should be populated");
    }

    @Test
    @DisplayName("Should include manifests in match response for matched service")
    void shouldIncludeManifestsInResponse() {
        var request = new MatchRequirementRequest(
            persistedRequirementId, null, null, null, null, null, null, null, null, null, null, null
        );
        var response = controller.match(request);
        var body = (MatchRequirementResponse) response.getBody();
        assertTrue(body.matched());
        assertNotNull(body.dockerComposeManifest(), "Docker Compose manifest should be present");
        assertNotNull(body.kubernetesManifest(), "Kubernetes manifest should be present");
        assertTrue(body.dockerComposeManifest().contains("services"),
            "Docker Compose should contain services key");
        assertTrue(body.kubernetesManifest().contains("kind"),
            "Kubernetes manifest should contain kind");
    }

    @Test
    @DisplayName("Should return 200 with matched service when inline fields are provided")
    void shouldMatchByInlineFields() {
        var request = new MatchRequirementRequest(
            null, null, "OPC-UA", "read-only", null, null, null, null, 0.90, 60, null, null
        );
        var response = controller.match(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        var body = (MatchRequirementResponse) response.getBody();
        assertTrue(body.matched());
    }

    @Test
    @DisplayName("Should return 400 when neither requirementId nor inline fields are provided")
    void shouldReturn400WhenNoModeProvided() {
        var request = new MatchRequirementRequest(
            null, null, null, null, null, null, null, null, null, null, null, null
        );
        var response = controller.match(request);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Should return 400 when both requirementId and inline fields are provided")
    void shouldReturn400WhenBothModesProvided() {
        var request = new MatchRequirementRequest(
            "REQ-001", null, "OPC-UA", "read-only", null, null, null, null, null, null, null, null
        );
        var response = controller.match(request);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Should return 201 when persisting a valid requirement")
    void shouldPersistRequirement() {
        var request = new MatchRequirementRequest(
            null, "FlowRate", "OPC-UA", "read-only", "m³/h", "SIL-2", "Plant", "Hard", null, null, 0.0, 100.0
        );
        var response = controller.persistRequirement(request);
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("requirementId"));
    }
}
