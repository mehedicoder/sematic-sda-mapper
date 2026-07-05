# Agent Workflow

This project uses a role-based workflow for the Semantic SDA Mapper prototype.

Agents are separated by responsibility, not by runtime environment. The human engineer remains the architect and final decision maker. Agents implement, review, and validate the approved roadmap for a lightweight semantic requirement-to-service mapping system.

---

## Project Mission

Build a small executable prototype that maps industrial automation requirements to deployable software services using:

- RDF/Turtle ontology data (CSS-SDA model: Capability, Skill, CommunicationBinding, DataConnector, DeploymentContext)
- SPARQL matching rules
- Apache Jena reasoning/query execution
- Java 25 and Spring Boot 4.0.x
- Input file parsers (MTP AML, OPC UA Server Datasheet, AAS)
- OWL reasoner inference for capability derivation
- REST API for requirement matching
- Deterministic Docker Compose and Kubernetes manifest generation
- Unit and integration tests proving match and rejection behavior

Target use cases:
- Flowmeter Asset-Health Monitoring for requirement `REQ_001`
- Fermentation Skid (Skid-A1) via MTP AML input — critical CPC, SIL-2
- HVAC Plant (AHU-01 + EM-01) via OPC UA Server Datasheet input — non-critical M+O
- Cross-plant semantic queries across both use cases

The five-step mapping procedure is: **Parse → Instantiate → Lift → Infer → Match**.

---

## Roles

1. Human Architect
2. Builder Agent
3. Reviewer Agent
4. Validation Agent
5. Specialist subagents under `prompts/subagents/`

---

## Human Architect

The Human Architect owns all architecture and production decisions.

The Human Architect decides:

- System architecture and service boundaries
- Ontology modeling direction beyond the approved specs
- REST API contract changes
- Deployment strategy
- Security boundaries
- External infrastructure decisions
- Whether optional roadmap phases are included
- Final approval
- Commit, push, merge, and deployment decisions

Agents may identify risks, suggest alternatives, or request clarification. Agents must not silently change the architecture or expand scope.

---

## Required Context Files

Before working, agents should read the relevant project documents:

- `specs/mission.md`
- `specs/tech-stack.md`
- `specs/roadmap.md`
- `specs/architecture.md` if available
- `specs/api-contract.md` if available
- `specs/local-development.md` if available
- `tasks.md` if available
- Active feature folder under `specs/YYYY-MM-DD-<feature-name>/` if available

Feature folders may contain:

- `requirements.md`
- `plan.md`
- `validation.md`
- `spec-review-report.md`
- `spec-validation-report.md`
- `review-report.md`
- `validation-report.md`

---

## Builder Agent

The Builder Agent implements human-approved roadmap tasks one vertical slice at a time.

Main outputs:

- Java source under `src/main/java/**`
- Tests under `src/test/java/**`
- RDF/Turtle ontology files under `src/main/resources/ontology/**`
- SPARQL queries under `src/main/resources/queries/**`
- Input file parsers (MTP AML, OPC UA Server Datasheet) under parser packages
- OWL reasoner inference service
- Docker Compose manifests under `docker/examples/`
- Kubernetes templates/examples under `k8s/examples/`
- Maven and Docker configuration
- Documentation and `tasks.md`

Builder must keep semantic matching logic in ontology data and SPARQL where it belongs. Java may orchestrate loading, query execution, DTO conversion, validation, and manifest generation, but must not hardcode service selection rules.

---

## Reviewer Agent

The Reviewer Agent reviews implementation and feature specs like a senior backend engineer focused on correctness, maintainability, and research-demo credibility.

Main outputs:

- Spec review: `specs/YYYY-MM-DD-<feature-name>/spec-review-report.md`
- Implementation review: `specs/YYYY-MM-DD-<feature-name>/review-report.md`

Reviewer checks:

- Roadmap and spec alignment
- Bugs and behavioral regressions
- Ontology consistency and valid Turtle syntax
- SPARQL correctness and explainability
- Java/Spring layering
- Jena resource handling and error behavior
- REST request/response behavior
- Kubernetes YAML determinism and validity
- Test coverage for positive and negative matching scenarios
- Unauthorized architecture or scope changes

---

## Validation Agent

The Validation Agent verifies that the implementation works correctly and that acceptance criteria are satisfied with evidence.

Main outputs:

- Spec validation: `specs/YYYY-MM-DD-<feature-name>/spec-validation-report.md`
- Implementation validation: `specs/YYYY-MM-DD-<feature-name>/validation-report.md`

Validator checks:

- `mvn clean test`
- `mvn clean verify` when applicable
- Application startup
- `/actuator/health`
- REST API behavior
- Ontology loading
- SPARQL query behavior
- Kubernetes YAML parsing
- Docker image and Docker Compose behavior when the phase requires it
- Roadmap acceptance criteria

Validation must distinguish passed, failed, and blocked items. Do not mark roadmap items complete without evidence.

---

## Specialist Subagents

Specialist prompts live in `prompts/subagents/` and may be used by Reviewer or Validation workflows when the topic applies:

- `spec-reviewer.md`: feature-spec completeness and implementability
- `spec-validation.md`: feature-spec validation readiness
- `spring-boot-reviewer.md`: Java/Spring Boot/API/layering review
- `security-reviewer.md`: API, dependency, and generated-manifest security review
- `concurrency-reviewer.md`: lifecycle, caching, thread-safety, and Jena model sharing review
- `sparql-optimizer.md`: SPARQL/Jena query performance review
- `test-coverage-reviewer.md`: test strategy and acceptance-criteria coverage review
- `mtp-parser-reviewer.md`: MTP AML parsing, VDI/VDE/NAMUR 2658 schema review
- `uaserver-datasheet-reviewer.md`: OPC UA Server Datasheet parsing, IDTA 02009-1-0 review
- `ontology-inference-reviewer.md`: OWL reasoner inference and OfferedCapability derivation review
- `k8s-manifest-reviewer.md`: Kubernetes YAML generation and nodeSelector review
- `rdf-ontology-reviewer.md`: Turtle/RDF ontology consistency and property alignment review

---

## Standard Workflow

1. Human selects or approves a roadmap task.
2. Builder Agent creates a feature specification using `skills/feature-spec` when requested or required by workflow.
3. Reviewer Agent reviews the feature spec and writes `spec-review-report.md` in the feature folder.
4. Builder Agent addresses required spec review findings.
5. Reviewer Agent re-reviews the feature spec when needed.
6. Validation Agent validates the feature spec and writes `spec-validation-report.md`.
7. Builder Agent addresses validation blockers.
8. Builder Agent implements the approved task.
9. Builder Agent updates `tasks.md` and roadmap markers only when supported by evidence.
10. Reviewer Agent reviews the latest git diff.
11. Builder Agent fixes required review findings.
12. Validation Agent validates tests and acceptance criteria.
13. Human performs final review.
14. Human decides whether to commit, push, merge, or deploy.

---

## Git Rules

- Work should happen on a feature branch.
- Agents must not push code unless the human explicitly asks.
- Agents must not commit automatically unless the human explicitly asks.
- Agents must not merge code.
- Agents must not rewrite git history unless explicitly requested.
- Agents should keep changes small and reviewable.
- Agents should avoid unrelated file changes.

---

## Roadmap Rules

`specs/roadmap.md` tracks planned work and high-level status.

Use this marker style:

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS] Currently being worked on
- [ ] [BLOCKED] Blocked by dependency, missing decision, or environment issue
```

Only mark a task completed when its acceptance criteria are validated or the human explicitly instructs the update.
