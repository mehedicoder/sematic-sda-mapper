package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Canonical parser registry")
class CanonicalParserRegistryTest {

    private final CanonicalParserRegistry registry = new CanonicalParserRegistry(List.of(
        new AasModelParser(), new MtpModelParser(), new OpcUaModelParser()));

    @Test
    @DisplayName("Should find the parser registered for each format")
    void shouldFindParsers() {
        assertEquals("AAS Parser", registry.find(AssetFileFormat.AAS_JSON).format().parserName());
        assertEquals("MTP Parser", registry.find(AssetFileFormat.MTP_AML).format().parserName());
        assertEquals("OPC UA Parser", registry.find(AssetFileFormat.OPCUA_JSON).format().parserName());
    }

    @Test
    @DisplayName("Should throw when no parser is registered for the format")
    void shouldThrowWhenParserMissing() {
        CanonicalParserRegistry empty = new CanonicalParserRegistry(List.of());
        assertThrows(IllegalArgumentException.class, () -> empty.find(AssetFileFormat.AAS_JSON));
    }

    @Test
    @DisplayName("Should route AAS XML to the AAS parser through the shared supports contract")
    void shouldRouteAasXml() {
        CanonicalAssetModel model = assertDoesNotThrow(() -> registry.parse(
            new ByteArrayInputStream("<assetAdministrationShell><idShort>Pump</idShort></assetAdministrationShell>"
                .getBytes(StandardCharsets.UTF_8)),
            "pump.xml", AssetFileFormat.AAS_XML));
        assertNotNull(model);
        assertEquals(1, model.assets().size());
    }
}
