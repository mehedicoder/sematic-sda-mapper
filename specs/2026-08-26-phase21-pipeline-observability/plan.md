# Plan — Phase 21: End-to-End Pipeline Observability

1. Observability foundation (`com.sda.mapper.observability`)
   1.1 Register `ObservedAspect` bean; confirm OTLP export stays disabled unless
       `OTEL_EXPORTER_OTLP_ENDPOINT` is set.
   1.2 `PipelineTracingContext`: opens a run scope (root span), sets baggage
       `run.id` / `session.id` / `requirement.id`, bridges baggage to MDC so the
       existing `%X{traceId:-},%X{spanId:-}` log pattern correlates all stage logs.
2. Stage instrumentation
   2.1 `@Observed` on public stage methods: `LiftService`, `InferenceService`,
       `MatchingService`, `SemanticMatchService`, `RecommendationService`,
       `ManifestExportService`, `EndToEndPipelineService`, `SemanticReasonerService`,
       and the pipeline entry/stage methods of `MappingHubService`.
   2.2 Entry points (`MappingHubService` run, `EndToEndPipelineService`,
       `RequirementMatchingController` match flow) create the parent run span so a
       complete run yields exactly one trace with one child span per stage.
3. SPARQL visibility
   3.1 `TracedSparqlExecutor` wrapping query execution: records duration timer
       `sda.sparql.query` (tag `query = <file name>`), result count as span attribute
       + metric tag-safe summary; integrate into `SemanticMatchService`
       (`hub-match-offered-required.rq`) and `SemanticReasonerService`
       (`match-generic.rq`) without changing query semantics.
4. Audit log
   4.1 Flyway `V6__pipeline_audit_log.sql` — table per requirements data shape.
   4.2 `PipelineEvent` JPA entity + `PipelineEventRepository`.
   4.3 `PipelineAuditPublisher`: writes STARTED / SUCCEEDED / FAILED per stage
       transition on both paths; write failures are logged at ERROR and never abort
       the pipeline.
   4.4 `PipelineAuditController`: `GET /api/v1/pipeline-audits?sessionId=…&requirementId=…&runId=…&limit=…`
       returns the chronological timeline (`started_at ASC`, default limit 200, max 1000)
       per the requirements API contract; errors via ProblemDetail.
5. Dimensional match tagging
   5.1 Map `ExplanationService` dimensions to MATCH-span attributes
       (`match.dimension.capability|protocol|accessMode|latency|recall|deployment|r2a-concept`
       = pass/fail); for no-match runs, persist the failing dimensions in the audit
       `detail` so history explains failures too.
6. Metrics & dashboard
   6.1 Micrometer: `sda.pipeline.runs` counter (outcome tag), `sda.pipeline.stage.duration`
       timer (stage tag), `sda.pipeline.match.result` counter (hit/no-hit),
       alongside `sda.sparql.*`; verify exposure at `/actuator/prometheus`.
   6.2 `docs/grafana/sda-mapper-pipeline-dashboard.json` visualizing runs/min,
       stage durations, match hit-rate, SPARQL latencies.
7. Live UI console in `MappingHubView`
   7.1 Components under `hub/ui`: `StageTimeline` (per-stage status badges +
       durations), `PipelineTracePanel` (current/last run id, SPARQL stats,
       dimension results), `PipelineConsolePanel` (streamed event feed).
   7.2 **Enable push on `MappingHubView`** (`@Push`, or `PushConfiguration` AUTOMATIC) —
       required because no view currently enables push (Flow default DISABLED) and
       `UI.access` alone only reaches the client at the next interaction; then stream
       audit events for the active session run via the existing timer/`UI.access`
       pattern (as in `SessionOverviewView` / `OpcUaVisualizationView`); existing
       console `[STAGE]` lines remain untouched.
8. Tests
   8.1 Unit: `TracedSparqlExecutorTest` (timer recorded, query tag, result count),
       `PipelineAuditPublisherTest` (one row per transition; FAILED on forced error;
       write failure does not propagate), dimension-tag population test.
   8.2 Integration: `PipelineAuditControllerTest` (filter by session/requirement/run,
       ordered timeline, ProblemDetail on bad params); one-trace-per-run assertion
       via test tracer (see approved test-scoped dep) or MDC-correlation fallback.
   8.3 UI: observability panels render in `MappingHubView`, timeline updates after a
       run; existing `MappingHubViewTest` console assertions still pass.
   8.4 Grafana dashboard JSON parse test (Jackson), mirroring the manifest-YAML rule.
   8.5 Full suite green without any external collector/backend.
