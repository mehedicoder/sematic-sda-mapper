package com.sda.mapper.ontology;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator that verifies the CSS-SDA ontology has been loaded into the Jena Model.
 * Delegates to {@link OntologyModelLoader} for readiness state.
 */
@Component
public class OntologyReadinessHealthIndicator implements HealthIndicator {

    private final OntologyModelLoader ontologyModelLoader;

    public OntologyReadinessHealthIndicator(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    @Override
    public Health health() {
        if (ontologyModelLoader.isLoaded()) {
            var model = ontologyModelLoader.getModel();
            long classCount = model.listSubjectsWithProperty(
                org.apache.jena.vocabulary.RDF.type,
                org.apache.jena.vocabulary.RDFS.Class).toList().size();
            return Health.up()
                .withDetail("ontology", ontologyModelLoader.getFilePath())
                .withDetail("status", "loaded")
                .withDetail("triples", model.size())
                .withDetail("classes", classCount)
                .build();
        } else {
            return Health.down()
                .withDetail("ontology", ontologyModelLoader.getFilePath())
                .withDetail("status", "not-loaded")
                .build();
        }
    }
}
