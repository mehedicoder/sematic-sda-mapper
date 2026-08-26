package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.AutomationMLEntity;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AutomationML (CAEX) model parser")
class AutomationMlParserTest {

    private final AutomationMlParser parser = new AutomationMlParser();

    private InputStream fixture() {
        InputStream stream = getClass().getResourceAsStream(
            "/device-descriptions/reference-formats/sample-caex.aml");
        if (stream == null) {
            throw new IllegalStateException("Fixture sample-caex.aml not found");
        }
        return stream;
    }

    @Test
    @DisplayName("Should report the handled format")
    void shouldHandleAutomationMl() {
        assertEquals(AssetFileFormat.AUTOMATION_ML, parser.format());
        assertTrue(parser.supports(AssetFileFormat.AUTOMATION_ML));
    }

    @Test
    @DisplayName("Should parse a CAEX description into InternalElement and ExternalInterface entities")
    void shouldParseCaexFixture() throws Exception {
        CanonicalAssetModel model;
        try (InputStream in = fixture()) {
            model = parser.parse(in, "sample-caex.aml");
        }
        assertEquals(2, model.allEntities().size());
        assertTrue(model.allEntities().stream()
            .anyMatch(e -> e instanceof AutomationMLEntity a
                && "InternalElement".equals(a.attributes().get("caexElementType"))
                && a.name().equals("PumpSkid")));
        assertTrue(model.allEntities().stream()
            .anyMatch(e -> e instanceof AutomationMLEntity a
                && "ExternalInterface".equals(a.attributes().get("caexElementType"))
                && a.name().equals("OPCUA_Server")));
        AutomationMLEntity pump = model.allEntities().stream()
            .filter(e -> e instanceof AutomationMLEntity a && a.name().equals("PumpSkid"))
            .map(e -> (AutomationMLEntity) e)
            .findFirst().orElseThrow();
        assertEquals("ie-pump", pump.id());
        assertEquals("AutomationML", pump.className());
        assertEquals("Plus/Function", pump.attributes().get("RefRoleClassPath"));
        assertEquals("5.5", pump.attributes().get("Power"));
    }

    @Test
    @DisplayName("Should reject a non-CAEX root element")
    void shouldRejectNonCaexRoot() {
        ParsingException exception = assertThrows(ParsingException.class, () ->
            parser.parse(new java.io.ByteArrayInputStream(
                "<NotCaex/>".getBytes(StandardCharsets.UTF_8)), "wrong.xml"));
        assertTrue(exception.getMessage().contains("wrong.xml"));
    }

    @Test
    @DisplayName("Should reject malformed XML")
    void shouldRejectMalformedXml() {
        assertThrows(ParsingException.class, () ->
            parser.parse(new java.io.ByteArrayInputStream(
                "not xml at all <<<".getBytes(StandardCharsets.UTF_8)), "broken.xml"));
    }
}
