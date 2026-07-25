package com.sda.mapper.imports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Import Recommendation Service")
class ImportRecommendationServiceTest {

    private ImportRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new ImportRecommendationService();
    }

    @Test
    @DisplayName("Should recommend OPC UA Datasheet for missing CommunicationBinding")
    void shouldRecommendOpcUaDatasheetForMissingBinding() {
        var items = List.of(
            new MissingItem(MissingCategory.MISSING_COMMUNICATION_BINDING,
                "http://test#Svc", "Test Svc", "Service", "sda:usesBinding",
                "missing binding", true)
        );

        var recs = service.recommend(items);
        assertTrue(recs.containsKey(MissingCategory.MISSING_COMMUNICATION_BINDING));
        var rec = recs.get(MissingCategory.MISSING_COMMUNICATION_BINDING);
        assertTrue(rec.suggestedFileTypes().contains(FileType.OPC_UA_DATASHEET));
        assertEquals(1, rec.affectedItemCount());
    }

    @Test
    @DisplayName("Should recommend MTP AML for missing Capability")
    void shouldRecommendMtpAmlForMissingCapability() {
        var items = List.of(
            new MissingItem(MissingCategory.MISSING_CAPABILITY,
                "http://test#Svc", "Test Svc", "Service", "capability chain",
                "missing capability", true)
        );

        var recs = service.recommend(items);
        assertTrue(recs.containsKey(MissingCategory.MISSING_CAPABILITY));
        var rec = recs.get(MissingCategory.MISSING_CAPABILITY);
        assertTrue(rec.suggestedFileTypes().contains(FileType.MTP_AML));
    }

    @Test
    @DisplayName("Should recommend AAS for missing DeploymentContext")
    void shouldRecommendAasForMissingDeployment() {
        var items = List.of(
            new MissingItem(MissingCategory.MISSING_DEPLOYMENT_CONTEXT,
                "http://test#Deploy", "Deploy", "DeploymentContext", "props",
                "missing deploy", true)
        );

        var recs = service.recommend(items);
        assertTrue(recs.containsKey(MissingCategory.MISSING_DEPLOYMENT_CONTEXT));
        var rec = recs.get(MissingCategory.MISSING_DEPLOYMENT_CONTEXT);
        assertTrue(rec.suggestedFileTypes().contains(FileType.AAS));
    }

    @Test
    @DisplayName("Should count affected items per category")
    void shouldCountAffectedItems() {
        var items = List.of(
            new MissingItem(MissingCategory.MISSING_SEMANTIC_ID,
                "http://test#P1", "P1", "SkillParameter", "sda:semanticID", "missing", true),
            new MissingItem(MissingCategory.MISSING_SEMANTIC_ID,
                "http://test#P2", "P2", "SkillParameter", "sda:semanticID", "missing", true),
            new MissingItem(MissingCategory.MISSING_ENGINEERING_UNIT,
                "http://test#P3", "P3", "SkillParameter", "sda:engineeringUnit", "missing", true)
        );

        var recs = service.recommend(items);
        assertEquals(2, recs.get(MissingCategory.MISSING_SEMANTIC_ID).affectedItemCount());
        assertEquals(1, recs.get(MissingCategory.MISSING_ENGINEERING_UNIT).affectedItemCount());
    }

    @Test
    @DisplayName("Should return empty recommendations for no missing items")
    void shouldReturnEmptyForNoMissingItems() {
        var recs = service.recommend(List.of());
        assertTrue(recs.isEmpty());
    }

    @Test
    @DisplayName("Should recommend no additional file for duplicate assets")
    void shouldRecommendNoFileForDuplicates() {
        var items = List.of(
            new MissingItem(MissingCategory.DUPLICATE_ASSET,
                "http://test#Dup", "Dup", "Service", "label", "duplicate", false)
        );

        var recs = service.recommend(items);
        assertTrue(recs.containsKey(MissingCategory.DUPLICATE_ASSET));
        assertTrue(recs.get(MissingCategory.DUPLICATE_ASSET).suggestedFileTypes().isEmpty());
    }
}
