package com.sda.mapper.observability;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the pipeline observability components that need constructor access to
 * infrastructure beans.
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Creates the run correlation context over the shared observation registry.
     *
     * @param observationRegistry micrometer observation registry (OTel-backed)
     * @return the pipeline tracing context
     */
    @Bean
    public PipelineTracingContext pipelineTracingContext(ObservationRegistry observationRegistry) {
        return new PipelineTracingContext(observationRegistry);
    }
}
