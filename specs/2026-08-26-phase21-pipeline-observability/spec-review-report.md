# Spec Review Report — Phase 21: End-to-End Pipeline Observability

**Reviewed:** 2026-08-26 · **Reviewer:** Reviewer Agent · **Branch:** `feature/2026-08-26-phase21-pipeline-observability`
**Artifacts:** `requirements.md`, `plan.md`, `validation.md`

## Checklist Results

| # | Check | Result |
|---|---|---|
| 1 | Artifact completeness | PASS — all three spec files present; report artifacts will land in-folder |
| 2 | Scope clarity | PASS — in/out-of-scope explicit; maps to roadmap Phase 21; both pipeline paths covered |
| 3 | API & contract completeness | PARTIAL — see F2 |
| 4 | Plan implementability | PASS — 8 groups sequenced foundation → instrumentation → audit → UI → tests |
| 5 | Validation executability | PARTIAL — see F1, F3 |
| 6 | Traceability | PARTIAL — mapping to roadmap tasks implicit only — see F4 |

No mission mismatch, no unauthorized scope expansion, no semantic-matching logic moved into Java (observability is read-only over existing services). Flyway numbering (V6) verified against `db/migration/` (V1–V5 exist).

## Findings

### F1 — MEDIUM: "@Push streaming" not achievable as written
Roadmap and spec say `@Push`, but the codebase has **zero** push usage:
`OpcUaVisualizationView.java:340–354` uses a Vaadin `Timer` + `UI.access`;
`SessionOverviewView.java:321–326` uses a `ScheduledExecutorService` + `UI.access`;
`application.yml` configures no Vaadin push (Flow default = DISABLED). With push
disabled, `UI.access` updates reach the client only at the next client-server
interaction — so the acceptance criterion *"live stage timeline updating during a run
without manual refresh"* would fail if the Builder copies the existing pattern verbatim.
**Required:** spec must explicitly include enabling push (`@Push` on `MappingHubView`,
or `PushConfiguration` AUTOMATIC) alongside the timer/`UI.access` delivery mechanism.
Vaadin 25 is already in the stack — no new dependency.

### F2 — MEDIUM: Audit REST contract under-specified
`GET /api/v1/pipeline-audits` defines query params but no response DTO shape, no
ordering guarantee in `requirements.md`, and no result bound — while the table is
explicitly "kept forever" (no retention). **Required:** define response fields
(mirroring the table schema), ordering (`started_at ASC`), and a `limit` parameter
(default ~200) so the endpoint cannot return unbounded payloads.

### F3 — LOW / decision: test-scoped tracer dependency undecided
Acceptance requires asserting *one trace per run, one child span per stage*.
The spec's fallback (MDC correlation + metrics) verifies correlation but cannot
assert span topology credibly. Recommend approving `micrometer-tracing-test`
(test scope only) per AGENTS dependency rule; needs explicit human sign-off before
implementation. Non-blocking because a fallback exists.

### F4 — LOW: traceability implicit
Requirements mirror roadmap Phase 21 tasks but no explicit requirement→roadmap /
validation→requirement mapping exists. Add a small traceability table.

### F5 — LOW: entity placement unspecified
Existing JPA/persistence code lives in `hub/persistence` (e.g. `InstanceTriple`).
State where `PipelineEvent` + repository live (`hub/persistence` vs new
`observability` sub-package) to keep layering consistent with `specs/tech-stack.md`.

## Spec Gaps Blocking Builder/Validator

- **F1** blocks implementation of plan group 7 / manual validation step 1 as written
  (the criterion would fail at runtime).
- **F2** blocks implementation of plan group 4.4 and `PipelineAuditControllerTest`.
- F3, F4, F5 are non-blocking but should be resolved before implementation begins.
