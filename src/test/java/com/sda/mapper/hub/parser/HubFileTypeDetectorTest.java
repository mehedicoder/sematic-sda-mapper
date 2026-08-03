package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Hub file type detector")
class HubFileTypeDetectorTest {

    private final HubFileTypeDetector detector = new HubFileTypeDetector();

    private Optional<AssetFileFormat> detect(String content) {
        return detector.detect("upload.bin",
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Should sniff AAS JSON by assetAdministrationShells key")
    void shouldSniffAasJson() {
        assertEquals(Optional.of(AssetFileFormat.AAS_JSON),
            detect("{\"assetAdministrationShells\":[]}"));
    }

    @Test
    @DisplayName("Should sniff AAS XML by idShort plus assetAdministrationShell token")
    void shouldSniffAasXml() {
        assertEquals(Optional.of(AssetFileFormat.AAS_XML),
            detect("<assetAdministrationShell idShort=\"Pump\"/>"));
    }

    @Test
    @DisplayName("Should sniff MTP AML by MtpAml root token")
    void shouldSniffMtpAml() {
        assertEquals(Optional.of(AssetFileFormat.MTP_AML),
            detect("<MtpAml><Instance/></MtpAml>"));
    }

    @Test
    @DisplayName("Should sniff OPC UA JSON by EndpointDescriptions key")
    void shouldSniffOpcUaJson() {
        assertEquals(Optional.of(AssetFileFormat.OPCUA_JSON),
            detect("{\"EndpointDescriptions\":[]}"));
    }

    @Test
    @DisplayName("Should fall back to the .aml extension when content is inconclusive")
    void shouldFallBackToExtension() {
        assertEquals(Optional.of(AssetFileFormat.MTP_AML),
            detector.detect("module.aml", new ByteArrayInputStream(new byte[0])));
    }

    @Test
    @DisplayName("Should return empty for unknown extensions and content")
    void shouldReturnEmptyForUnknown() {
        assertTrue(detector.detect("notes.txt",
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))).isEmpty());
        assertTrue(detector.detect("module.xml",
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))).isEmpty());
        assertTrue(detector.detectByFileName(null).isEmpty());
    }

    @Test
    @DisplayName("Should not throw on null input content")
    void shouldHandleNullInput() {
        assertTrue(detector.detectByContent(null).isEmpty());
    }
}
