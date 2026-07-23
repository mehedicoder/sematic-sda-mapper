package com.sda.mapper.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DisplayName("Ontology Registration Controller")
class OntologyRegistrationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("GET /api/v1/ontology/status should return ontology status")
    void shouldReturnOntologyStatus() throws Exception {
        setUp();
        mockMvc.perform(get("/api/v1/ontology/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTriples").isNumber())
            .andExpect(jsonPath("$.uploadedTriples").isNumber())
            .andExpect(jsonPath("$.uploadedFileExists").isBoolean());
    }

    @Test
    @DisplayName("DELETE /api/v1/ontology/uploaded should clear uploaded instances")
    void shouldClearUploadedInstances() throws Exception {
        setUp();
        mockMvc.perform(delete("/api/v1/ontology/uploaded"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("All uploaded instances cleared"))
            .andExpect(jsonPath("$.totalTriples").isNumber());
    }
}
