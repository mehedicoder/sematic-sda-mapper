package com.sda.mapper.ontology;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Health indicator that verifies the CSS-SDA ontology file is present and loadable.
 * Returns UNKNOWN when the file is not yet created (Phase 2 pending),
 * and DOWN only when the file exists but is malformed.
 */
@Component
public class OntologyReadinessHealthIndicator implements HealthIndicator {

    private static final String ONTOLOGY_PATH = "ontology/css-sda-small.ttl";

    @Override
    public Health health() {
        try {
            var resource = new ClassPathResource(ONTOLOGY_PATH);
            if (resource.exists()) {
                return Health.up()
                    .withDetail("ontology", ONTOLOGY_PATH)
                    .withDetail("status", "present")
                    .build();
            } else {
                return Health.unknown()
                    .withDetail("ontology", ONTOLOGY_PATH)
                    .withDetail("status", "not-yet-created")
                    .withDetail("hint", "Ontology will be added in Phase 2")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("ontology", ONTOLOGY_PATH)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
