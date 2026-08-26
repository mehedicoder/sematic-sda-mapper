package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.service.OntologyDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the raw ABox TTL parser used to ingest service catalogue,
 * asset and edge-node Turtle files.
 */
@DisplayName("ABox TTL parser")
class AboxTtlParserTest {

    private final AboxTtlParser parser = new AboxTtlParser(new OntologyDataService());

    @Test
    @DisplayName("Should report the ABOX_TTL format")
    void shouldReportFormat() {
        assertEquals(AssetFileFormat.ABOX_TTL, parser.format());
    }

    @Test
    @DisplayName("Should project ABox individuals onto the canonical model with their "
        + "canonical (parent) class, not the ABox subclass")
    void shouldProjectIndividualsToCanonicalClasses() {
        String ttl = "@prefix : <http://sda-platform.org/ontology#> .\n"
            + "@prefix ex: <http://example.org#> .\n"
            + "ex:Service_Diagnostics_v1 a :AnalyticsService ;\n"
            + "  :serviceID \"SVC-1\" ;\n"
            + "  :serviceName \"NE107 Service\" ;\n"
            + "  :serviceType \"Analytics\" .\n"
            + "ex:OCap_AssetConditionMonitoring a :OfferedCapability ;\n"
            + "  :capabilityID \"OCAP-1\" ;\n"
            + "  :capabilityName \"AssetConditionMonitoring\" .\n"
            + "ex:Flowmeter_TankFill_01 a :Asset ;\n"
            + "  :assetID \"AST-1\" ;\n"
            + "  :assetName \"Tank Flowmeter\" .\n";

        CanonicalAssetModel model = parser.parse(
            new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8)), "sample.ttl");

        assertEquals(1, model.getEntitiesForClass("Service").size());
        assertEquals("NE107 Service", model.getEntitiesForClass("Service").get(0).name());
        assertEquals(1, model.getEntitiesForClass("Capability").size());
        assertEquals("AssetConditionMonitoring",
            model.getEntitiesForClass("Capability").get(0).name());
        assertEquals(1, model.getEntitiesForClass("Asset").size());
        assertEquals("Tank Flowmeter", model.getEntitiesForClass("Asset").get(0).name());

        // Subclass type names must not appear under their own node
        assertEquals(0, model.getEntitiesForClass("AnalyticsService").size());
        assertEquals(0, model.getEntitiesForClass("OfferedCapability").size());

        // The projected entities must be concrete typed records, not ManualEntity
        CanonicalEntity service = model.getEntitiesForClass("Service").get(0);
        assertEquals("Service", service.className());
        assertFalse(service.sourceFileName().isEmpty());
    }

    @Test
    @DisplayName("Should record the originating upload file as entity provenance")
    void shouldRecordSourceFileName() {
        String ttl = "@prefix : <http://sda-platform.org/ontology#> .\n"
            + "@prefix ex: <http://example.org#> .\n"
            + "ex:Flowmeter_TankFill_01 a :Asset ;\n"
            + "  :assetID \"AST-1\" ;\n"
            + "  :assetName \"Tank Flowmeter\" .\n";
        CanonicalAssetModel model = parser.parse(
            new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8)), "assets.ttl");
        assertEquals("assets.ttl",
            model.getEntitiesForClass("Asset").get(0).sourceFileName().orElse(""));
    }
}
