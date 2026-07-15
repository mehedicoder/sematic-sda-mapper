package com.sda.mapper.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Options Configuration Service")
class OptionsConfigServiceTest {

    @Autowired
    private OptionsConfigService optionsService;

    @Test
    @DisplayName("Should load protocol options from JSON")
    void testLoadProtocolOptions() {
        List<Option> protocolOptions = optionsService.getOptions("protocol");
        assertFalse(protocolOptions.isEmpty());
        assertTrue(protocolOptions.stream().anyMatch(o -> o.value().equals("OPC-UA")));
    }

    @Test
    @DisplayName("Should return empty list for nonexistent field")
    void testGetNonexistentField() {
        List<Option> options = optionsService.getOptions("nonexistent");
        assertTrue(options.isEmpty());
    }

    @Test
    @DisplayName("Should return all field names")
    void testGetFieldNames() {
        List<String> fieldNames = optionsService.getFieldNames();
        assertFalse(fieldNames.isEmpty());
        assertTrue(fieldNames.contains("protocol"));
        assertTrue(fieldNames.contains("accessMode"));
    }

    @Test
    @DisplayName("Should check if field exists")
    void testHasField() {
        assertTrue(optionsService.hasField("protocol"));
        assertFalse(optionsService.hasField("nonexistent"));
    }
}
