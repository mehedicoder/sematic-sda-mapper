# Validation — Phase 21: End-to-End Pipeline Observability

## Automated

- `mvn clean test` passes; full suite green on the feature branch (baseline 536).
- Required assertions:
  - `TracedSparqlExecutorTest`: timer `sda.sparql.query` recorded with `query` tag;
    result count captured for match and no-match executions.
  - `PipelineAuditPublisherTest`: exactly one STARTED + one SUCCEEDED row per executed
    stage; FAILED row (with reason in `detail`) when a stage throws; an injected
    repository failure is logged and does not abort the pipeline run.
  - Trace-shape test: one root run span + one child span per executed stage, baggage
    attributes (`run.id`, `session.id`, `requirement.id`) present, MDC carries
    traceId/spanId during stage execution.
  - No-match run: at least one `match.dimension.* = fail` tag, consistent with the
    user-facing explanation.
  - `PipelineAuditControllerTest`: timeline filtered by sessionId / requirementId /
    runId, ordered by `started_at` ASC, `limit` default 200 / max 1000 enforced;
    invalid params → ProblemDetail.
  - Prometheus scrape test: `/actuator/prometheus` contains `sda.pipeline.runs`,
    `sda.pipeline.stage.duration`, `sda.pipeline.match.result`, `sda.sparql.query`.
  - Grafana dashboard JSON parses via Jackson.
  - Existing `MappingHubServiceTest` / `MappingHubViewTest` console-line assertions
    unchanged and passing.

## Manual

1. Start app (`docker compose up --build` or `mvn spring-boot:run`); open
   `MappingHubView`, load the demo session, run the pipeline:
   - Stage timeline updates live during the run without manual refresh.
   - Console still shows the familiar `[INSTANTIATE] / [LIFT] / [INFER] / [MATCH]` lines.
2. After completion, query
   `GET /api/v1/pipeline-audits?sessionId=<demo-session>` — full chronological
   history returned.
3. Force a no-match (e.g. protocol mismatch requirement via `RequirementView`):
   trace/metrics expose the failing dimension; audit `detail` names it; the UI panel
   and the explanation text agree.
4. `curl -s localhost:8080/actuator/prometheus | grep sda.pipeline` — metrics present.
5. Optional (not CI-gated): point `OTEL_EXPORTER_OTLP_ENDPOINT` at a local Jaeger and
   confirm one trace per run arrives.

## Tone check

UI labels human-readable and consistent with `PipelineStage` labels ("Parse", "Lift",
"Infer", "Match", "Manifest"); error/failure text actionable; no jargon leaks into user copy.

## Definition of done

- All Phase 21 acceptance criteria in `specs/roadmap.md` checked `[x]` with evidence.
- `tasks.md` updated per AGENTS workflow (evidence-based only).
- No new runtime dependencies; any test-scoped tracer dependency explicitly approved.
- No new infrastructure services introduced.
