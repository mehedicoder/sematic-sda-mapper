package com.sda.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the application context starts without external dependencies.
 */
@SpringBootTest
@DisplayName("Application Context")
class SemanticSdaMapperApplicationTest {

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
    }
}
