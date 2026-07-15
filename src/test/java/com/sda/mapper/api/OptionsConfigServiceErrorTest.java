package com.sda.mapper.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Options Configuration Service Error Handling")
class OptionsConfigServiceErrorTest {

    @Autowired
    private OptionsConfigService optionsService;

    @Test
    @DisplayName("Should return 404-equivalent empty list for nonexistent field")
    void testNonexistentFieldReturnsEmpty() {
        var response = new OptionsController(optionsService).getOptions("nonexistent");
        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("Should return empty list from service for nonexistent field")
    void testServiceReturnsEmptyForNonexistent() {
        var options = optionsService.getOptions("nonexistent_field_xyz");
        assertNotNull(options);
        assertTrue(options.isEmpty());
    }

    @Test
    @DisplayName("Should have all expected field names from options.json")
    void testExpectedFieldNamesPresent() {
        assertTrue(optionsService.hasField("protocol"));
        assertTrue(optionsService.hasField("accessMode"));
        assertTrue(optionsService.hasField("engineeringUnit"));
        assertTrue(optionsService.hasField("safetyIntegrityLevel"));
        assertTrue(optionsService.hasField("networkZone"));
        assertTrue(optionsService.hasField("determinismClass"));
        assertTrue(optionsService.hasField("informationModel"));
    }
}
