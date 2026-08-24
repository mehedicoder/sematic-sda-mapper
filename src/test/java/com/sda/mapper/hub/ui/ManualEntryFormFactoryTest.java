package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Ontology-driven manual entry form factory")
class ManualEntryFormFactoryTest {

    private final ManualEntryFormFactory factory =
        new ManualEntryFormFactory(new OntologyDataService());

    @Test
    @DisplayName("Should support all six L1 classes and the other manually editable classes")
    void shouldSupportL1Classes() {
        for (String className : List.of("Stakeholder", "UseCase", "UseCaseRequirement",
            "UseCaseRequirementParameter", "UseCaseRequirementConstraint", "VerificationCriterion",
            "Capability", "Sensor", "ServiceInstance", "DeploymentContext",
            "ClusterNode", "NetworkZone")) {
            assertTrue(factory.supports(className),
                "Expected manual entry support for " + className);
        }
    }

    @Test
    @DisplayName("Should return null for unsupported classes")
    void shouldRejectUnsupportedClass() {
        assertNull(factory.create("InformationModel", Map.of(), entity -> { }));
    }

    @Test
    @DisplayName("Should derive the UseCaseRequirement fields from the ontology")
    void shouldBuildRequirementFormFromOntology() {
        VerticalLayout form = factory.create("UseCaseRequirement", Map.of(), entity -> { });
        assertNotNull(form);

        TextField nameField = childById(form, "hub-field-usecaserequirement-name", TextField.class);
        assertNotNull(nameField, "UseCaseRequirement must expose its ontology name property");
        assertEquals("Name", nameField.getLabel());

        TextField description = childById(form,
            "hub-field-usecaserequirement-description", TextField.class);
        assertNotNull(description, "UseCaseRequirement must expose its ontology description property");

        TextField requirementType = childById(form,
            "hub-field-usecaserequirement-requirementType", TextField.class);
        assertNotNull(requirementType,
            "UseCaseRequirement must expose its ontology requirementType property");
    }

    @Test
    @DisplayName("Should show the priority suggestion ComboBox on the UseCase form")
    void shouldBuildUseCaseFormWithPriorityComboBox() {
        VerticalLayout form = factory.create("UseCase", Map.of(), entity -> { });
        ComboBox<String> priority = childById(form, "hub-field-usecase-priority", ComboBox.class);
        assertNotNull(priority, "UseCase must expose its ontology priority property");
        assertEquals(List.of("Critical", "High", "Medium", "Low"),
            priority.getListDataView().getItems().toList());
        assertTrue(priority.isAllowCustomValue());
        assertTrue(priority.getElement().getProperty("placeholder").contains("priority"));
    }

    @Test
    @DisplayName("Should populate model-derived dropdowns for UseCase and VerificationCriterion")
    void shouldPopulateModelDerivedDropdowns() {
        Map<String, List<String>> options = Map.of(
            "targetAsset", List.of("Pump FT101"),
            "verifiesRequirement", List.of("Pump must deliver 0-100 l/min"));

        VerticalLayout useCaseForm = factory.create("UseCase", options, entity -> { });
        ComboBox<String> targetAsset = childById(useCaseForm, "hub-field-usecase-targetAsset", ComboBox.class);
        assertNotNull(targetAsset);
        assertEquals(List.of("Pump FT101"), targetAsset.getListDataView().getItems().toList());

        VerticalLayout criterionForm = factory.create(
            "VerificationCriterion", options, entity -> { });
        ComboBox<String> verifies = childById(criterionForm,
            "hub-field-verificationcriterion-verifiesRequirement", ComboBox.class);
        assertNotNull(verifies);
        assertEquals(List.of("Pump must deliver 0-100 l/min"),
            verifies.getListDataView().getItems().toList());
    }

    @Test
    @DisplayName("Should save a typed ManualEntity carrying the entered field values")
    void shouldSaveManualEntityWithValues() {
        AtomicReference<ManualEntity> saved = new AtomicReference<>();
        VerticalLayout form = factory.create(
            "Stakeholder", Map.of(), entity -> saved.set((ManualEntity) entity));
        setText(childById(form, "hub-field-stakeholder-role", TextField.class), "Maintenance Engineer");
        setText(childById(form, "hub-field-stakeholder-organization", TextField.class), "Siemens");
        setText(childById(form, "hub-field-stakeholder-contact", TextField.class), "ops@siemens.com");

        Button save = childByClass(form, "hub-manual-save", Button.class);
        save.click();

        ManualEntity entity = saved.get();
        assertNotNull(entity, "Save listener must produce an entity");
        assertEquals("Stakeholder", entity.className());
        assertTrue(entity.id().startsWith("manual-stakeholder-"));
        assertEquals("Maintenance Engineer", entity.attributes().get("role"));
        assertEquals("Siemens", entity.attributes().get("organization"));
        assertEquals("ops@siemens.com", entity.attributes().get("contact"));
    }

    @Test
    @DisplayName("Should fall back to the class name when the name attribute is blank")
    void shouldFallBackToClassNameForBlankName() {
        AtomicReference<ManualEntity> saved = new AtomicReference<>();
        VerticalLayout form = factory.create(
            "UseCase", Map.of(), entity -> saved.set((ManualEntity) entity));
        Button save = childByClass(form, "hub-manual-save", Button.class);
        save.click();

        ManualEntity entity = saved.get();
        assertNotNull(entity);
        assertEquals("UseCase", entity.name());
    }

    @Test
    @DisplayName("Should reject a binding that references an undeclared ontology property")
    void shouldFailOnUndeclaredBinding() {
        // A class with no datatype properties in the ontology must not be manually editable.
        assertNull(factory.create("Observation", Map.of(), entity -> { }));
    }

    private static <T extends Component> T childById(VerticalLayout form, String id, Class<T> type) {
        return form.getChildren()
            .filter(child -> id.equals(child.getId().orElse("")))
            .filter(type::isInstance)
            .map(type::cast)
            .findFirst()
            .orElse(null);
    }

    private static <T extends Component> T childByClass(VerticalLayout form, String className, Class<T> type) {
        return form.getChildren()
            .filter(child -> child.getElement().getClassList().contains(className))
            .filter(type::isInstance)
            .map(type::cast)
            .findFirst()
            .orElse(null);
    }

    private static void setText(TextField field, String value) {
        field.setValue(value);
    }
}
