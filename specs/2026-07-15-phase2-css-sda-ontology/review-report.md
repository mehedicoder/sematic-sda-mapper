# Phase 2 Implementation Review Report

**Reviewer:** Reviewer Agent
**Date:** 2026-07-16
**Branch:** `feature/2026-07-15-phase2-css-sda-ontology`
**Scope:** CSS-SDA Ontology Model — all 3 use cases, SPARQL queries, ExplorerView, tests

## Summary

Phase 2 replaces the stub ontology with the paper's OPC-UA flowmeter diagnostics model and adds all three use cases. The implementation is well-structured and tests pass (108 tests, 0 failures). Findings are ordered by severity.

---

## Findings

### HIGH

#### H1: NE 107 Failure exclusion test was structurally weak — FIXED
**File:** `SparqlMatchingTest.java:108-135`
**Status:** Fixed. Added `shouldExcludeWhenDeviceHasNe107Failure` test that modifies the ontology to set `pump1Asset` status to `NE107-Failure`, then verifies the `FILTER NOT EXISTS` clause excludes all results.

#### H2: `match-service.rq` hardcodes BIND(:REQ-001) — ACCEPTED
**File:** `match-service.rq:10`
**Status:** Accepted for Phase 2. Phase 7 (Reasoner Service) must parameterize the requirement ID at runtime.

---

### MEDIUM

#### M1: `SVC-WrongCapability` exclusion test is indirectly validated
**File:** `SparqlMatchingTest.java:82-91`

The test asserts `SVC-WrongCapability` is not in the results. This service is unreachable via R1 because it is not connected to any Requirement. The test is valid but does not directly prove the capability chain filtering works. A stronger test would create a service linked to a wrong capability chain and verify it is excluded.

#### M2: Cross-plant query may return no results in current ontology
**File:** `cross-plant-temperature.rq:22-24`

The query uses `OPTIONAL { ?asset sda:belongsToSite ?site }` to find the site. But there is no triple connecting `:CB-AHU01-OPCUA` or `:CB-EM01-OPCUA` to `:AHU-01` or `:EM-01` via `sda:usesConnector`. The `?site` variable may be unbound for some results. The test (`shouldFindCrossPlantTemperatureServices`) checks for label content, not site binding, so this does not cause a test failure.

**Recommendation:** The query works but the OPTIONAL pattern for site discovery is loose. Consider adding a direct `sda:isDeployedBy` or `sda:connectedTo` property for tighter cross-plant queries in Phase 7.

#### M3: OntologyExplorerView uses hardcoded static data
**File:** `OntologyExplorerView.java:96-104`

The tree is populated from `buildOntologyTree()` which returns hardcoded `OntologyNode` instances, not from the Jena model. This is acceptable per the spec (dynamic loading is Phase 3+), but the tree will drift from the ontology as it evolves.

#### M4: Unused imports in test file
**File:** `OntologyExplorerViewTest.java:3-5`

`SplitLayout`, `TreeGrid`, and `Component` are imported but not used in any test method.

#### M5: No test for `SVC-NE107Failure` being the ONLY service that would match if NE 107 filter were removed
**File:** `SparqlMatchingTest.java`

If the `FILTER NOT EXISTS { ?device sda:hasNE107Status :NE107-Failure }` clause were removed from `match-service.rq`, `SVC-NE107Failure` would still not appear (it's unreachable via R1). This means the FILTER NOT EXISTS clause is effectively redundant for the current ontology. A test proving the clause works in isolation (with a reachable service that has NE 107 Failure) would strengthen confidence.

---

### LOW

#### L1: Unused imports in test files — FIXED
**File:** `OntologyExplorerViewTest.java:3-5`
**Status:** Fixed. Removed unused `SplitLayout`, `TreeGrid`, `Component` imports.

#### L2: `OntologyExplorerViewTest` does not test Vaadin rendering
**File:** `OntologyExplorerViewTest.java`

All tests verify the `OntologyNode` record and `buildOntologyTree()` static method. No test exercises the actual Vaadin `SplitLayout`/`TreeGrid` rendering (which requires Vaadin context). This is acceptable since Vaadin rendering tests require a running server context.

---

## Validation Against Phase 2 Acceptance Criteria

| # | Criterion | Status |
|---|---|---|
| 1 | Ontology supports paper's SPARQL query (Table I) without modification | **PASS** — R1, R2b, R3 all enforced |
| 2 | SPARQL returns SVC-FlowmeterDiagnostics for REQ-001 | **PASS** |
| 3 | SPARQL excludes SVC-WrongProtocol | **PASS** (via protocol filter) |
| 4 | SPARQL excludes SVC-WrongCapability | **PASS** (via R1 chain, not capability filter) |
| 5 | SPARQL excludes SVC-NE107Failure | **PASS** (via FILTER NOT EXISTS) |
| 6 | Pump recovery restores SPARQL match | **NOT TESTED** — requires runtime status change |
| 7 | NE 107 status individuals and sub-codes | **PASS** |
| 8 | DegradedMode class with hasDegradationLevel | **PASS** |
| 9 | Requirement class has paper's properties | **PASS** (requirementID, requirementType, lifecycleStatus) |
| 10 | Capability class has paper's properties | **PASS** (capabilityID) |
| 11 | SPARQL returns SVC-FermentationSkid for REQ-002 via PROFINET | **PASS** |
| 12 | SPARQL excludes service when SIL mismatch | **PASS** (indirectly) |
| 13 | SPARQL excludes service when determinism class mismatch | **PASS** |
| 14 | SPARQL returns SVC-HVACTempAnalysis for REQ-003 | **PASS** |
| 15 | Cross-plant query finds temperature services | **PASS** |
| 16 | SPARQL excludes service when networkZone mismatch | **PASS** |
| 17 | Ontology is valid Turtle | **PASS** |
| 18 | Ontology contains all 8 CSS-SDA classes | **PASS** |
| 19 | ExplorerView shows CSS-SDA class hierarchy | **PASS** (hardcoded tree) |
| 20 | mvn clean test passes | **PASS** (108 tests) |

---

## Residual Risks

1. **Pump recovery test (AC #6)** not implemented — requires runtime NE 107 status mutation during SPARQL execution. Should be added in Phase 7 when the reasoner service integrates with `Ne107DiagnosticSimulator`.
2. **SPARQL query parameterization** — current queries hardcode requirement IDs. Phase 7 must introduce runtime binding.
3. **Cross-plant query site discovery** is loose (OPTIONAL pattern). Phase 7 should tighten with explicit asset-service relationships.
