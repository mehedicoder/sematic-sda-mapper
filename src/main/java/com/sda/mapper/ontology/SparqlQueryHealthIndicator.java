package com.sda.mapper.ontology;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Health indicator that verifies SPARQL query files are loadable.
 */
@Component
public class SparqlQueryHealthIndicator implements HealthIndicator {

    private static final String QUERY_PATH = "queries/match-service.rq";

    @Override
    public Health health() {
        try {
            var resource = new ClassPathResource(QUERY_PATH);
            if (resource.exists()) {
                return Health.up()
                    .withDetail("sparql-query", QUERY_PATH)
                    .withDetail("status", "loadable")
                    .build();
            } else {
                return Health.unknown()
                    .withDetail("sparql-query", QUERY_PATH)
                    .withDetail("status", "not-yet-created")
                    .withDetail("hint", "SPARQL queries will be added in Phase 5")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("sparql-query", QUERY_PATH)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
