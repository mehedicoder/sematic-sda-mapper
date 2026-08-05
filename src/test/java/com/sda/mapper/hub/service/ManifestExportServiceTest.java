package com.sda.mapper.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.service.OntologyDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Manifest export service")
class ManifestExportServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ManifestExportService createService() {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        return new ManifestExportService(matchingService, recommendationService);
    }

    @Test
    @DisplayName("Should export a JSON manifest with metadata and coverage")
    void shouldExportManifestMetadata() throws Exception {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance",
            "pump-aas.json"));

        String json = createService().build(model, List.of(),
            List.of(new UploadedFileInfo("pump-aas.json", AssetFileFormat.AAS_JSON, 42)),
            Set.of(AssetFileFormat.AAS_JSON));

        JsonNode root = mapper.readTree(json);
        assertEquals("1.0", root.path("manifestVersion").asText());
        assertTrue(root.path("generatedAt").asText().length() > 0);
        assertTrue(root.path("coveragePercent").asDouble() > 0);
        assertEquals("pump-aas.json (detected: AAS JSON)",
            root.path("files").get(0).asText());
        assertEquals("PRESENT", root.path("coverage").path("Asset").path("status").asText());
    }

    @Test
    @DisplayName("Should group entities by ontology class in the manifest")
    void shouldGroupEntitiesByClass() throws Exception {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance"));

        String json = createService().build(model, List.of(), List.of(), Set.of());

        JsonNode root = mapper.readTree(json);
        JsonNode asset = root.path("entities").path("Asset").get(0);
        assertEquals("Pump FT101", asset.path("name").asText());
        assertTrue(asset.path("inferred").isBoolean());
    }

    @Test
    @DisplayName("Should include blocking gaps and recommendations arrays")
    void shouldIncludeGapsAndRecommendations() throws Exception {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance"));

        String json = createService().build(model, List.of(), List.of(), Set.of());

        JsonNode root = mapper.readTree(json);
        assertTrue(root.path("blockingGaps").isArray());
        assertTrue(root.path("recommendations").isArray());
    }

    @Test
    @DisplayName("Should include per-layer coverage in the manifest")
    void shouldIncludeLayerCoverage() throws Exception {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance"));

        String json = createService().build(model, List.of(), List.of(), Set.of());

        JsonNode root = mapper.readTree(json);
        JsonNode layerCoverage = root.path("layerCoverage");
        assertTrue(layerCoverage.isArray());
        assertEquals(5, layerCoverage.size());
        JsonNode l4 = null;
        for (JsonNode node : layerCoverage) {
            if ("L4".equals(node.path("layer").asText())) {
                l4 = node;
            }
        }
        assertNotNull(l4, "Manifest must contain L4 layer coverage");
        assertEquals(25.0, l4.path("coveragePercent").asDouble());
        assertEquals(1, l4.path("present").asInt());
        assertEquals(3, l4.path("missing").asInt());
    }
}
