package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Server Datasheet Parser")
class UaServerDatasheetParserTest {

    private final UaServerDatasheetParser parser = new UaServerDatasheetParser();

    private static InputStream resource(String name) {
        return UaServerDatasheetParserTest.class.getClassLoader()
            .getResourceAsStream("examples/" + name);
    }

    @Test
    @DisplayName("Should parse valid datasheet and return ParseResult with correct triple count")
    void shouldParseValidDatasheet() {
        try (InputStream input = resource("req003-uaserver-ahu01.json")) {
            assertNotNull(input, "Test resource req003-uaserver-ahu01.json must exist");

            ParseResult result = parser.parse(input);

            assertNotNull(result);
            assertEquals("OPC-UA-Datasheet", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
            assertFalse(result.details().isEmpty(), "Should have detail entries");
            assertNotNull(result.summary());
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should produce CommunicationBinding with endpoint")
    void shouldProduceCommunicationBindingWithEndpoint() {
        try (InputStream input = resource("req003-uaserver-ahu01.json")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            boolean hasBinding = result.details().stream()
                .anyMatch(d -> d.contains("CommunicationBinding:") && d.contains("opc.tcp://ahu01.local:4840"));
            assertTrue(hasBinding, "Should contain CommunicationBinding with OPC UA endpoint");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing EndpointDescriptions gracefully")
    void shouldHandleMissingEndpointDescriptions() {
        String json = """
            {
              "Configuration": {
                "NodeSets": [
                  { "id": "ns=2;s=Test.Node1" }
                ]
              },
              "Identification": {
                "productUri": "urn:test:device"
              }
            }
            """;
        InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        ParseResult result = parser.parse(input);

        assertNotNull(result);
        assertEquals("OPC-UA-Datasheet", result.fileType());
        assertTrue(result.tripleCount() >= 4, "Should count node set + identification triples");
    }

    @Test
    @DisplayName("Should throw ParserException on invalid JSON")
    void shouldThrowOnInvalidJson() {
        String invalid = "{ not valid json {{{";
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
    @DisplayName("Should produce DataConnector triples from NodeSets")
    void shouldProduceDataConnectorsFromNodeSets() {
        try (InputStream input = resource("req003-uaserver-ahu01.json")) {
            assertNotNull(input);

            ParseResult result = parser.parse(input);

            long connectorCount = result.details().stream()
                .filter(d -> d.contains("DataConnector:"))
                .count();
            assertEquals(2, connectorCount, "Should produce 2 DataConnectors from 2 NodeSets");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
