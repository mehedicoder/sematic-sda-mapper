package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.Capability;
import com.sda.mapper.hub.model.ClusterNode;
import com.sda.mapper.hub.model.DeploymentContext;
import com.sda.mapper.hub.model.NetworkZone;
import com.sda.mapper.hub.model.Requirement;
import com.sda.mapper.hub.model.Sensor;
import com.sda.mapper.hub.model.ServiceInstance;
import com.sda.mapper.hub.model.Stakeholder;
import com.sda.mapper.hub.model.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the manual-entry form for ontology classes that support it, with the
 * exact field lists specified for gap filling.
 */
public final class ManualEntryFormFactory {

    private static final Map<String, List<String>> FIELD_LISTS = Map.ofEntries(
        Map.entry("Stakeholder", List.of("name", "role", "organization", "contact")),
        Map.entry("UseCase", List.of("name", "description", "priority")),
        Map.entry("Requirement", List.of("name", "description", "priority")),
        Map.entry("DeploymentContext", List.of("name", "description", "clusterType")),
        Map.entry("ClusterNode", List.of("name", "address", "role")),
        Map.entry("NetworkZone", List.of("name", "description", "cidr")),
        Map.entry("Sensor", List.of("name", "assetId", "unit")),
        Map.entry("Capability", List.of("name", "description")),
        Map.entry("ServiceInstance", List.of("serviceId", "endpointUrl"))
    );

    private ManualEntryFormFactory() {
    }

    /**
     * Returns whether manual entry is supported for the given class.
     *
     * @param className the ontology class name
     * @return {@code true} when a manual form exists
     */
    public static boolean supports(String className) {
        return FIELD_LISTS.containsKey(className);
    }

    /**
     * Creates the manual-entry form for the given class.
     *
     * @param className the ontology class name
     * @param onSave    callback with the built entity
     * @return the form, or {@code null} when manual entry is not supported
     */
    public static VerticalLayout create(String className, Consumer<CanonicalEntity> onSave) {
        List<String> fields = FIELD_LISTS.get(className);
        if (fields == null) {
            return null;
        }
        VerticalLayout form = new VerticalLayout();
        form.addClassName("hub-manual-form");
        form.setPadding(true);
        form.setSpacing(true);

        Span heading = new Span("Enter " + className + " manually");
        heading.addClassName("hub-manual-heading");
        form.add(heading);

        Map<String, TextField> inputs = new LinkedHashMap<>();
        for (String field : fields) {
            TextField input = new TextField(displayLabel(field));
            input.setId("hub-field-" + className.toLowerCase() + "-" + field);
            inputs.put(field, input);
            form.add(input);
        }

        Button save = new Button("Save " + className);
        save.addClassName("hub-manual-save");
        save.addClickListener(e -> {
            Map<String, String> values = new LinkedHashMap<>();
            inputs.forEach((field, input) -> values.put(field, input.getValue().trim()));
            onSave.accept(buildEntity(className, values));
        });
        form.add(save);
        return form;
    }

    private static String displayLabel(String field) {
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (Character.isUpperCase(c)) {
                label.append(' ');
            }
            label.append(c);
        }
        return label.toString().trim();
    }

    private static CanonicalEntity buildEntity(String className, Map<String, String> values) {
        String id = "manual-" + className.toLowerCase() + "-" + System.currentTimeMillis();
        return switch (className) {
            case "Stakeholder" -> new Stakeholder(id, value(values, "name"), value(values, "role"),
                value(values, "organization"), value(values, "contact"), "manual-entry");
            case "UseCase" -> new UseCase(id, value(values, "name"), value(values, "description"),
                value(values, "priority"), "manual-entry");
            case "Requirement" -> new Requirement(id, value(values, "name"), value(values, "description"),
                value(values, "priority"), "manual-entry");
            case "DeploymentContext" -> new DeploymentContext(id, value(values, "name"),
                value(values, "description"), value(values, "clusterType"), "manual-entry");
            case "ClusterNode" -> new ClusterNode(id, value(values, "name"), value(values, "address"),
                value(values, "role"), "manual-entry");
            case "NetworkZone" -> new NetworkZone(id, value(values, "name"), value(values, "description"),
                value(values, "cidr"), "manual-entry");
            case "Sensor" -> new Sensor(id, value(values, "name"), value(values, "assetId"),
                value(values, "unit"), "manual-entry");
            case "Capability" -> new Capability(id, value(values, "name"), value(values, "description"),
                "manual-entry");
            case "ServiceInstance" -> new ServiceInstance(id, value(values, "serviceId"),
                value(values, "endpointUrl"), "manual-entry");
            default -> throw new IllegalArgumentException("Manual entry not supported for " + className);
        };
    }

    private static String value(Map<String, String> values, String field) {
        return values.getOrDefault(field, "");
    }
}
