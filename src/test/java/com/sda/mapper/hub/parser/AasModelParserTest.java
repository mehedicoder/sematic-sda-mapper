package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.Observation;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.hub.model.UseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AAS model parser")
class AasModelParserTest {

    private final AasModelParser parser = new AasModelParser();

    private InputStream fixture() {
        InputStream stream = getClass().getResourceAsStream("/device-descriptions/aas/pump-aas.json");
        if (stream == null) {
            throw new IllegalStateException("Fixture pump-aas.json not found");
        }
        return stream;
    }

    @Test
    @DisplayName("Should report the handled formats")
    void shouldHandleAasFormats() {
        assertEquals(AssetFileFormat.AAS_JSON, parser.format());
        assertTrue(parser.supports(AssetFileFormat.AAS_JSON));
        assertTrue(parser.supports(AssetFileFormat.AAS_XML));
        assertFalse(parser.supports(AssetFileFormat.MTP_AML));
    }

    @Test
    @DisplayName("Should parse the AAS JSON pump fixture into canonical entities")
    void shouldParseAasJsonFixture() throws IOException {
        CanonicalAssetModel model;
        try (InputStream in = fixture()) {
            model = parser.parse(in, "pump-aas.json");
        }
        assertEquals("PumpModule-FT101", model.assets().get(0).name());
        assertEquals(1, model.communicationBindings().size());
        CommunicationBinding binding = model.communicationBindings().get(0);
        assertEquals("opc.tcp://pump01:4840", binding.endpointUrl());
        assertEquals(2, model.skills().size());
        assertTrue(model.skills().stream().anyMatch(s -> s.name().equals("Pumping")));
        assertEquals(4, model.processVariables().size());
        assertEquals(4, model.observations().size());
        Observation first = model.observations().get(0);
        assertTrue(first.processVariableId().contains("FlowRate"));
        assertEquals(3, model.useCases().size());
        assertTrue(model.useCases().stream().anyMatch(u -> u.name().equals("StartPump")));
        assertTrue(model.allEntities().stream()
            .anyMatch(e -> e.className().equals("SafetyClass")));
        assertTrue(model.allEntities().stream()
            .anyMatch(e -> e.className().equals("DataConnector")));
        assertTrue(model.allEntities().stream()
            .anyMatch(e -> e.className().equals("SemanticConcept")));
        assertTrue(model.allEntities().stream()
            .anyMatch(e -> e.className().equals("Service")));
        assertTrue(model.allEntities().stream()
            .allMatch(e -> e.sourceFileName().orElse("").equals("pump-aas.json")));
    }

    @Test
    @DisplayName("Should reject invalid JSON with a ParsingException")
    void shouldRejectInvalidJson() {
        ParsingException exception = assertThrows(ParsingException.class, () ->
            parser.parse(new java.io.ByteArrayInputStream(
                "{not json".getBytes(StandardCharsets.UTF_8)), "bad.json"));
        assertTrue(exception.getMessage().contains("bad.json"));
    }

    @Test
    @DisplayName("Should reject JSON without administration shells")
    void shouldRejectMissingShells() {
        assertThrows(ParsingException.class, () -> parser.parse(
            new java.io.ByteArrayInputStream(
                "{\"submodels\":[]}".getBytes(StandardCharsets.UTF_8)), "empty.json"));
    }

    @Test
    @DisplayName("Should reject empty documents")
    void shouldRejectEmpty() {
        assertThrows(ParsingException.class, () -> parser.parse(
            new java.io.ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), "empty.json"));
    }

    @Test
    @DisplayName("Should reject a null input stream")
    void shouldRejectNullStream() {
        assertThrows(ParsingException.class, () -> parser.parse(null, "null.json"));
    }
}
