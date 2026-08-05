package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ManualEntity;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the manual-entry form for ontology classes that support it, with the
 * exact per-class field lists from the UI spec. All L1 classes
 * (Stakeholder, UseCase, Requirement, RequirementParameter,
 * RequirementConstraint, VerificationCriterion) are supported without file
 * upload. Fields are either free-text inputs or suggestion ComboBoxes that
 * still allow custom values.
 */
public final class ManualEntryFormFactory {

    /**
     * A single manual-entry field: the attribute key, the label shown to the
     * user, the suggested ComboBox options and whether the options must be
     * derived from the canonical model.
     */
    private record Field(String key, String label, List<String> suggestions, boolean fromModel) {

        static Field text(String key, String label) {
            return new Field(key, label, List.of(), false);
        }

        static Field suggested(String key, String label, List<String> suggestions) {
            return new Field(key, label, suggestions, false);
        }

        static Field modelRef(String key, String label) {
            return new Field(key, label, List.of(), true);
        }
    }

    /**
     * The manual-entry form specification for one ontology class.
     *
     * @param nameAttribute the field whose value also becomes the entity name
     * @param fields        the fields of the form
     */
    private record ClassSpec(String nameAttribute, List<Field> fields) {
    }

    private static final Map<String, ClassSpec> SPECS = buildSpecs();

    private ManualEntryFormFactory() {
    }

    private static Map<String, ClassSpec> buildSpecs() {
        Map<String, ClassSpec> specs = new LinkedHashMap<>();
        specs.put("Stakeholder", new ClassSpec("role", List.of(
            Field.text("role", "Role"),
            Field.text("organization", "Organization"),
            Field.text("contact", "Contact"))));
        specs.put("UseCase", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("description", "Description"),
            Field.modelRef("targetAsset", "Target Asset"))));
        specs.put("Requirement", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("description", "Description"),
            Field.suggested("priority", "Priority",
                List.of("Critical", "High", "Medium", "Low")))));
        specs.put("RequirementParameter", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("value", "Value"),
            Field.suggested("unit", "Unit",
                List.of("l/min", "bar", "\u00b0C", "%", "m", "s", "mA", "V")))));
        specs.put("RequirementConstraint", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("expression", "Expression"),
            Field.text("description", "Description"))));
        specs.put("VerificationCriterion", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("criterion", "Criterion"),
            Field.modelRef("verifiesRequirement", "Verifies Requirement"))));
        specs.put("DeploymentContext", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("runtime", "Runtime"),
            Field.text("zone", "Zone"))));
        specs.put("ClusterNode", new ClassSpec("hostname", List.of(
            Field.text("hostname", "Hostname"),
            Field.text("ipAddress", "IP Address"),
            Field.text("role", "Role"))));
        specs.put("NetworkZone", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("subnet", "Subnet"),
            Field.text("securityLevel", "Security Level"))));
        specs.put("Sensor", new ClassSpec("type", List.of(
            Field.text("type", "Type"),
            Field.text("location", "Location"),
            Field.modelRef("measuredVariable", "Measured Variable"))));
        specs.put("Capability", new ClassSpec("name", List.of(
            Field.text("name", "Name"),
            Field.text("description", "Description"),
            Field.modelRef("mapsToRequirement", "Maps-To Requirement"))));
        specs.put("ServiceInstance", new ClassSpec("instanceId", List.of(
            Field.text("instanceId", "Instance ID"),
            Field.modelRef("deployedOn", "Deployed On"),
            Field.suggested("status", "Status",
                List.of("Running", "Stopped", "Maintenance", "Unknown")))));
        return specs;
    }

    /**
     * Returns whether manual entry is supported for the given class.
     *
     * @param className the ontology class name
     * @return {@code true} when a manual form exists
     */
    public static boolean supports(String className) {
        return SPECS.containsKey(className);
    }

    /**
     * Creates the manual-entry form for the given class.
     *
     * @param className       the ontology class name
     * @param dropdownOptions options per dropdown field key, derived from the model
     * @param onSave          callback with the built entity
     * @return the form, or {@code null} when manual entry is not supported
     */
    public static VerticalLayout create(String className, Map<String, List<String>> dropdownOptions,
                                        Consumer<CanonicalEntity> onSave) {
        ClassSpec spec = SPECS.get(className);
        if (spec == null) {
            return null;
        }
        VerticalLayout form = new VerticalLayout();
        form.addClassName("hub-manual-form");
        form.setPadding(true);
        form.setSpacing(true);

        Span heading = new Span("Enter " + className + " manually");
        heading.addClassName("hub-manual-heading");
        form.add(heading);

        Map<String, TextField> textInputs = new LinkedHashMap<>();
        Map<String, ComboBox<String>> selectInputs = new LinkedHashMap<>();
        for (Field field : spec.fields()) {
            if (field.fromModel() || !field.suggestions().isEmpty()) {
                ComboBox<String> combo = new ComboBox<>(field.label());
                combo.setId("hub-field-" + className.toLowerCase() + "-" + field.key());
                combo.setItems(field.fromModel()
                    ? dropdownOptions.getOrDefault(field.key(), List.of())
                    : field.suggestions());
                combo.setAllowCustomValue(true);
                combo.setPlaceholder("Select " + field.label().toLowerCase() + "...");
                combo.setWidthFull();
                combo.addCustomValueSetListener(e -> combo.setValue(e.getDetail()));
                selectInputs.put(field.key(), combo);
                form.add(combo);
            } else {
                TextField input = new TextField(field.label());
                input.setId("hub-field-" + className.toLowerCase() + "-" + field.key());
                input.setPlaceholder("Enter " + field.label().toLowerCase() + "...");
                input.setWidthFull();
                textInputs.put(field.key(), input);
                form.add(input);
            }
        }

        Button save = new Button("Save " + className);
        save.addClassName("hub-manual-save");
        save.addClickListener(e -> {
            Map<String, String> values = new LinkedHashMap<>();
            textInputs.forEach((key, input) -> values.put(key, input.getValue().trim()));
            selectInputs.forEach((key, combo) ->
                values.put(key, combo.getValue() == null ? "" : combo.getValue().trim()));
            onSave.accept(buildEntity(className, spec, values));
        });
        form.add(save);
        return form;
    }

    private static CanonicalEntity buildEntity(String className, ClassSpec spec,
                                               Map<String, String> values) {
        String id = "manual-" + className.toLowerCase() + "-" + System.currentTimeMillis();
        String nameValue = values.getOrDefault(spec.nameAttribute(), "").trim();
        String name = nameValue.isBlank() ? className : nameValue;

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", id);
        attributes.put("name", name);
        for (Field field : spec.fields()) {
            attributes.put(field.key(), values.getOrDefault(field.key(), ""));
        }
        return new ManualEntity(className, id, name, attributes);
    }
}
