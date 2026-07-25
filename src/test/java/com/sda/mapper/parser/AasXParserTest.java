package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AASX Parser")
class AasXParserTest {

    private final AasParser aasParser = new AasParser();
    private final AasXParser parser = new AasXParser(aasParser);

    @Nested
    @DisplayName("Valid AASX packages")
    class ValidAasx {

        @Test
        @DisplayName("Should parse minimal AASX with data.json and return correct fileType")
        void shouldParseMinimalAasx() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJson(
                    AasXTestHelper.minimalAasJson())) {
                ParseResult result = parser.parse(input);

                assertNotNull(result);
                assertEquals("AASX", result.fileType());
                assertTrue(result.tripleCount() > 0, "Should produce triples from AASX");
                assertNotNull(result.model());
            }
        }

        @Test
        @DisplayName("Should extract CommunicationBinding from AASX data.json")
        void shouldExtractCommunicationBinding() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJson(
                    AasXTestHelper.minimalAasJson())) {
                ParseResult result = parser.parse(input);

                boolean hasBinding = result.details().stream()
                    .anyMatch(d -> d.contains("CommunicationBinding:") && d.contains("protocol=OPC-UA"));
                assertTrue(hasBinding, "Should contain CommunicationBinding with OPC-UA protocol");
            }
        }

        @Test
        @DisplayName("Should extract Skills from AASX data.json")
        void shouldExtractSkills() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJson(
                    AasXTestHelper.minimalAasJson())) {
                ParseResult result = parser.parse(input);

                boolean hasPumping = result.details().stream()
                    .anyMatch(d -> d.contains("Skill:") && d.contains("Pumping"));
                assertTrue(hasPumping, "Should contain Pumping skill from AASX");
            }
        }

        @Test
        @DisplayName("Should extract DataConnector from AASX data.json")
        void shouldExtractDataConnector() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJson(
                    AasXTestHelper.minimalAasJson())) {
                ParseResult result = parser.parse(input);

                boolean hasConnector = result.details().stream()
                    .anyMatch(d -> d.contains("DataConnector:"));
                assertTrue(hasConnector, "Should contain DataConnector from ProcessValues");
            }
        }

        @Test
        @DisplayName("Should parse AASX with data.json in subdirectory (aasx/data.json)")
        void shouldParseSubdirDataJson() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJsonInSubdir(
                    AasXTestHelper.minimalAasJson())) {
                ParseResult result = parser.parse(input);

                assertEquals("AASX", result.fileType());
                assertTrue(result.tripleCount() > 0);
            }
        }

        @Test
        @DisplayName("Should parse AASX with multiple AAS shells")
        void shouldParseMultiShellAasx() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJson(
                    AasXTestHelper.multiShellAasJson())) {
                ParseResult result = parser.parse(input);

                assertEquals("AASX", result.fileType());
                assertTrue(result.tripleCount() > 0);

                boolean hasPumpService = result.details().stream()
                    .anyMatch(d -> d.contains("assetId=") && d.contains("pump"));
                assertTrue(hasPumpService, "Should contain Pump asset identification");

                boolean hasValveService = result.details().stream()
                    .anyMatch(d -> d.contains("assetId=") && d.contains("valve"));
                assertTrue(hasValveService, "Should contain Valve asset identification");
            }
        }

        @Test
        @DisplayName("Should produce same triples as direct JSON parse for equivalent content")
        void shouldProduceEquivalentTriplesToDirectJsonParse() throws Exception {
            String json = AasXTestHelper.minimalAasJson();

            ParseResult jsonResult = aasParser.parse(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

            try (InputStream aasxInput = AasXTestHelper.createAasxWithDataJson(json)) {
                ParseResult aasxResult = parser.parse(aasxInput);

                assertEquals(jsonResult.tripleCount(), aasxResult.tripleCount(),
                    "AASX and direct JSON parse should produce same triple count");
                assertEquals(jsonResult.details().size(), aasxResult.details().size(),
                    "AASX and direct JSON parse should produce same number of details");
            }
        }

        @Test
        @DisplayName("Should parse real TankSimServer.aasx file from resources")
        void shouldParseRealTankSimServerAasx() {
            try (InputStream input = AasXParserTest.class.getClassLoader()
                    .getResourceAsStream("device-descriptions/aas/TankSimServer.aasx")) {
                if (input == null) {
                    fail("Test resource TankSimServer.aasx not found");
                    return;
                }

                ParseResult result = parser.parse(input);

                assertNotNull(result);
                assertEquals("AASX", result.fileType());
                assertTrue(result.tripleCount() > 0, "Real AASX should produce triples");
                assertNotNull(result.model());
                assertFalse(result.details().isEmpty());
            } catch (Exception e) {
                fail("Should not throw: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw ParserException when input stream is null")
        void shouldThrowOnNullInput() {
            assertThrows(ParserException.class, () -> parser.parse(null));
        }

        @Test
        @DisplayName("Should throw ParserException for empty AASX (no entries)")
        void shouldThrowOnEmptyAasx() throws Exception {
            try (InputStream input = AasXTestHelper.createEmptyAasx()) {
                assertThrows(ParserException.class, () -> parser.parse(input));
            }
        }

        @Test
        @DisplayName("Should throw ParserException when AASX has no data.json")
        void shouldThrowWhenNoDataJson() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithoutDataJson()) {
                ParserException ex = assertThrows(ParserException.class, () -> parser.parse(input));
                assertTrue(ex.getMessage().contains("data.json"),
                    "Error should mention data.json");
            }
        }

        @Test
        @DisplayName("Should throw ParserException when AASX contains invalid JSON in data.json")
        void shouldThrowOnInvalidJsonInsideAasx() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJson(
                    "{ not valid json <<<<>>")) {
                assertThrows(ParserException.class, () -> parser.parse(input));
            }
        }

        @Test
        @DisplayName("Should throw ParserException when AASX data.json is empty")
        void shouldThrowOnEmptyDataJson() throws Exception {
            try (InputStream input = AasXTestHelper.createAasxWithDataJson("")) {
                assertThrows(ParserException.class, () -> parser.parse(input));
            }
        }

        @Test
        @DisplayName("Should throw ParserException when data.json is not AAS format")
        void shouldThrowOnNonAasDataJson() throws Exception {
            String nonAasJson = """
                {
                  "EndpointDescriptions": [],
                  "Identification": {}
                }
                """;
            try (InputStream input = AasXTestHelper.createAasxWithDataJson(nonAasJson)) {
                assertThrows(ParserException.class, () -> parser.parse(input));
            }
        }

        @Test
        @DisplayName("Should throw ParserException for non-ZIP content with .aasx extension")
        void shouldThrowOnNonZipContent() {
            String notAZip = "This is just plain text, not a ZIP file";
            InputStream input = new ByteArrayInputStream(notAZip.getBytes(StandardCharsets.UTF_8));
            assertThrows(ParserException.class, () -> parser.parse(input));
        }
    }
}
