package com.sda.mapper.observability;

import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Match dimension tags land on the MATCH span of a run")
class MatchDimensionSpanTagTest {

    private CapturingHandler handler;
    private ObservationRegistry registry;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        handler = new CapturingHandler();
        registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        meterRegistry = new SimpleMeterRegistry();
    }

    @AfterEach
    void tearDown() {
        org.slf4j.MDC.clear();
    }

    @Test
    @DisplayName("Should tag every explanation dimension pass/fail on the reasoning-path MATCH span")
    void shouldTagDimensionsOnReasoningPathMatchSpan() {
        var loader = new com.sda.mapper.ontology.OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());

        PipelineMetrics metrics = new PipelineMetrics(meterRegistry);
        SemanticReasonerService reasoner = new SemanticReasonerService(
            loader,
            new SparqlQueryLoader(),
            new RDeployService(loader),
            new TracedSparqlExecutor(metrics),
            new MatchDimensionTagger(),
            new PipelineStageObserver(registry, new PipelineAuditPublisher(null), metrics),
            new PipelineTracingContext(registry),
            new PipelineAuditPublisher(null),
            metrics);

        reasoner.matchByRequirementId("REQ-001");

        List<Observation.Context> matchStages = handler.stopped.stream()
            .filter(c -> "pipeline MATCH".equals(c.getContextualName()))
            .toList();

        assertThat(matchStages).hasSize(1);
        Observation.Context matchStage = matchStages.getFirst();

        for (String dimension : List.of("capability", "protocol", "accessMode",
                "deployment", "recall", "latency", "r2a-concept")) {
            assertThat(matchStage.getHighCardinalityKeyValue("match.dimension." + dimension))
                .as("dimension %s must be tagged on the MATCH span", dimension)
                .extracting(io.micrometer.common.KeyValue::getValue)
                .isEqualTo("pass");
        }
    }

    /** Collects stopped observations so span attributes can be asserted. */
    private static final class CapturingHandler implements ObservationHandler<Observation.Context> {

        private final List<Observation.Context> stopped = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        @Override
        public void onStop(Observation.Context context) {
            stopped.add(context);
        }
    }
}
