package com.sda.mapper.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Options Controller Integration")
class OptionsControllerTest {

    @Autowired
    private OptionsController optionsController;

    @Test
    @DisplayName("Should return protocol options via controller")
    void testGetProtocolOptions() {
        var response = optionsController.getOptions("protocol");
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
        assertEquals("OPC-UA", response.getBody().getFirst().value());
    }

    @Test
    @DisplayName("Should return 404 for nonexistent field via controller")
    void testGetNonexistentField() {
        var response = optionsController.getOptions("nonexistent");
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Should return all field names via controller")
    void testGetFieldNames() {
        var response = optionsController.getFieldNames();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("protocol"));
        assertTrue(response.getBody().contains("accessMode"));
    }
}
