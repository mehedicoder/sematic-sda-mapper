package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AAS Parser")
class AasParserTest {

    private final AasParser parser = new AasParser();

    private static InputStream resource(String path) {
        return AasParserTest.class.getClassLoader()
            .getResourceAsStream(path);
    }

    @Test
    @DisplayName("Should parse valid AAS JSON and return ParseResult with correct triple count")
    void shouldParseValidAasJson() {
        try (InputStream input = resource("device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            assertNotNull(input, "Test resource pump-aas.json must exist");

            ParseResult result = parser.parse(input);

            assertNotNull(result);
            assertEquals("AAS", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
            assertFalse(result.details().isEmpty(), "Should have detail entries");
            assertNotNull(result.summary());
            assertNotNull(result.model(), "Model must not be null");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should extract CommunicationBinding from Communication submodel")
    void shouldExtractCommunicationBinding() {
        try (InputStream input = resource("device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            boolean hasBinding = result.details().stream()
                .anyMatch(d -> d.contains("CommunicationBinding:") && d.contains("protocol=OPC-UA"));
            assertTrue(hasBinding, "Should contain CommunicationBinding with OPC-UA protocol");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should extract Skills from Skills submodel")
    void shouldExtractSkills() {
        try (InputStream input = resource("device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            boolean hasPumpingSkill = result.details().stream()
                .anyMatch(d -> d.contains("Skill:") && d.contains("Pumping"));
            assertTrue(hasPumpingSkill, "Should contain Pumping skill");

            boolean hasFlowMonitoringSkill = result.details().stream()
                .anyMatch(d -> d.contains("Skill:") && d.contains("FlowMonitoring"));
            assertTrue(hasFlowMonitoringSkill, "Should contain FlowMonitoring skill");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should create Skill resources with sda:Skill rdf:type")
    void shouldCreateCssSkillType() {
        try (InputStream input = resource("device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);
            Model model = result.model();

            Resource skillType = model.createResource("http://sda-platform.org/ontology#Skill");
            boolean hasSkillType = model.listSubjectsWithProperty(
                model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                skillType).hasNext();
            assertTrue(hasSkillType, "Parsed Skills should have sda:Skill rdf:type");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should extract engineering unit from Skills submodel")
    void shouldExtractEngineeringUnit() {
        try (InputStream input = resource("device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            boolean hasSkillWithUnit = result.details().stream()
                .anyMatch(d -> d.contains("Skill:") && d.contains("unit=l/min"));
            assertTrue(hasSkillWithUnit, "Should contain Skill with engineering unit l/min");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should extract DataConnector from ProcessValues submodel")
    void shouldExtractDataConnector() {
        try (InputStream input = resource("device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            boolean hasConnector = result.details().stream()
                .anyMatch(d -> d.contains("DataConnector:"));
            assertTrue(hasConnector, "Should contain DataConnector from ProcessValues");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should parse second AAS file (motor-aas.json)")
    void shouldParseMotorAas() {
        try (InputStream input = resource("device-descriptions/reference-formats/motor-aas.json")) {
            assertNotNull(input, "Test resource motor-aas.json must exist");

            ParseResult result = parser.parse(input);

            assertNotNull(result);
            assertEquals("AAS", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw ParserException on invalid JSON")
    void shouldThrowOnInvalidJson() {
        String invalid = "{ not valid json <<<<>>>";
        InputStream input = new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8));

        assertThrows(ParserException.class, () -> parser.parse(input));
    }

    @Test
    @DisplayName("Should throw ParserException on empty input")
    void shouldThrowOnEmptyInput() {
        InputStream input = new ByteArrayInputStream(new byte[0]);

        assertThrows(ParserException.class, () -> parser.parse(input));
    }

    @Test
    @DisplayName("Should throw ParserException when assetAdministrationShells is missing")
    void shouldThrowOnNonAasJson() {
        String json = """
            {
              "EndpointDescriptions": [],
              "Identification": {}
            }
            """;
        InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        assertThrows(ParserException.class, () -> parser.parse(input));
    }

    @Test
    @DisplayName("Should throw ParserException when input stream is null")
    void shouldThrowOnNullInput() {
        assertThrows(ParserException.class, () -> parser.parse(null));
    }
}
