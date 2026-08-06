package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.AttributeBindings;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the manual-entry form for ontology classes that support it. The
 * per-class field inventory is derived from the v2 ontology: every supported
 * class shows the datatype properties declared on it in the TBox (projected
 * onto the hub attribute vocabulary via {@link AttributeBindings}), plus a
 * small set of curated convenience fields (free-text entries with no ontology
 * counterpart, e.g. {@code UseCase.description}) and object-property reference
 * ComboBoxes.
 *
 * <p>The property registry is validated against the ontology at construction,
 * so a field whose ontology property no longer exists fails startup with a
 * clear error instead of silently drifting from the TBox.</p>
 */
@Component
public class ManualEntryFormFactory {

    /**
     * A single manual-entry field: the hub attribute key, the label shown to
     * the user, the bound ontology datatype property (or {@code null} for
     * curated and reference fields), the suggested ComboBox options and
     * whether the options must be derived from the canonical model.
     */
    private record Field(String key, String label, String ontologyProperty,
                         List<String> suggestions, boolean fromModel) {

        static Field text(String key, String label, String ontologyProperty) {
            return new Field(key, label, ontologyProperty, List.of(), false);
        }

        static Field suggested(String key, String label, String ontologyProperty,
                               List<String> suggestions) {
            return new Field(key, label, ontologyProperty, suggestions, false);
        }

        static Field curated(String key, String label) {
            return new Field(key, label, null, List.of(), false);
        }

        static Field modelRef(String key, String label) {
            return new Field(key, label, null, List.of(), true);
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

    /**
     * How a bound ontology property is rendered in the form.
     */
    private enum Widget { TEXT, SUGGESTED }

    /**
     * Hub presentation of an ontology datatype property.
     *
     * @param hubKey      the attribute key stored on the entity
     * @param label       the label shown to the user
     * @param widget      the widget type
     * @param suggestions the suggested options for {@link Widget#SUGGESTED}
     */
    private record PropertyBinding(String hubKey, String label,
                                   Widget widget, List<String> suggestions) {

        static PropertyBinding text(String ontologyProperty, String label) {
            return new PropertyBinding(AttributeBindings.hubKey(ontologyProperty), label,
                Widget.TEXT, List.of());
        }

        static PropertyBinding suggested(String ontologyProperty, String label,
                                         List<String> suggestions) {
            return new PropertyBinding(AttributeBindings.hubKey(ontologyProperty), label,
                Widget.SUGGESTED, suggestions);
        }
    }

    /**
     * Ontology datatype properties shown on manual-entry forms, keyed by the
     * ontology property name. Every key must be declared as a datatype property
     * on at least one class of the TBox (validated at construction).
     */
    private static final Map<String, PropertyBinding> BINDINGS = Map.ofEntries(
        Map.entry("stakeholderName", PropertyBinding.text("stakeholderName", "Name")),
        Map.entry("role", PropertyBinding.text("role", "Role")),
        Map.entry("organization", PropertyBinding.text("organization", "Organization")),
        Map.entry("contactInfo", PropertyBinding.text("contactInfo", "Contact")),
        Map.entry("useCaseName", PropertyBinding.text("useCaseName", "Name")),
        Map.entry("priority", PropertyBinding.suggested("priority", "Priority",
            List.of("Critical", "High", "Medium", "Low"))),
        Map.entry("businessGoal", PropertyBinding.text("businessGoal", "Business Goal")),
        Map.entry("title", PropertyBinding.text("title", "Name")),
        Map.entry("description", PropertyBinding.text("description", "Description")),
        Map.entry("requirementType", PropertyBinding.text("requirementType", "Requirement Type")),
        Map.entry("parameterName", PropertyBinding.text("parameterName", "Name")),
        Map.entry("unit", PropertyBinding.suggested("unit", "Unit",
            List.of("l/min", "bar", "\u00b0C", "%", "m", "s", "mA", "V"))),
        Map.entry("dataType", PropertyBinding.text("dataType", "Data Type")),
        Map.entry("minValue", PropertyBinding.text("minValue", "Min Value")),
        Map.entry("maxValue", PropertyBinding.text("maxValue", "Max Value")),
        Map.entry("expression", PropertyBinding.text("expression", "Expression")),
        Map.entry("constraintType", PropertyBinding.text("constraintType", "Constraint Type")),
        Map.entry("criterionDescription", PropertyBinding.text("criterionDescription", "Criterion")),
        Map.entry("testMethod", PropertyBinding.text("testMethod", "Test Method")),
        Map.entry("passThreshold", PropertyBinding.text("passThreshold", "Pass Threshold")),
        Map.entry("deploymentMethod", PropertyBinding.text("deploymentMethod", "Runtime")),
        Map.entry("safetyZone", PropertyBinding.text("safetyZone", "Zone")),
        Map.entry("hostname", PropertyBinding.text("hostname", "Hostname")),
        Map.entry("marginDeviceRole", PropertyBinding.text("marginDeviceRole", "Role")),
        Map.entry("zoneName", PropertyBinding.text("zoneName", "Name")),
        Map.entry("zoneType", PropertyBinding.text("zoneType", "Zone Type")),
        Map.entry("iec62443Level", PropertyBinding.text("iec62443Level", "Security Level")),
        Map.entry("sensorType", PropertyBinding.text("sensorType", "Type")),
        Map.entry("capabilityName", PropertyBinding.text("capabilityName", "Name")),
        Map.entry("capabilityDescription", PropertyBinding.text("capabilityDescription", "Description")),
        Map.entry("capabilityVersion", PropertyBinding.text("capabilityVersion", "Version")),
        Map.entry("instanceID", PropertyBinding.text("instanceID", "Instance ID")),
        Map.entry("deployStatus", PropertyBinding.suggested("deployStatus", "Status",
            List.of("Running", "Stopped", "Maintenance", "Unknown")))
    );

    private final OntologyDataService ontologyDataService;
    private final Map<String, ClassSpec> specs;

    /**
     * Creates the factory and derives every manual form from the ontology.
     *
     * @param ontologyDataService the ontology data service
     * @throws IllegalStateException when a form field is bound to an ontology
     *                               property that does not exist, or a form has
     *                               no fields
     */
    public ManualEntryFormFactory(OntologyDataService ontologyDataService) {
        this.ontologyDataService = ontologyDataService;
        this.specs = buildSpecs();
        validate();
    }

    private Map<String, ClassSpec> buildSpecs() {
        Map<String, ClassSpec> specs = new LinkedHashMap<>();
        specs.put("Stakeholder", spec("Stakeholder", "name", List.of(), List.of()));
        specs.put("UseCase", spec("UseCase", "name",
            List.of(Field.curated("description", "Description")),
            List.of(Field.modelRef("targetAsset", "Target Asset"))));
        specs.put("UseCaseRequirement", spec("UseCaseRequirement", "name", List.of(), List.of()));
        specs.put("UseCaseRequirementParameter",
            spec("UseCaseRequirementParameter", "name", List.of(), List.of()));
        specs.put("UseCaseRequirementConstraint",
            spec("UseCaseRequirementConstraint", "name",
                List.of(Field.curated("name", "Name")), List.of()));
        specs.put("VerificationCriterion", spec("VerificationCriterion", "name",
            List.of(Field.curated("name", "Name")),
            List.of(Field.modelRef("verifiesRequirement", "Verifies UseCaseRequirement"))));
        specs.put("DeploymentContext", spec("DeploymentContext", "name",
            List.of(Field.curated("name", "Name")), List.of()));
        specs.put("ClusterNode", spec("ClusterNode", "hostname",
            List.of(Field.curated("ipAddress", "IP Address")), List.of()));
        specs.put("NetworkZone", spec("NetworkZone", "name",
            List.of(Field.curated("subnet", "Subnet")), List.of()));
        specs.put("Sensor", spec("Sensor", "name",
            List.of(Field.curated("name", "Name"), Field.curated("location", "Location")),
            List.of(Field.modelRef("measuredVariable", "Measured Variable"))));
        specs.put("Capability", spec("Capability", "name", List.of(),
            List.of(Field.modelRef("mapsToRequirement", "Maps-To UseCaseRequirement"))));
        specs.put("ServiceInstance", spec("ServiceInstance", "name", List.of(),
            List.of(Field.modelRef("deployedOn", "Deployed On"))));
        return specs;
    }

    private ClassSpec spec(String className, String nameAttribute,
                           List<Field> curatedFields, List<Field> referenceFields) {
        List<Field> datatypeFields = ontologyDataService.getDatatypeProperties(className).stream()
            .filter(property -> BINDINGS.containsKey(property.name()))
            .map(property -> {
                PropertyBinding binding = BINDINGS.get(property.name());
                return switch (binding.widget()) {
                    case TEXT -> Field.text(binding.hubKey(), binding.label(), property.name());
                    case SUGGESTED -> Field.suggested(binding.hubKey(), binding.label(),
                        property.name(), binding.suggestions());
                };
            })
            .toList();
        List<Field> fields = new ArrayList<>(datatypeFields);
        fields.addAll(curatedFields);
        fields.addAll(referenceFields);
        return new ClassSpec(nameAttribute, List.copyOf(fields));
    }

    private void validate() {
        for (Map.Entry<String, ClassSpec> entry : specs.entrySet()) {
            String className = entry.getKey();
            if (ontologyDataService.getClassByName(className).isEmpty()) {
                throw new IllegalStateException(
                    "Manual entry form references unknown ontology class: " + className);
            }
            if (entry.getValue().fields().isEmpty()) {
                throw new IllegalStateException(
                    "Manual entry form for " + className + " has no fields");
            }
        }
        List<String> declaredProperties = ontologyDataService.getDatatypeProperties().stream()
            .map(property -> property.name())
            .toList();
        for (String ontologyProperty : BINDINGS.keySet()) {
            if (!declaredProperties.contains(ontologyProperty)) {
                throw new IllegalStateException(
                    "Manual entry binding references undeclared ontology datatype property: "
                        + ontologyProperty);
            }
        }
    }

    /**
     * Returns whether manual entry is supported for the given class.
     *
     * @param className the ontology class name
     * @return {@code true} when a manual form exists
     */
    public boolean supports(String className) {
        return specs.containsKey(className);
    }

    /**
     * Creates the manual-entry form for the given class.
     *
     * @param className       the ontology class name
     * @param dropdownOptions options per dropdown field key, derived from the model
     * @param onSave          callback with the built entity
     * @return the form, or {@code null} when manual entry is not supported
     */
    public VerticalLayout create(String className, Map<String, List<String>> dropdownOptions,
                                 Consumer<CanonicalEntity> onSave) {
        ClassSpec spec = specs.get(className);
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
