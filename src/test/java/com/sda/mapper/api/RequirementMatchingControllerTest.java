package com.sda.mapper.api;

import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.api.dto.MatchRequirementResponse;
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

    @Test
    @DisplayName("Should return 200 with matched service when requirementId is provided")
    void shouldMatchByRequirementId() {
        var request = new MatchRequirementRequest(
            "REQ-001", null, null, null, null, null, null, null, null, null, null, null
        );
        var response = controller.match(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        var body = (MatchRequirementResponse) response.getBody();
        assertTrue(body.matched());
        assertNotNull(body.serviceName());
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
