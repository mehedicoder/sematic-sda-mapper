package com.sda.mapper.observability;

import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.MtpModelParser;
import com.sda.mapper.hub.parser.OpcUaModelParser;
import com.sda.mapper.hub.service.InferenceService;
import com.sda.mapper.hub.service.LiftService;
import com.sda.mapper.hub.service.ManifestExportService;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.MappingHubSession;
import com.sda.mapper.hub.service.MatchingService;
import com.sda.mapper.hub.service.PipelineResult;
import com.sda.mapper.hub.service.RecommendationService;
import com.sda.mapper.hub.service.SemanticMatchService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("One pipeline run produces one root observation with one child per stage")
class PipelineObservabilityTraceShapeTest {

    private CapturingHandler handler;
    private ObservationRegistry registry;
    private SimpleMeterRegistry meterRegistry;
    private List<PipelineEvent> auditEvents;

    @BeforeEach
    void setUp() {
        handler = new CapturingHandler();
        registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        meterRegistry = new SimpleMeterRegistry();
        auditEvents = new CopyOnWriteArrayList<>();
    }

    private MappingHubService createService() {
        PipelineAuditPublisher publisher = new PipelineAuditPublisher(null);
        publisher.addListener(auditEvents::add);
        PipelineStageObserver observer =
            new PipelineStageObserver(registry, publisher, new PipelineMetrics(meterRegistry));
        return new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            new MatchingService(new com.sda.mapper.service.OntologyDataService()),
            new SemanticMatchService(new SparqlQueryLoader(),
                new TracedSparqlExecutor(new PipelineMetrics(meterRegistry))),
            new RecommendationService(),
            new ManifestExportService(
                new MatchingService(new com.sda.mapper.service.OntologyDataService()),
                new RecommendationService()),
            new MappingHubSession(),
            null, null, null,
            observer,
            new PipelineTracingContext(registry),
            publisher,
            new PipelineMetrics(meterRegistry),
            null,
            null);
    }

    @Test
    @DisplayName("Should open a root run observation and one child observation per executed stage")
    void shouldNestStageObservationsUnderRun() {
        MappingHubService service = createService();

        try (InputStream in = getClass()
                .getResourceAsStream("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            byte[] content = in.readAllBytes();
            service.processUpload("pump-aas.json", content);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        Observation.Context runContext = handler.findByName("sda.pipeline.run");
        assertThat(runContext).as("exactly one root sda.pipeline.run observation").isNotNull();

        List<Observation.Context> stageContexts = handler.findByNameStartingWith("sda.pipeline.stage");
        assertThat(stageContexts).extracting(c -> c.getContextualName())
            .containsSubsequence(
                "pipeline INSTANTIATE", "pipeline LIFT", "pipeline INFER", "pipeline MATCH");

        for (Observation.Context stage : stageContexts) {
            assertThat(stage.getParentObservation())
                .as("stage must be parented to the run observation")
                .isNotNull();
            assertThat(stage.getParentObservation().getContextView().getName())
                .as("parent of every stage span")
                .isEqualTo("sda.pipeline.run");
        }
    }

    @Test
    @DisplayName("Should expose run correlation through MDC and OTel baggage while stages execute")
    void shouldCorrelateRunThroughMdcAndBaggage() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json", fixture());

        List<String> stageMdcRunIds = handler.mdcRunIdsAtStart("sda.pipeline.stage");
        assertThat(stageMdcRunIds).isNotEmpty();
        assertThat(stageMdcRunIds).allMatch(id -> id != null && !id.isBlank(),
            "every stage must observe a runId in the MDC for log correlation");

        String runId = stageMdcRunIds.getFirst();
        List<String> stageBaggageRunIds = handler.baggageRunIdsAtStart("sda.pipeline.stage");
        assertThat(stageBaggageRunIds).isNotEmpty();
        assertThat(stageBaggageRunIds).containsOnly(runId);

        List<String> runMdcRunIds = handler.mdcRunIdsAtStart("sda.pipeline.run");
        assertThat(runMdcRunIds).hasSize(1);
        assertThat(runMdcRunIds.getFirst()).isEqualTo(runId);
    }

    @Test
    @DisplayName("Should write STARTED plus SUCCEEDED audit rows for every executed stage")
    void shouldAuditEveryExecutedStage() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json", fixture());

        for (String stage : List.of("INSTANTIATE", "LIFT", "INFER", "MATCH")) {
            assertThat(auditEvents.stream()
                    .filter(e -> stage.equals(e.stage()))
                    .map(PipelineEvent::status))
                .as("stage %s transitions", stage)
                .contains(PipelineEvent.STARTED, PipelineEvent.SUCCEEDED);
        }
    }

    @Test
    @DisplayName("Should expose pipeline SLI metrics after a run")
    void shouldRecordSliMetrics() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json", fixture());

        assertThat(meterRegistry.get("sda.pipeline.runs")
            .tag("path", "hub").tag("outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("sda.pipeline.match.result")
            .tag("result", "no-hit").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("sda.pipeline.stage.duration").timers()).isNotEmpty();
        assertThat(meterRegistry.get("sda.sparql.query").timers()).isNotEmpty();
    }

    @Test
    @DisplayName("Should record a FAILED audit row when a stage throws")
    void shouldAuditFailure() {
        PipelineAuditPublisher publisher = new PipelineAuditPublisher(null);
        List<PipelineEvent> events = new CopyOnWriteArrayList<>();
        publisher.addListener(events::add);
        PipelineStageObserver observer =
            new PipelineStageObserver(registry, publisher, new PipelineMetrics(meterRegistry));

        try (PipelineStageObserver.Stage stage = observer.begin("MATCH", null, null, "REQ-X")) {
            stage.error(new IllegalStateException("match failed"));
            throw new IllegalStateException("match failed");
        } catch (IllegalStateException expected) {
            // rethrown unchanged
        }

        assertThat(events).extracting(PipelineEvent::status)
            .containsExactly(PipelineEvent.STARTED, PipelineEvent.FAILED);
        assertThat(events.get(1).detail()).isEqualTo("match failed");
    }

    private byte[] fixture() {
        try (InputStream in = getClass()
                .getResourceAsStream("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json")) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Collects started/stopped observations so span topology can be asserted. */
    private static final class CapturingHandler implements ObservationHandler<Observation.Context> {

        private final List<Observation.Context> started = new CopyOnWriteArrayList<>();
        private final List<Observation.Context> stopped = new CopyOnWriteArrayList<>();
        private final java.util.concurrent.ConcurrentMap<Observation.Context, String> mdcRunIds =
            new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.concurrent.ConcurrentMap<Observation.Context, String> baggageRunIds =
            new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        @Override
        public void onStart(Observation.Context context) {
            started.add(context);
            String mdcRunId = org.slf4j.MDC.get(PipelineTracingContext.MDC_RUN_ID);
            mdcRunIds.put(context, mdcRunId == null ? "" : mdcRunId);
            String baggageRunId =
                io.opentelemetry.api.baggage.Baggage.current().getEntryValue("run.id");
            baggageRunIds.put(context, baggageRunId == null ? "" : baggageRunId);
        }

        @Override
        public void onStop(Observation.Context context) {
            stopped.add(context);
        }

        Observation.Context findByName(String name) {
            return stopped.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElse(null);
        }

        List<Observation.Context> findByNameStartingWith(String prefix) {
            return stopped.stream()
                .filter(c -> c.getName().startsWith(prefix))
                .toList();
        }

        List<String> mdcRunIdsAtStart(String observationName) {
            return started.stream()
                .filter(c -> c.getName().equals(observationName))
                .map(mdcRunIds::get)
                .toList();
        }

        List<String> baggageRunIdsAtStart(String observationName) {
            return started.stream()
                .filter(c -> c.getName().equals(observationName))
                .map(baggageRunIds::get)
                .toList();
        }
    }
}
