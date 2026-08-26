package com.sda.mapper.hub.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic canonical entity for manually entered data.
 *
 * <p>Carries an arbitrary attribute map so gap forms can store the exact
 * per-class fields defined by the UI spec without expanding every typed
 * record. Manual entities always originate from the {@code "manual-entry"}
 * source and are explicit, never inferred.</p>
 */
public final class ManualEntity implements CanonicalEntity {

    private final String className;
    private final String id;
    private final String name;
    private final Map<String, Object> attributes;
    private final String sourceFileName;

    /**
     * Creates a manual entity with the default {@code "manual-entry"} source.
     *
     * @param className  the ontology class name
     * @param id         a stable identifier
     * @param name       the display name
     * @param attributes the domain attributes in insertion order
     */
    public ManualEntity(String className, String id, String name, Map<String, Object> attributes) {
        this(className, id, name, attributes, "manual-entry");
    }

    /**
     * Creates a manual entity with an explicit source file name (used when
     * projecting uploaded ABox Turtle individuals so coverage provenance points
     * at the originating upload rather than {@code "manual-entry"}).
     *
     * @param className       the ontology class name
     * @param id              a stable identifier
     * @param name            the display name
     * @param attributes      the domain attributes in insertion order
     * @param sourceFileName  the originating file name, or blank for the default
     */
    public ManualEntity(String className, String id, String name, Map<String, Object> attributes,
                        String sourceFileName) {
        this.className = className;
        this.id = id;
        this.name = name;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.sourceFileName = sourceFileName;
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
        return Optional.ofNullable(sourceFileName)
            .filter(s -> !s.isBlank())
            .or(() -> Optional.of("manual-entry"));
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
