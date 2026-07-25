package com.sda.mapper.imports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("File Type Classifier")
class FileTypeClassifierTest {

    private final FileTypeClassifier classifier = new FileTypeClassifier();

    @Test
    @DisplayName("Should classify TTL files as RDF_TURTLE")
    void shouldClassifyTurtleFiles() {
        assertEquals(FileType.RDF_TURTLE, classifier.classify("ontology.ttl", null));
        assertEquals(FileType.RDF_TURTLE, classifier.classify("model.turtle", null));
    }

    @Test
    @DisplayName("Should classify JSON-LD files as JSON_LD")
    void shouldClassifyJsonLdFiles() {
        assertEquals(FileType.JSON_LD, classifier.classify("data.jsonld", null));
        assertEquals(FileType.JSON_LD, classifier.classify("data.json-ld", null));
    }

    @Test
    @DisplayName("Should classify YAML files as YAML")
    void shouldClassifyYamlFiles() {
        assertEquals(FileType.YAML, classifier.classify("config.yaml", null));
        assertEquals(FileType.YAML, classifier.classify("config.yml", null));
    }

    @Test
    @DisplayName("Should classify AML files as MTP_AML")
    void shouldClassifyAmlFiles() {
        assertEquals(FileType.MTP_AML, classifier.classify("service.aml", null));
    }

    @Test
    @DisplayName("Should classify XML files as MTP_AML by default")
    void shouldClassifyXmlFilesAsMtpAml() {
        assertEquals(FileType.MTP_AML, classifier.classify("service.xml", null));
    }

    @Test
    @DisplayName("Should classify OPC UA NodeSet XML via content sniffing")
    void shouldClassifyNodeSetXml() {
        String content = "<?xml version=\"1.0\"?><UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\"></UANodeSet>";
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        assertEquals(FileType.OPC_UA_NODESET, classifier.classify("nodeset.xml", input));
    }

    @Test
    @DisplayName("Should classify AAS JSON via content sniffing")
    void shouldClassifyAasJson() {
        String content = "{\"assetAdministrationShells\": []}";
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        assertEquals(FileType.AAS, classifier.classify("aas.json", input));
    }

    @Test
    @DisplayName("Should classify AASX files by extension without content sniffing")
    void shouldClassifyAasxFiles() {
        assertEquals(FileType.AASX, classifier.classify("device.aasx", null));
        assertEquals(FileType.AASX, classifier.classify("TankSimServer.aasx", null));
        assertEquals(FileType.AASX, classifier.classify("MY-DEVICE.AASX", null));
    }

    @Test
    @DisplayName("Should classify OPC UA Datasheet JSON as default for JSON")
    void shouldClassifyOpcUaDatasheet() {
        String content = "{\"endpointDescriptions\": []}";
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        assertEquals(FileType.OPC_UA_DATASHEET, classifier.classify("datasheet.json", input));
    }

    @Test
    @DisplayName("Should return UNKNOWN for null or blank filename")
    void shouldReturnUnknownForNullFilename() {
        assertEquals(FileType.UNKNOWN, classifier.classify(null, null));
        assertEquals(FileType.UNKNOWN, classifier.classify("", null));
        assertEquals(FileType.UNKNOWN, classifier.classify("  ", null));
    }

    @Test
    @DisplayName("Should return UNKNOWN for unsupported extensions")
    void shouldReturnUnknownForUnsupportedExtensions() {
        assertEquals(FileType.UNKNOWN, classifier.classify("readme.txt", null));
        assertEquals(FileType.UNKNOWN, classifier.classify("image.png", null));
    }
}
