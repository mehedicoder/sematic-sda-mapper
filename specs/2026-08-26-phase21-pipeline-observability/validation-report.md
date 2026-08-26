# Validation Report — Phase 21: End-to-End Pipeline Observability

**Validated:** 2026-08-26 · **Validator:** Validation Agent · **Branch:** `feature/2026-08-26-phase21-pipeline-observability`

## Commands Run

| Command | Result |
|---|---|
| `git status --short` / `git diff --stat` | PASS — 6 modified files (+485/−127), new observability package/tests/Flyway V6/Grafana JSON; no unrelated changes |
| `mvn clean test` (final, after all fixes) | **PASS — 556 tests, 0 failures, 0 errors** |
| `docker compose up -d --build` (postgres, nodered, app) | PASS — image built; app healthy (`/actuator/health` = UP) |
| Flyway V6 on real PostgreSQL | PASS — `flyway_schema_history`: version 6 "pipeline audit log", success=t; `\d pipeline_audit_log` matches the requirements schema exactly |
| Live match: `POST /api/v1/requirements/match {"requirementId":"REQ-001"}` (authenticated form-login session) | PASS — `matched=true`, service "Flowmeter Diagnostics Service" |
| Live history: `GET /api/v1/pipeline-audits?requirementId=REQ-001&limit=50` | PASS — chronological STARTED/SUCCEEDED pairs with non-null run ids and human-readable detail |
| Live E2E: `POST /api/v1/pipeline/execute?requirementIds=REQ-001` (pump-aas.json upload) | PASS — fileType=AAS, parsedTriples=17, inferredTriples=964, elapsedMs=356 |
| Prometheus scrape `/actuator/prometheus` | PASS — `sda_pipeline_runs_total{outcome="success",path="reasoning"} 1`, `sda_pipeline_match_result_total{result="hit"} 1`, `sda_sparql_query_seconds{query="queries/match-generic.rq"}`, `sda_sparql_results_total{query=…} 1` |

## Defect Found and Fixed During Live Validation

**V1 — HIGH (found by live run, missed by unit tests):** reasoning/E2E paths passed
`runId=null` into `PipelineStageObserver.begin(...)`, so every audit insert violated
`pipeline_audit_log.run_id NOT NULL`. The publisher's failure-swallowing design kept the
pipeline fully functional (match still returned `matched=true`) while silently dropping
audit rows — container logs showed exactly 2 failures for MATCH and 6 for PARSE/LIFT/INFER.

**Fix:** `SemanticReasonerService.matchByRequirementId` and
`EndToEndPipelineService.execute` now generate a real run UUID and pass it to every
stage observer call. Regression test added
(`PipelineAuditControllerTest.shouldPersistReasonerMatchTransitions`) that runs the real
reasoner against a real database and asserts two persisted rows with non-null run ids.

**Post-fix live evidence (single E2E run `6583ef36…`):**

```
PARSE | SUCCEEDED |   45ms | run=6583ef36
LIFT  | SUCCEEDED |    2ms | run=6583ef36
INFER | SUCCEEDED |   59ms | run=6583ef36
MATCH | SUCCEEDED |  219ms | req=REQ-001 | run=843abe71 (per-requirement run)
```

One trace per run confirmed: all three pipeline stages share one run id; each
requirement match is its own correlated run (documented semantics).

## Acceptance Criteria Results

| Roadmap Phase 21 criterion | Status | Evidence |
|---|---|---|
| One trace per run, one span per stage, correlated by run.id in logs | PASS | `PipelineObservabilityTraceShapeTest` (root + parented children); MDC/baggage asserted per stage start |
| Historical audit queryability by session and requirement | PASS | Controller tests + live REST query above against real PostgreSQL |
| Live stage timeline in MappingHubView without refresh (`@Push`) | PARTIAL (automated only) | Component tests + `SdaAppShell` push annotation verified; browser walkthrough remains for human review |
| No-match exposes failing dimension, consistent with explanation | PASS | `MatchDimensionTaggerTest` + `MatchDimensionSpanTagTest`; audit detail carries failed-dimension summary |
| Prometheus SLIs + parseable Grafana JSON | PASS | Live scrape above; `GrafanaDashboardJsonParseTest` |
| Full suite green with positive/negative observability tests | PASS | 556 tests, 0 failures (17→19 new tests across review/validation fixes) |

## Blocked Items

None.

## Residual Risks

1. **Browser-level UI walkthrough not executed headlessly** — timeline/console rendering
   under a real WebSocket push session should be eyeballed once by the human
   (`MappingHubView` → Run Pipeline). Automated coverage verifies components and push config.
2. Hub-path audits were validated at unit/integration level and via shared observer code;
   the live walkthrough exercised the reasoning path end-to-end.
3. Each requirement inside one E2E execution gets its own run id (MATCH nested runs).
   Documented as intended semantics; revisit if a single-trace-per-upload requirement emerges.
4. Grafana dashboard visual layout is syntax-validated only (no live Prometheus/Grafana in CI).

## Verdict

**PASS** — Phase 21 implementation validated against roadmap acceptance criteria with
automated and live evidence. Recommended: mark roadmap/tasks.md complete with this
report as evidence, followed by human final review and commit decision.
