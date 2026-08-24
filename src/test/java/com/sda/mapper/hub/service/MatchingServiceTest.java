package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.BlockingGap;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.Capability;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.LayerCoverage;
import com.sda.mapper.hub.model.Sensor;
import com.sda.mapper.model.OntologyDatatypeProperty;
import com.sda.mapper.service.OntologyDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Matching stage service")
class MatchingServiceTest {

    private final MatchingService service = new MatchingService(new OntologyDataService());

    @Test
    @DisplayName("Should fail construction when a required attribute is missing from the ontology")
    void shouldFailWhenRequiredAttributeNotDeclared() {
        OntologyDataService stub = new OntologyDataService() {
            @Override
            public List<OntologyDatatypeProperty> getDatatypeProperties(String className) {
                return List.of();
            }
        };
        assertThrows(IllegalStateException.class, () -> new MatchingService(stub),
            "Required attributes must be validated against the ontology at construction");
    }

    @Test
    @DisplayName("Should report all 24 classes as missing on an empty model")
    void shouldReportAllMissingForEmptyModel() {
        GapReport report = service.compute(new CanonicalAssetModel(), List.of());
        assertEquals(24, report.entries().size());
        assertEquals(24, report.count(CoverageStatus.MISSING));
        assertEquals(0.0, report.coveragePercent());
        assertTrue(report.blockingGaps().isEmpty(),
            "No core relation can be blocked when nothing is covered");
    }

    @Test
    @DisplayName("Should report a fully populated class as present")
    void shouldReportPresentForCompleteEntity() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance"));

        GapReport report = service.compute(model, List.of());

        assertEquals(CoverageStatus.PRESENT, report.forClass("Asset").orElseThrow().status());
    }

    @Test
    @DisplayName("Should report a class with missing required fields as partial")
    void shouldReportPartialForIncompleteEntity() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new CommunicationBinding("cb-1", "OPC UA binding", "OPC-UA",
            "", "None", ""));

        GapReport report = service.compute(model, List.of());

        CoverageEntry entry = report.forClass("CommunicationBinding").orElseThrow();
        assertEquals(CoverageStatus.PARTIAL, entry.status());
        assertTrue(entry.missingFields().contains("endpointUrl"));
    }

    @Test
    @DisplayName("Should report a class covered only by inference as inferred")
    void shouldReportInferredStatus() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        List<CanonicalEntity> inferred = List.<CanonicalEntity>of(
            new Sensor("SENSOR-flow", "FlowRate Sensor", "Pump01", "l/min"));

        GapReport report = service.compute(model, inferred);

        CoverageEntry entry = report.forClass("Sensor").orElseThrow();
        assertEquals(CoverageStatus.INFERRED, entry.status());
        assertTrue(report.count(CoverageStatus.INFERRED) >= 1);
    }

    @Test
    @DisplayName("Should compute the weighted coverage percentage")
    void shouldComputeWeightedCoverage() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Capability("cap-1", "Pumping", "Pumping capability"));
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance"));

        GapReport report = service.compute(model, List.of());

        assertEquals(8.3, report.coveragePercent());
    }

    @Test
    @DisplayName("Should report a blocking gap for a core relation to a missing class")
    void shouldReportBlockingGap() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Capability("cap-1", "Pumping", "Pumping capability"));

        GapReport report = service.compute(model, List.of());

        assertFalse(report.blockingGaps().isEmpty());
        BlockingGap gap = report.blockingGaps().stream()
            .filter(g -> g.className().equals("Service")).findFirst().orElseThrow();
        assertTrue(gap.reasons().stream().anyMatch(r -> r.contains("Capability")));
    }

    @Test
    @DisplayName("Should track distinct source files per class")
    void shouldTrackSources() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance",
            "pump-aas.json"));

        GapReport report = service.compute(model, List.of());

        CoverageEntry entry = report.forClass("Asset").orElseThrow();
        assertEquals(List.of("pump-aas.json"), entry.sources());
    }

    @Test
    @DisplayName("Should aggregate coverage percentages per layer")
    void shouldAggregatePerLayerCoverage() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance"));

        GapReport report = service.compute(model, List.of());

        assertEquals(List.of("L1", "L2", "L2b", "L3", "L4"),
            report.layerCoverage().stream().map(LayerCoverage::layerId).toList());
        LayerCoverage l4 = report.layerCoverage().stream()
            .filter(l -> l.layerId().equals("L4")).findFirst().orElseThrow();
        assertEquals(4, l4.total());
        assertEquals(1, l4.present());
        assertEquals(3, l4.missing());
        assertEquals(25.0, l4.coveragePercent());
        LayerCoverage l1 = report.layerCoverage().stream()
            .filter(l -> l.layerId().equals("L1")).findFirst().orElseThrow();
        assertEquals(6, l1.total());
        assertEquals(0.0, l1.coveragePercent());
        assertEquals(6, l1.missing());
    }
}
