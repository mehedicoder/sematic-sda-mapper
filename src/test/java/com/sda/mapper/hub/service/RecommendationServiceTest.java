package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Recommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Recommendation service")
class RecommendationServiceTest {

    private final RecommendationService service = new RecommendationService();

    private GapReport emptyReport() {
        return new MatchingService(new com.sda.mapper.service.OntologyDataService())
            .compute(new CanonicalAssetModel(), List.of());
    }

    @Test
    @DisplayName("Should list the formats that can fill a given class")
    void shouldListFormatsThatCanFill() {
        List<AssetFileFormat> formats = service.formatsThatCanFill("Sensor");
        assertTrue(formats.containsAll(List.of(
            AssetFileFormat.AAS_XML, AssetFileFormat.AAS_JSON,
            AssetFileFormat.MTP_AML, AssetFileFormat.OPCUA_JSON)));
        assertTrue(service.formatsThatCanFill("Asset").contains(AssetFileFormat.OPCUA_JSON));
        assertTrue(service.formatsThatCanFill("Skill").contains(AssetFileFormat.MTP_AML));
    }

    @Test
    @DisplayName("Should recommend every format when nothing is uploaded yet")
    void shouldRecommendAllFormatsInitially() {
        List<Recommendation> recommendations = service.recommend(emptyReport(), Set.of());
        assertEquals(4, recommendations.size());
    }

    @Test
    @DisplayName("Should never re-recommend an already uploaded format")
    void shouldExcludeUploadedFormats() {
        List<Recommendation> recommendations = service.recommend(
            emptyReport(), Set.of(AssetFileFormat.AAS_JSON));
        assertFalse(recommendations.stream().anyMatch(r -> r.format() == AssetFileFormat.AAS_JSON));
        assertTrue(recommendations.stream().anyMatch(r -> r.format() == AssetFileFormat.MTP_AML));
    }

    @Test
    @DisplayName("Should return no recommendations when all formats are uploaded")
    void shouldReturnEmptyWhenAllUploaded() {
        assertTrue(service.recommend(emptyReport(),
            Set.of(AssetFileFormat.values())).isEmpty());
    }

    @Test
    @DisplayName("Should sort recommendations by the number of classes they fill")
    void shouldSortByFillsDescending() {
        List<Recommendation> recommendations = service.recommend(emptyReport(), Set.of());
        for (int i = 1; i < recommendations.size(); i++) {
            assertTrue(recommendations.get(i - 1).fills().size()
                >= recommendations.get(i).fills().size());
        }
    }

    @Test
    @DisplayName("Should describe what an upload can fill")
    void shouldExplainFills() {
        Recommendation recommendation = service.recommend(emptyReport(), Set.of()).get(0);
        assertTrue(recommendation.reason().contains(recommendation.format().label()));
        assertFalse(recommendation.fills().isEmpty());
    }
}
