# Feature Spec: Paper Experiment Completion

**Date:** 2026-07-17
**Paper:** "A Semantic Approach towards Software-Defined Automation" (Islam, Maurer, Diedrich 2026)
**Branch:** `feature/2026-07-17-paper-experiment-completion`

## Goal

Complete the experiment described in Section 4.4 of the paper by implementing all missing ontology elements and Java equivalents of SWRL rules R-Deploy and R-Verify, so the full Use Case A (Flowmeter Asset-Health Monitoring based on NE 107) can be demonstrated end-to-end.

## Paper Rules to Implement

| Rule | Type | Status | Description |
|---|---|---|---|
| R1 | SPARQL | ✅ Done | Capability match chain |
| R2a | SPARQL | ❌ Missing | Semantic identifier matching (requiresConcept) |
| R2b | SPARQL | ✅ Done | Communication protocol match |
| R3 | SPARQL | ✅ Done | Performance + deployment constraints |
| R-Deploy | SWRL→Java | ❌ Missing | Assert bound* triples, lifecycleStatus="Matched" |
| R-Verify | SWRL→Java | ❌ Missing | Verify criteria, lifecycleStatus="Verified" |

## Gaps to Close

### G1: UseCase Individual
- Add `:UseCase-FlowmeterNE107` individual (type `css:UseCase`)
- Add `sda:hasRequirement :REQ-001` object property
- **File:** `css-sda-small.ttl`

### G2: Requirement Parameters and Constraints
- Add `sda:RequirementParameter` class
- Add `sda:RequirementConstraint` class
- Add instances: `:REQ-001-Param-MonitoringInterval`, `:REQ-001-Constraint-MaxLatency`
- Wire to REQ-001 via `sda:hasParameter` and `sda:hasConstraint`
- **File:** `css-sda-small.ttl`

### G3: VerificationCriterion
- Add `sda:VerificationCriterion` class
- Add instances: `:VC-REQ001-RecallCheck`, `:VC-REQ001-LatencyCheck`
- Wire to REQ-001 via `sda:verifiedBy`
- **File:** `css-sda-small.ttl`

### G4-G7: R-Deploy Java Equivalent
- Create `RDeployService` Spring service
- After SPARQL match succeeds:
  - Assert `sda:boundService(?req, ?service)` triple
  - Assert `sda:boundBinding(?req, ?binding)` triple
  - Assert `sda:boundConnector(?req, ?connector)` triple
  - Assert `sda:boundContext(?req, ?deployContext)` triple
  - Assert `sda:matchedCandidate(?req, ?service)` triple
  - Update `sda:lifecycleStatus` from "Defined" to "Matched"
- **File:** `src/main/java/com/sda/mapper/reasoning/RDeployService.java`

### G4-G7: R-Verify Java Equivalent
- Create `RVerifyService` Spring service
- After deployment:
  - Read `sda:verifiedBy` → VerificationCriterion instances
  - Evaluate each criterion against actual service metrics
  - If all criteria satisfied: update `sda:lifecycleStatus` to "Verified"
- **File:** `src/main/java/com/sda/mapper/reasoning/RVerifyService.java`

### G8: R2a Semantic Concept Matching
- Add `sda:requiresConcept` object property (Skill → Concept)
- Add `sda:providesConcept` object property (DataConnector → Concept)
- Add PA-DIM concept individuals: `:PA-DIM-FlowConcept`, `:PA-DIM-TemperatureConcept`
- Add `sda:Concept` class
- Wire SK-FlowmeterDiagnostics → requiresConcept → :PA-DIM-FlowConcept
- Wire DC-Flowmeter-FT101 → providesConcept → :PA-DIM-FlowConcept
- Update SPARQL queries with NOT EXISTS filter from paper
- **Files:** `css-sda-small.ttl`, `match-service.rq`, `match-service-req002.rq`, `match-service-req003.rq`

## Testing Requirements

### Unit Tests
- `RDeployServiceTest`: assert bound* triples, lifecycleStatus transition, matchedCandidate
- `RVerifyServiceTest`: criterion evaluation, lifecycleStatus "Verified" when all pass, stays "Matched" if any fail
- Ontology loading tests: new classes and individuals load without error

### Integration Tests
- Full lifecycle: REQ-001 starts as "Defined" → SPARQL match → R-Deploy → "Matched" → R-Verify → "Verified"
- R2a: Skill with requiresConcept matches DataConnector with providesConcept
- R2a: Skill with requiresConcept NOT PROVIDED by any DataConnector → no match (NOT EXISTS)

### SPARQL Tests
- R2a NOT EXISTS filter works correctly
- R1-R3 still work after adding requiresConcept

### UI Tests
- Lifecycle status displayed in requirement matching results
- Bound properties shown in match response

## Acceptance Criteria

- [ ] All 8 CSS-SDA classes have instances (UseCase has at least 1 individual)
- [ ] REQ-001 has parameters and constraints populated
- [ ] VerificationCriterion class and instances exist
- [ ] R-Deploy writes bound* triples and sets lifecycleStatus="Matched"
- [ ] R-Verify evaluates criteria and sets lifecycleStatus="Verified"
- [ ] R2a requiresConcept filter works in SPARQL
- [ ] All existing 253 tests still pass
- [ ] New tests added for every new feature
- [ ] `mvn clean test` passes
