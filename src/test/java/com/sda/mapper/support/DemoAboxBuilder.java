package com.sda.mapper.support;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Test-support helper that merges the v2 demo ABox fixture
 * ({@code ontology/demo-abox-v2.ttl}) into a Jena model that already holds the
 * TBox-only {@code sda_ontology_v2_structured.ttl} ontology.
 * <p>
 * The demo ABox uses the canonical v2 vocabulary
 * {@code http://sda-platform.org/ontology#} with instance namespace
 * {@code http://www.example.org/instance#} (see
 * {@code src/test/resources/ontology/demo-abox-v2.ttl}).
 */
public final class DemoAboxBuilder {

    private static final String DEMO_ABOX_PATH = "ontology/demo-abox-v2.ttl";

    private DemoAboxBuilder() {
    }

    /**
     * Merges the demo ABox fixture into the given model.
     *
     * @param model the model to enrich (usually a loaded TBox ontology model)
     * @return the same model, now containing the demo instance data
     */
    public static Model apply(Model model) {
        try {
            ClassPathResource resource = new ClassPathResource(DEMO_ABOX_PATH);
            if (!resource.exists()) {
                throw new IllegalStateException("Demo ABox fixture not found on classpath: " + DEMO_ABOX_PATH);
            }
            Model abox = ModelFactory.createDefaultModel();
            try (InputStream in = resource.getInputStream()) {
                abox.read(in, null, "TTL");
            }
            model.add(abox);
            return model;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read demo ABox fixture: " + DEMO_ABOX_PATH, e);
        }
    }
}
