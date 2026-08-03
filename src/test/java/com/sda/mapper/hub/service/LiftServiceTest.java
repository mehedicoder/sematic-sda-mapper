package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.InformationModel;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.SemanticConcept;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Lift stage service")
class LiftServiceTest {

    private final LiftService service = new LiftService();

    @Test
    @DisplayName("Should derive one semantic concept per exposed node")
    void shouldDeriveConceptsPerNode() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new ProcessVariable("ns=2;s=Pump01.FlowRate", "FlowRate", "l/min",
            "ns=2;s=Pump01.FlowRate", "Pump01"));
        model.addEntity(new ProcessVariable("ns=2;s=Pump01.Pressure", "Pressure", "bar",
            "ns=2;s=Pump01.Pressure", "Pump01"));

        List<CanonicalEntity> lifted = service.lift(model);

        assertEquals(2, lifted.stream().filter(e -> e.className().equals("SemanticConcept")).count());
        assertTrue(lifted.stream().anyMatch(e -> e.name().equals("FlowRate")));
        assertTrue(lifted.stream().anyMatch(e -> e.name().equals("Pressure")));
    }

    @Test
    @DisplayName("Should derive an information model per asset")
    void shouldDeriveInformationModelPerAsset() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Asset("urn:pump", "Pump FT101", "Siemens", "FT101-2025", "Instance"));

        List<CanonicalEntity> lifted = service.lift(model);

        assertEquals(1, lifted.stream().filter(e -> e.className().equals("InformationModel")).count());
        InformationModel informationModel = (InformationModel) lifted.get(0);
        assertTrue(informationModel.name().contains("Pump FT101"));
        assertEquals("OPC UA", informationModel.modelType());
    }

    @Test
    @DisplayName("Should not mark lifted entities as inferred")
    void shouldNotFlagLiftedEntitiesAsInferred() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new ProcessVariable("ns=2;s=Pump01.Status", "Status", "",
            "ns=2;s=Pump01.Status", "Pump01"));

        List<CanonicalEntity> lifted = service.lift(model);

        assertFalse(lifted.stream().anyMatch(CanonicalEntity::inferred));
    }

    @Test
    @DisplayName("Should ignore nodes without a node id")
    void shouldIgnoreNodesWithoutId() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new ProcessVariable("pv-1", "NoNode", "", "", "Pump01"));

        List<CanonicalEntity> lifted = service.lift(model);

        assertEquals(0, lifted.stream().filter(e -> e.className().equals("SemanticConcept")).count());
    }

    @Test
    @DisplayName("Should return no entities for an empty model")
    void shouldReturnEmptyForEmptyModel() {
        assertTrue(service.lift(new CanonicalAssetModel()).isEmpty());
    }
}
