package com.sda.mapper.hub.model;

import com.sda.mapper.hub.service.MatchingService;
import com.sda.mapper.service.OntologyDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Manual entity conversion")
class ManualEntityConverterTest {

    private static ManualEntity manual(String className, Map<String, Object> attributes) {
        return new ManualEntity(className,
            String.valueOf(attributes.get("id")),
            String.valueOf(attributes.get("name")),
            attributes);
    }

    private static Map<String, Object> attributes(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("Should convert a manual UseCaseRequirement into a typed requirement stored in the model")
    void shouldConvertManualRequirementToTypedEntity() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(manual("UseCaseRequirement", attributes(
            "id", "manual-requirement-1",
            "name", "Pump must deliver 0-100 l/min",
            "description", "Flow control over the nominal range",
            "priority", "High")));

        assertEquals(1, model.countForClass("UseCaseRequirement"));
        assertEquals(CoverageStatus.PRESENT,
            new MatchingService(new OntologyDataService())
                .compute(model, List.of()).forClass("UseCaseRequirement").orElseThrow().status());
        UseCaseRequirement requirement = model.requirements().get(0);
        assertEquals("Pump must deliver 0-100 l/min", requirement.name());
        assertEquals("High", requirement.priority());
        assertTrue(requirement.sourceFileName().isPresent());
        assertEquals("manual-entry", requirement.sourceFileName().orElse(""));
        assertFalse(requirement.inferred());
    }

    @Test
    @DisplayName("Should store manual requirement parameter and constraint as typed entities")
    void shouldConvertManualL1RequirementTypes() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(manual("UseCaseRequirementParameter", attributes(
            "id", "manual-reqparam-1", "name", "Flow", "value", "100", "unit", "l/min")));
        model.addEntity(manual("UseCaseRequirementConstraint", attributes(
            "id", "manual-reqconstraint-1", "name", "Accuracy", "expression", "<= 0.1 %",
            "description", "Accuracy class")));

        assertEquals(1, model.requirementParameters().size());
        assertEquals(1, model.requirementConstraints().size());
        assertEquals("l/min", model.requirementParameters().get(0).unit());
        assertEquals("<= 0.1 %", model.requirementConstraints().get(0).expression());
    }

    @Test
    @DisplayName("Should cover Stakeholder, VerificationCriterion and the other supported classes")
    void shouldConvertAllSupportedClasses() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(manual("Stakeholder", attributes(
            "id", "manual-stakeholder-1", "name", "Maintenance", "role", "Maintenance Engineer",
            "organization", "Siemens", "contact", "ops@siemens.com")));
        model.addEntity(manual("UseCase", attributes(
            "id", "manual-usecase-1", "name", "Monitor Pump", "description", "Monitor pump health")));
        model.addEntity(manual("VerificationCriterion", attributes(
            "id", "manual-criterion-1", "name", "Accuracy check", "criterion", "Accuracy <= 0.1%",
            "method", "Inspection")));

        assertEquals(1, model.stakeholders().size());
        assertEquals(1, model.useCases().size());
        assertEquals(1, model.verificationCriteria().size());
        assertEquals("Maintenance Engineer", model.stakeholders().get(0).role());
        assertEquals("Inspection", model.verificationCriteria().get(0).method());
    }

    @Test
    @DisplayName("Should reject manual entities for classes without a mapping")
    void shouldRejectUnknownClass() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", "x");
        attributes.put("name", "x");
        assertThrows(IllegalArgumentException.class,
            () -> model.addEntity(manual("InformationModel", attributes)));
    }
}
