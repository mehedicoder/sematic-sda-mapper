package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical entity lifted from a generic AutomationML (CAEX) description.
 *
 * <p>AutomationML is profile-based (MTP is one such profile). Unlike the MTP AML
 * profile, a plain AutomationML/CAEX document does not map onto the fixed
 * Skill/Service vocabulary, so its elements are carried as uniform attribute
 * maps keyed by their CAEX element type (e.g. {@code InternalElement},
 * {@code ExternalInterface}). This keeps the extracted content visible in the
 * Mapping Hub canonical model and manifest export without hardcoding a single
 * profile's service-selection rules.</p>
 */
public class AutomationMLEntity implements CanonicalEntity {

    private final String className;
    private final String id;
    private final String name;
    private final String sourceFileName;
    private final Map<String, Object> attributes;

    /**
     * Creates an AutomationML-derived canonical entity.
     *
     * @param className      the CAEX element type, e.g. "InternalElement"
     * @param id             stable identifier (CAEX ID or generated)
     * @param name           display name (CAEX Name)
     * @param sourceFileName name of the source file, or {@code null}
     * @param attributes     extracted attribute map
     */
    public AutomationMLEntity(String className, String id, String name,
                              String sourceFileName, Map<String, Object> attributes) {
        this.className = Objects.requireNonNull(className, "className");
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.sourceFileName = sourceFileName;
        this.attributes = Map.copyOf(attributes);
    }

    @Override
    public String className() {
        return className;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<String> sourceFileName() {
        return Optional.ofNullable(sourceFileName);
    }

    @Override
    public boolean inferred() {
        return false;
    }

    @Override
    public Map<String, Object> attributes() {
        return attributes;
    }
}
