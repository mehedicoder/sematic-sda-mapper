package com.sda.mapper.support;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Test-support helper that merges the demo ABox fixture
 * ({@code ontology/demo-abox.ttl}) into a Jena model that already holds the
 * TBox-only {@code css-sda-unified.ttl} ontology.
 * <p>
 * The demo ABox is the faithful migration of the former combined ontology's
 * instance data onto the canonical {@code https://example.org/ontology/sda#}
 * vocabulary (see {@code src/test/resources/ontology/demo-abox.ttl}).
 */
public final class DemoAboxBuilder {

    private static final String DEMO_ABOX_PATH = "ontology/demo-abox.ttl";

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
