# Requirements — Phase 21: End-to-End Pipeline Observability

## Scope

Full visibility into every pipeline run across **both** execution paths:

1. **Hub path** — `MappingHubService` stage methods (`INSTANTIATE → LIFT → INFER → MATCH`
   + parse-on-upload + manifest export), driven from `MappingHubView`.
2. **Reasoning path** — `EndToEndPipelineService` / `SemanticReasonerService`, driven from
   the REST API (`POST /api/v1/requirements/match`).

Included:

- **Distributed tracing**: `@Observed` on every pipeline stage service (`LiftService`,
  `InferenceService`, `MatchingService`, `SemanticMatchService`, `RecommendationService`,
  `ManifestExportService`, `EndToEndPipelineService`, `SemanticReasonerService`,
  `MappingHubService`); one root span per run; OpenTelemetry baggage (`requirement.id`,
  `session.id`, `run.id`) bridged into MDC for log correlation.
- **Audit log**: `PipelineEvent` + `pipeline_audit_log` table (Flyway `V6__pipeline_audit_log.sql`)
  persisting every stage transition; historical queryability via
  `GET /api/v1/pipeline-audits?sessionId=…&requirementId=…&runId=…`.
- **SPARQL visibility**: `TracedSparqlExecutor` around query execution
  (`hub-match-offered-required.rq`, `match-generic.rq`) recording query name, duration,
  result count as `sda.sparql.*` metrics + span attributes.
- **Dimensional match tagging**: per-dimension pass/fail span tags (capability,
  protocol/accessMode, latency/recall, deployment zone/SIL/determinism, R2a concept
  coverage) on MATCH spans, derived from `ExplanationService` dimensions.
- **Live UI console**: `PipelineTracePanel`, `StageTimeline`, `PipelineConsolePanel`
  embedded in `MappingHubView`; server-push streaming of stage transitions during a run.
- **Metrics & dashboards**: `sda.pipeline.*` counters/timers exposed at
  `/actuator/prometheus`; Grafana dashboard JSON exported to `docs/grafana/`.

### Data shape — `pipeline_audit_log`

| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| run_id | VARCHAR(64) | groups one pipeline run |
| session_id | TEXT | hub session; NULL for pure REST runs |
| requirement_id | TEXT | NULL for non-requirement stages |
| stage | VARCHAR(32) | text (covers `INSTANTIATE`, which is not in `PipelineStage`) |
| status | VARCHAR(16) | STARTED / SUCCEEDED / FAILED |
| started_at | TIMESTAMPTZ | |
| duration_ms | BIGINT | NULL for STARTED |
| detail | TEXT | match summary or failure reason |

### Audit API contract

```
GET /api/v1/pipeline-audits?sessionId=…&requirementId=…&runId=…&limit=…
```

- All filters optional and combinable; `limit` optional int (default **200**, max **1000**) — bounds payloads against the retention-free table.
- `200 OK`: JSON array ordered by `started_at ASC`, each element mirroring the table schema:
  `{ "id", "runId", "sessionId", "requirementId", "stage", "status", "startedAt" (ISO-8601), "durationMs", "detail" }`
- Errors as ProblemDetail (RFC 9457): `400` invalid `limit`; empty result set is a valid `200 []`.

### Code placement

`PipelineEvent`, `PipelineEventRepository`, publisher, executor, tracing context, and
controller live in `com.sda.mapper.observability` (feature-cohesive module; extends the
tech-stack observability module responsibility). Flyway script follows the existing
`db/migration` convention.

## Decisions (confirmed defaults)

- **Both paths instrumented** — acceptance says "one trace per pipeline run", not "per hub run".
- **No new trace backend**: spans/metrics stay in-app; OTLP export remains opt-in via the
  existing `OTEL_EXPORTER_OTLP_ENDPOINT`; Grafana JSON shipped as a file only. Complies
  with AGENTS "no new infrastructure".
- **Audit history via REST only** — no past-runs panel in `SessionOverviewView`.
- **No retention job** — rows kept forever (prototype scale).
- **Always-on** — no feature flag; tracing/metrics/audit active unconditionally so
  acceptance behaviour is deterministic. Sampling already tunable via
  `MANAGEMENT_TRACING_SAMPLING_PROBABILITY`.
- **Dimension tags reuse `ExplanationService` dimensions verbatim**, so trace tags match
  user-facing explanations.
- **Audit write failure must not fail the pipeline** — logged at ERROR, business flow
  continues (observability must not take matching down).
- **Tests need no external collector** — `SimpleMeterRegistry` (ships in micrometer-core)
  for metrics; span-capture assertions use a **test-scoped** tracer utility
  (`micrometer-tracing-test`) — **recommended for approval** since the acceptance
  criterion "one trace per run, one span per stage" cannot be credibly asserted via
  MDC fallback alone. Requires human sign-off per AGENTS dependency rule; if rejected,
  fallback asserts via MDC traceId correlation + metrics (weaker but non-blocking).

## Context

- Deps already present per `specs/tech-stack.md`: `micrometer-registry-prometheus`,
  `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`. `@Observed` needs an
  `ObservedAspect` bean (micrometer-core, transitively present via actuator).
- New code goes in `com.sda.mapper.observability` per the tech-stack module layout;
  domain health indicators stay where they are.
- `PipelineStage` enum has `PARSE/LIFT/INFER/MATCH/MANIFEST`; `MappingHubService` also logs
  `[INSTANTIATE]`. Audit `stage` column is text so both are covered without breaking the enum.
- **Do not regress existing console-line tests** (`[INSTANTIATE]`, `[LIFT]`, `[INFER]`,
  `[MATCH]` prefixes asserted in `MappingHubServiceTest` / `MappingHubViewTest`).
- AGENTS rules apply: metrics for every service-layer method processing requests;
  ProblemDetail for API errors; JavaDoc on new public classes; `@DisplayName` on every
  test class/method.

## Traceability

| Roadmap Phase 21 task | Spec location | Validation check |
|---|---|---|
| `TracedSparqlExecutor` + `@Timed` wired into both reasoner paths | Scope §SPARQL; Plan 3 | Automated: TracedSparql executor assertions |
| `@Observed` on every stage service; one trace per run | Scope §Distributed tracing; Plan 2 | Automated: trace-shape test |
| Baggage `requirement.id`/`session.id`/`run.id` → MDC | Plan 1.2 | Automated: baggage/MDC assertion |
| `PipelineEvent` + Flyway V6 + audit publisher | Data shape; Plan 4.1–4.3 | Automated: publisher tests |
| Historical audit queryability (REST) | Audit API contract; Plan 4.4 | Automated: controller tests; Manual step 2 |
| Per-dimension pass/fail tags from `ExplanationService` | Scope §Dimensional tagging; Plan 5 | Automated: no-match dimension test; Manual step 3 |
| Live console panels in `MappingHubView`, streaming | Scope §Live UI console; Plan 7 | Automated: UI tests; Manual step 1 |
| `sda.pipeline.*` metrics + Grafana JSON (parse-tested) | Scope §Metrics; Plan 6 | Automated: scrape + JSON parse tests; Manual step 4 |

## Out of scope

- Jaeger/Tempo/Prometheus/Grafana servers in docker-compose.
- Retention/cleanup jobs; multi-node trace aggregation.
- Any change to matching semantics — observability is read-only over the pipeline.
- Phase 17 K8s backend / Phase 18 SWRL extension points.

## Artifact placement

All feature artifacts under `specs/2026-08-26-phase21-pipeline-observability/`:
`requirements.md`, `plan.md`, `validation.md`; future reports (`spec-review-report.md`,
`spec-validation-report.md`, `review-report.md`, `validation-report.md`) land here too.
