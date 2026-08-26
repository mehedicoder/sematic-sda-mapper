# Implementation Review Report — Phase 21: End-to-End Pipeline Observability

**Reviewed:** 2026-08-26 · **Reviewer:** Reviewer Agent · **Branch:** `feature/2026-08-26-phase21-pipeline-observability`
**Scope:** 6 modified files (+482/−127), 10 new main classes, 7 new test classes, Flyway V6, Grafana JSON.
**Build evidence at review time:** `mvn clean test` — 553 tests, 0 failures (baseline 536).

## Verified Against Review Priorities

| Check | Result |
|---|---|
| No matching logic moved into Java / no hardcoded service selection | PASS — observability wraps results; `MatchDimensionTagger` classifies after the fact |
| SPARQL semantics unchanged by `TracedSparqlExecutor` | PASS — pure measurement wrapper; query strings untouched (`match-generic.rq`, `hub-match-offered-required.rq`) |
| ProblemDetail (RFC 9457) for API errors | PASS — `PipelineAuditController` @ExceptionHandler → 400 ProblemDetail |
| Micrometer metrics on request-processing service methods | PASS (see F3 for one parity gap) |
| Existing console-line contracts preserved | PASS — `MappingHubServiceTest` `[INSTANTIATE]/[LIFT]/[INFER]/[MATCH]` assertions green |
| JavaDoc on new public classes/methods | PASS |
| `@DisplayName` on all new tests | PASS |
| Positive + negative tests per feature | PARTIAL — see F2/F4 |
| Vaadin UI tests for new components | PASS — `MappingHubViewObservabilityTest` (5 tests) + existing view tests green |
| Flyway migration versioning | PASS — V6 next in sequence; DDL verified H2-compatible via controller/publisher tests |

## Findings

### F1 — HIGH: dimension tags promised on match spans are only in audit text
`SemanticReasonerService.matchByRequirementId` calls
`dimensionTagger.failureSummary(result)` and writes it into the stage audit detail,
but the pass/fail values are **never attached as span attributes**
(`match.dimension.capability`, …). The roadmap acceptance criterion *"Per-dimension
pass/fail tags … on match spans"* and validation check *"no-match trace exposes which
dimension failed"* are therefore not fully met.
**Required:** attach each dimension entry as a key-value on the MATCH observation;
assert their presence in a test.

### F2 — HIGH: baggage/MDC correlation promised by validation.md is untested
`validation.md` requires assertions that baggage attributes are present and MDC carries
correlation ids during stage execution. Neither is asserted anywhere. The mechanism is
implemented (`PipelineTracingContext`) but its core promise — log/trace correlation per
run — has zero coverage.
**Required:** extend the trace-shape capture to assert `MDC.runId` and OTel baggage
`run.id` while a stage observation starts.

### F3 — LOW/MEDIUM: reasoning-path success outcome metric missing
`metrics.recordRun("reasoning", "failure")` is recorded on exceptions but success runs
are never counted, so `sda.pipeline.runs{path=reasoning,outcome=success}` stays empty and
the Grafana failure-rate panel misreads for the REST path.
**Required:** record success on normal completion of `matchByRequirementId`.

### F4 — LOW: unused parameter
`EndToEndPipelineService.firstRequirement(List<String>, Map<String,String>)` ignores its
second parameter. Remove it or use it (a newly created inline requirement could seed
baggage `requirement.id`).
**Recommended:** drop the parameter (keep scope minimal).

### F5 — LOW: `PipelineTracePanel.appendOutcome` string surgery
The panel rewrites rendered text by index arithmetic on `StringBuilder`; fragile if stage
names ever repeat inside a line. UI-only cosmetic risk; acceptable for prototype with a
comment/naming note.

## Non-blocking observations

- Legacy constructors duplicate noop-wiring boilerplate in three services — acceptable
  for test ergonomics; revisit if a fourth appears.
- `PipelineAuditRepository.find` builds SQL by concatenation of fixed fragments with
  parameterized values only — safe.
- `SdaAppShell` enables push application-wide (documented deviation from per-view push,
  forced by Vaadin 25 constraint); OPC UA monitor benefits too.

## Required Before Validation

F1, F2, F3 must be fixed and demonstrated by tests. F4 recommended.

## Builder Resolution (re-review 2026-08-26)

| Finding | Fix | Evidence |
|---|---|---|
| F1 dimension tags on spans | `PipelineStageObserver.Stage.attribute(key, value)` added; `SemanticReasonerService` attaches every `match.dimension.*` entry to the MATCH observation | New `MatchDimensionSpanTagTest` asserts all 7 dimensions tagged `pass` on the captured MATCH span |
| F2 baggage/MDC untested | `PipelineTracingContext.openRun` now establishes baggage + MDC **before** starting the root observation; capture handler snapshots both at every observation start | New test `shouldCorrelateRunThroughMdcAndBaggage` — all stage starts see non-blank MDC `runId`, OTel baggage `run.id` identical to the run scope's id |
| F3 missing success metric | `metrics.recordRun("reasoning", "success")` recorded on normal completion of `matchByRequirementId` | Suite green; counter exercised in reasoning-path tests |
| F4 unused parameter | `EndToEndPipelineService.firstRequirement` reduced to single parameter | Compilation clean |

**Re-review verdict:** findings closed. Final suite: `mvn clean test` — **555 tests, 0 failures, 0 errors**.
Residual (non-blocking): F5 cosmetic panel string surgery; manual walkthrough steps of
validation.md still pending execution by the Validation Agent / human.
