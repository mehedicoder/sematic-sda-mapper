package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parser Factory")
class ParserFactoryTest {

    private final AasParser aasParser = new AasParser();
    private final ParserFactory factory = new ParserFactory(
        new MTPAmlParser(), new UaServerDatasheetParser(), new OpcUaNodeSetParser(),
        aasParser, new AasXParser(aasParser));

    @Test
    @DisplayName("Should return MTPAmlParser for .xml extension via extension-only lookup")
    void shouldReturnMtpAmlParserForXml() {
        Parser parser = factory.getParser("test-file.xml");
        assertInstanceOf(MTPAmlParser.class, parser);
    }

    @Test
    @DisplayName("Should return MTPAmlParser for .aml extension")
    void shouldReturnMtpAmlParserForAml() {
        Parser parser = factory.getParser("test-file.aml");
        assertInstanceOf(MTPAmlParser.class, parser);
    }

    @Test
    @DisplayName("Should return UaServerDatasheetParser for .json extension")
    void shouldReturnUaServerDatasheetParserForJson() {
        Parser parser = factory.getParser("test-file.json");
        assertInstanceOf(UaServerDatasheetParser.class, parser);
    }

    @Test
    @DisplayName("Should throw ParserException for unknown extension")
    void shouldThrowForUnknownExtension() {
        assertThrows(ParserException.class, () -> factory.getParser("test-file.txt"));
        assertThrows(ParserException.class, () -> factory.getParser("test-file.csv"));
        assertThrows(ParserException.class, () -> factory.getParser("test-file.zip"));
    }

    @Test
    @DisplayName("Should return OpcUaNodeSetParser for XML containing UANodeSet root element")
    void shouldReturnNodeSetParserForUANodeSet() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\">\n"
            + "  <NamespaceUris><Uri>urn:test:namespace</Uri></NamespaceUris>\n"
            + "</UANodeSet>";
        InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Parser parser = factory.getParser("test-file.xml", input);
        assertInstanceOf(OpcUaNodeSetParser.class, parser);
    }

    @Test
    @DisplayName("Should return MTPAmlParser for XML without UANodeSet root element")
    void shouldReturnMtpAmlParserForNonNodeSetXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<MtpAml xmlns=\"http://www.example.org/mtp-aml\">\n"
            + "  <Instance Name=\"Test\">\n"
            + "    <Service Name=\"TestService\" />\n"
            + "  </Instance>\n"
            + "</MtpAml>";
        InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Parser parser = factory.getParser("test-file.xml", input);
        assertInstanceOf(MTPAmlParser.class, parser);
    }

    @Test
    @DisplayName("Should return AasParser for JSON containing assetAdministrationShells key")
    void shouldReturnAasParserForAasJson() {
        String json = "{\n"
            + "  \"assetAdministrationShells\": [\n"
            + "    {\n"
            + "      \"idShort\": \"TestAAS\",\n"
            + "      \"id\": { \"idType\": \"IRI\", \"identifier\": \"https://example.org/aas/test\" }\n"
            + "    }\n"
            + "  ]\n"
            + "}";
        InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Parser parser = factory.getParser("test-file.json", input);
        assertInstanceOf(AasParser.class, parser);
    }

    @Test
    @DisplayName("Should return UaServerDatasheetParser for JSON without assetAdministrationShells")
    void shouldReturnDatasheetParserForNonAasJson() {
        String json = "{\n"
            + "  \"EndpointDescriptions\": [\n"
            + "    { \"uri\": \"opc.tcp://localhost:4840\" }\n"
            + "  ]\n"
            + "}";
        InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Parser parser = factory.getParser("test-file.json", input);
        assertInstanceOf(UaServerDatasheetParser.class, parser);
    }

    @Test
    @DisplayName("Should return AasXParser for .aasx extension via extension-only lookup")
    void shouldReturnAasXParserForAasx() {
        Parser parser = factory.getParser("device.aasx");
        assertInstanceOf(AasXParser.class, parser);
    }

    @Test
    @DisplayName("Should return AasXParser for .AASX (uppercase) extension")
    void shouldReturnAasXParserForUppercaseAasx() {
        Parser parser = factory.getParser("DEVICE.AASX");
        assertInstanceOf(AasXParser.class, parser);
    }

    @Test
    @DisplayName("Should return AasXParser for .aasx extension with content stream")
    void shouldReturnAasXParserForAasxWithContent() {
        InputStream input = new ByteArrayInputStream(new byte[0]);
        Parser parser = factory.getParser("device.aasx", input);
        assertInstanceOf(AasXParser.class, parser);
    }
}
