package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.SemanticConcept;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OPC UA model parser")
class OpcUaModelParserTest {

    private final OpcUaModelParser parser = new OpcUaModelParser();

    private InputStream fixture() {
        InputStream stream = getClass().getResourceAsStream("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-opcua-datasheet.json");
        if (stream == null) {
            throw new IllegalStateException("Fixture pump-opcua-datasheet.json not found");
        }
        return stream;
    }

    @Test
    @DisplayName("Should report the handled format")
    void shouldHandleOpcUa() {
        assertEquals(AssetFileFormat.OPCUA_JSON, parser.format());
        assertTrue(parser.supports(AssetFileFormat.OPCUA_JSON));
    }

    @Test
    @DisplayName("Should parse the OPC UA pump fixture into asset, binding, variables and concepts")
    void shouldParseOpcUaFixture() throws Exception {
        CanonicalAssetModel model;
        try (InputStream in = fixture()) {
            model = parser.parse(in, "pump-opcua-datasheet.json");
        }
        assertEquals(1, model.assets().size());
        assertEquals("Siemens Process Automation", model.assets().get(0).name());
        assertEquals(1, model.communicationBindings().size());
        CommunicationBinding binding = model.communicationBindings().get(0);
        assertEquals("opc.tcp://pump01:4840", binding.endpointUrl());
        assertEquals(4, model.processVariables().size());
        assertEquals(1, model.semanticConcepts().size());
        SemanticConcept concept = model.semanticConcepts().get(0);
        assertEquals("Pump01", concept.name());
        assertEquals(7, model.allEntities().size());
    }

    @Test
    @DisplayName("Should reject JSON that is not an OPC UA datasheet")
    void shouldRejectNonDatasheet() {
        ParsingException exception = assertThrows(ParsingException.class, () -> parser.parse(
            new java.io.ByteArrayInputStream(
                "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8)), "unknown.json"));
        assertTrue(exception.getMessage().contains("unknown.json"));
    }

    @Test
    @DisplayName("Should reject malformed JSON")
    void shouldRejectMalformedJson() {
        assertThrows(ParsingException.class, () -> parser.parse(
            new java.io.ByteArrayInputStream(
                "not json".getBytes(StandardCharsets.UTF_8)), "bad.json"));
    }

    @Test
    @DisplayName("Should reject a null input stream")
    void shouldRejectNullStream() {
        assertThrows(ParsingException.class, () -> parser.parse(null, "null.json"));
    }
}
