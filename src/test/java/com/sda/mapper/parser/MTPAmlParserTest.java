package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MTP AML Parser")
class MTPAmlParserTest {

    private final MTPAmlParser parser = new MTPAmlParser();

    private static InputStream resource(String name) {
        return MTPAmlParserTest.class.getClassLoader()
            .getResourceAsStream("examples/" + name);
    }

    @Test
    @DisplayName("Should parse valid MTP AML and return ParseResult with correct triple count")
    void shouldParseValidMtpAml() {
        try (InputStream input = resource("req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input, "Test resource req002-mtp-fermentation-skid.xml must exist");

            ParseResult result = parser.parse(input);

            assertNotNull(result);
            assertEquals("MTP-AML", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
            assertFalse(result.details().isEmpty(), "Should have detail entries");
            assertNotNull(result.summary());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should parse Skill instances with engineering unit")
    void shouldParseSkillWithEngineeringUnit() {
        try (InputStream input = resource("req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            boolean hasSkillWithUnit = result.details().stream()
                .anyMatch(d -> d.contains("Skill:") && d.contains("unit=") && !d.contains("unit=)"));
            assertTrue(hasSkillWithUnit, "Should contain Skill with a non-empty engineering unit");

            boolean hasTempSkill = result.details().stream()
                .anyMatch(d -> d.contains("Skill:") && d.contains("FermentationControl"));
            assertTrue(hasTempSkill, "Should contain Skill named FermentationControl");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing Service elements gracefully")
    void shouldHandleMissingServiceElements() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <MtpAml xmlns="http://www.example.org/mtp-aml">
                <Service Name="EmptyService">
                </Service>
            </MtpAml>
            """;
        InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        ParseResult result = parser.parse(input);

        assertNotNull(result);
        assertEquals("MTP-AML", result.fileType());
        assertEquals(2, result.tripleCount(), "One service with rdf:type + label = 2 triples");
    }

    @Test
    @DisplayName("Should throw ParserException on invalid XML")
    void shouldThrowOnInvalidXml() {
        String invalid = "<not valid xml <<<<>>>";
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
    @DisplayName("Should produce CommunicationBinding triples with protocol OPC-UA")
    void shouldProduceCommunicationBindingWithProtocol() {
        try (InputStream input = resource("req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            boolean hasBinding = result.details().stream()
                .anyMatch(d -> d.contains("CommunicationBinding:") && d.contains("protocol=OPC-UA"));
            assertTrue(hasBinding, "Should contain CommunicationBinding with protocol OPC-UA");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
