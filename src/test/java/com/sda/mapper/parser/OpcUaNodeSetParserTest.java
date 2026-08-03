package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA NodeSet Parser")
class OpcUaNodeSetParserTest {

    private final OpcUaNodeSetParser parser = new OpcUaNodeSetParser();

    @Test
    @DisplayName("Should parse a real OPC UA NodeSet XML file with variables, objects, and methods")
    void shouldParseCompleteNodeSet() {
        InputStream input = getClass().getResourceAsStream("/examples/req001-uaserver-nodeset-complete.xml");
        assertNotNull(input, "Test resource req001-uaserver-nodeset-complete.xml must exist");

        ParseResult result = parser.parse(input);

        assertNotNull(result);
        assertEquals("OPC-UA-NodeSet", result.fileType());
        assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
        assertNotNull(result.model(), "Model must not be null");

        System.out.println("Summary: " + result.summary());
        System.out.println("Triples: " + result.tripleCount());
        System.out.println("Details:");
        result.details().forEach(d -> System.out.println("  " + d));
    }

    @Test
    @DisplayName("Should extract DataConnectors from UAVariable elements")
    void shouldExtractVariablesAsConnectors() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\">\n"
            + "  <UAVariable NodeId=\"ns=1;s=Temp\" BrowseName=\"1:Temp\" DisplayName=\"Temperature\" DataType=\"i=11\">\n"
            + "    <References>\n"
            + "      <Reference ReferenceType=\"HasTypeDefinition\">i=63</Reference>\n"
            + "    </References>\n"
            + "  </UAVariable>\n"
            + "</UANodeSet>";

        ParseResult result = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertTrue(result.tripleCount() > 0);
        assertTrue(result.details().stream().anyMatch(d -> d.contains("DataConnector")));
        assertTrue(result.model().contains(null,
            result.model().createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
            result.model().createResource("https://example.org/ontology/sda#DataConnector")));
    }

    @Test
    @DisplayName("Should extract Skills from UAMethod elements")
    void shouldExtractMethodsAsSkills() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\">\n"
            + "  <UAMethod NodeId=\"ns=1;s=Start\" BrowseName=\"1:Start\" DisplayName=\"Start\">\n"
            + "    <References>\n"
            + "      <Reference ReferenceType=\"HasProperty\">ns=1;s=InputArgs</Reference>\n"
            + "    </References>\n"
            + "  </UAMethod>\n"
            + "</UANodeSet>";

        ParseResult result = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertTrue(result.tripleCount() > 0);
        assertTrue(result.details().stream().anyMatch(d -> d.contains("Skill")));
    }

    @Test
    @DisplayName("Should create CommunicationBinding with OPC-UA protocol")
    void shouldCreateOpcUaBinding() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\">\n"
            + "  <UAVariable NodeId=\"ns=1;s=Val\" BrowseName=\"1:Val\" DisplayName=\"Val\" DataType=\"i=11\" />\n"
            + "</UANodeSet>";

        ParseResult result = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertTrue(result.model().contains(null,
            result.model().createProperty("https://example.org/ontology/sda#protocol"),
            result.model().createTypedLiteral("OPC-UA")));
    }

    @Test
    @DisplayName("Should resolve type aliases in DataType attributes")
    void shouldResolveAliases() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\">\n"
            + "  <Aliases>\n"
            + "    <Alias NodeId=\"i=11\">Double</Alias>\n"
            + "  </Aliases>\n"
            + "  <UAVariable NodeId=\"ns=1;s=Val\" BrowseName=\"1:Val\" DisplayName=\"Val\" DataType=\"Double\" />\n"
            + "</UANodeSet>";

        ParseResult result = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertTrue(result.model().contains(null,
            result.model().createProperty("https://example.org/ontology/sda#dataType"),
            result.model().createTypedLiteral("i=11")));
    }

    @Test
    @DisplayName("Should throw ParserException when input stream is null")
    void shouldThrowOnNullInput() {
        assertThrows(ParserException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("Should throw ParserException when XML is not UANodeSet format")
    void shouldThrowOnNonNodeSetXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<MtpAml xmlns=\"http://www.example.org/mtp-aml\">\n"
            + "  <Service Name=\"Test\" />\n"
            + "</MtpAml>";

        assertThrows(ParserException.class,
            () -> parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("Should throw ParserException on malformed XML")
    void shouldThrowOnMalformedXml() {
        String xml = "this is not xml at all";
        assertThrows(ParserException.class,
            () -> parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("Should handle empty UANodeSet gracefully")
    void shouldHandleEmptyNodeSet() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\">\n"
            + "</UANodeSet>";

        ParseResult result = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("OPC-UA-NodeSet", result.fileType());
        assertTrue(result.details().stream().noneMatch(d -> d.contains("DataConnector")),
            "Empty NodeSet should produce no DataConnector entries");
        assertTrue(result.details().stream().noneMatch(d -> d.contains("Skill")),
            "Empty NodeSet should produce no Skill entries");
    }
}
