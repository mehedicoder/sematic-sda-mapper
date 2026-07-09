# Validation Agent

Verify behavior, tests, acceptance criteria, and execution readiness. You validate evidence; you do not design architecture or approve scope changes.

Primary output locations:
- Spec validation: `specs/YYYY-MM-DD-<feature-name>/spec-validation-report.md`
- Implementation validation: `specs/YYYY-MM-DD-<feature-name>/validation-report.md`

## Validation Commands

```bash
git status
git diff --stat
mvn clean test
mvn clean verify
mvn spring-boot:run
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus  # metrics exposed
curl http://localhost:8080/actuator/health  # check custom health indicators (ontology, sparql, opcua)
docker build -t semantic-sda-mapper:local -f docker/Dockerfile .
docker compose up --build

# Parser tests (Phase 4)
mvn test -Dtest="MTPAmlParserTest,UaServerDatasheetParserTest"

# Inference tests (Phase 6)
mvn test -Dtest="OntologyInferenceServiceTest"

# Vaadin UI tests
mvn test "-Dtest=com.sda.mapper.ui.*Test"

# Error handling validation
curl -X POST http://localhost:8080/api/v1/requirements/match -H "Content-Type: application/json" -d "{}"  # expects 400 with ProblemDetail
curl -X POST http://localhost:8080/api/v1/requirements/match -H "Content-Type: application/json" -d "{\"requirementId\":\"NONEXISTENT\"}"  # expects 200 with matched=false
curl -s http://localhost:8080/actuator/health  # ensure app hasn't crashed after error tests

# Validate that custom exceptions (OntologyLoadingException, SparqlExecutionException, MatchingException)
# are caught by GlobalExceptionHandler and return ProblemDetail, not raw 500s.
# Run specific unit tests covering exception scenarios:
mvn test -Dtest="*ExceptionHandler*" 2>/dev/null || echo "Using global tests for exception coverage:"
mvn test -Dtest="SemanticReasonerServiceTest#testOntologyLoadFailure" 2>/dev/null || true

# Flyway migration validation
mvn flyway:info && mvn flyway:validate

# YAML validation (Phase 9)
python -c "import yaml; yaml.safe_load(open('docker/examples/...'))"
python -c "import yaml; yaml.safe_load(open('k8s/examples/...'))"
```

Do not claim a command passed unless you ran it or the human provided trustworthy evidence.

## Phase-Specific Validation Targets

| Phase | Validate |
|---|---|
| 1 | Build, startup, Actuator health, Prometheus metrics, custom health indicators, structured logging with trace IDs, Dockerfile, Docker Compose, OPC UA simulation |
| 2-3 | Turtle validity, ontology loading, error handling, ExplorerView tree and tabs |
| 4 | Parser correctness (valid/malformed/invalid input), triple generation, UploadView |
| 5 | SPARQL match for REQ_001, reject wrong capability/protocol/latency/SIL/zone |
| 6 | OWL inference derives OfferedCapability, preserves explicit triples, parameter sensitivity |
| 7 | Match/no-match result without Java hardcoding, metadata extraction |
| 8 | REST validation, match/no-match response, error codes |
| 9 | Docker Compose + K8s YAML validity, nodeSelector from DeploymentContext |
| 10 | Explanation for match and no-match responses |
| 11 | Evaluation dataset (fermentation + HVAC), cross-plant queries, positive/negative tests |
| 13 | GraphView, SettingsView, accessibility, mobile QA, Vaadin integration tests |
| 14+ | Margo Sandbox, optional phases as defined in roadmap |

## Feature-Spec Validation Mode

Validate execution readiness:

1. **Artifact completeness** — `requirements.md`, `plan.md`, `validation.md` exist.
2. **Requirement readiness** — scope explicit, decisions concrete, API testable.
3. **Plan readiness** — tasks actionable, aligned with roadmap phase.
4. **Validation quality** — commands concrete, checks observable, DoD measurable.
5. **Traceability** — requirements map to roadmap/validation, review findings addressed.

Mark `BLOCKED` for missing artifacts, contradictory instructions, non-testable criteria, or missing validation for core flows.

## Output Rules

Report: commands run and result, acceptance criteria passed/failed, blocked items, residual risks.
- Spec validation → `spec-validation-report.md` with `Spec Validation Gaps Blocking Builder/Validator`.
- Implementation validation → `validation-report.md`.
