# AGENTS.md

This repository uses a human-led, role-based agent workflow for the Semantic SDA Mapper project.

There is no Architect Agent. The human engineer is the architect and final decision maker.

---

## Project Mission

Semantic SDA Mapper is a lightweight prototype that maps industrial automation requirements to deployable software services.

The project demonstrates this five-step semantic mapping procedure:

```text
Input Files (MTP AML, OPC UA Server Datasheet, AAS)
→ Parse
→ Instantiate (CSS-SDA ontology ABox triples)
→ Lift (MTP Service → css:Skill)
→ Infer (OWL reasoner derives OfferedCapability)
→ Match (SPARQL multi-dimensional query)
→ Docker Compose / Kubernetes Manifest
```

The CSS-SDA model: SDA-Service := ⟨ Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext ⟩

The MVP uses:

- Java 25
- Spring Boot 4.0.x
- Maven
- Apache Jena (RDF + SPARQL + OWL inference)
- RDF/Turtle ontology files
- MTP AML parser (VDI/VDE/NAMUR 2658)
- OPC UA Server Datasheet parser (IDTA 02009-1-0)
- OWL reasoner for capability inference
- SPARQL matching queries including cross-plant queries
- REST API endpoints
- Docker Compose and Kubernetes YAML generation
- JUnit 5, AssertJ, and Spring Boot Test
- Docker and Docker Compose for local execution
- Node-RED for OPC UA server simulation

The target use cases are:

- **Flowmeter Asset-Health Monitoring** for requirement `REQ_001`
- **Fermentation Skid (Skid-A1)** — critical CPC, SIL-2, MTP AML input
- **HVAC Plant (AHU-01 + EM-01)** — non-critical M+O, OPC UA Server Datasheet input

---

## Human-Owned Decisions

The human engineer owns:

- System architecture and service boundaries
- Ontology modeling direction beyond the approved specs
- REST API contract changes
- Security boundaries
- External infrastructure decisions
- Deployment strategy
- Optional roadmap phase inclusion
- Final review
- Commit, push, merge, and deployment decisions

Agents may suggest improvements, risks, or alternatives, but must not make major architecture or scope decisions without explicit human approval.

---

## Required Project Context

Before doing work, read the relevant project documents:

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
- `prompts.md` if prompt logging is used for that feature

---

## Role Prompt Files

Role-specific prompts live under `prompts/`.

Use the correct role prompt when the human explicitly activates a role or when the task clearly matches a role.

### Workflow Guide

Use for overall workflow rules:

- `prompts/agent-workflow.md`

### Builder Agent

Use for implementation work:

- `prompts/builder-agent.md`

Builder Agent tasks include:

- Feature implementation
- Bug fixes
- Java source changes
- Tests
- RDF/Turtle ontology files
- SPARQL query files (including cross-plant queries)
- Input file parsers (MTP AML, OPC UA Server Datasheet)
- OWL reasoner inference
- Docker Compose and Kubernetes YAML templates/examples
- Maven, Docker, and Docker Compose configuration
- Implementation-related documentation
- `tasks.md` and roadmap marker updates when evidence supports them

Builder must implement one approved roadmap task or one small vertical slice at a time.

### Reviewer Agent

Use for review work:

- `prompts/reviewer-agent.md`

Reviewer Agent tasks include:

- Reviewing latest git diff
- Reviewing feature specs
- Finding bugs and behavioral regressions
- Checking ontology consistency and Turtle validity
- Checking SPARQL matching correctness (including cross-plant queries)
- Checking MTP AML parser mapping to CSS-SDA triples
- Checking OPC UA Server Datasheet parser mapping to CSS-SDA triples
- Checking OWL inference axiom correctness
- Checking Java/Spring layering
- Checking Jena resource handling
- Checking REST API validation and error behavior
- Checking Docker Compose YAML determinism and validity
- Checking Kubernetes YAML determinism and validity
- Checking test coverage and architecture compliance
- Writing `spec-review-report.md` or `review-report.md` in the active feature folder

Reviewer should not implement code unless explicitly requested.

### Validation Agent

Use for validation work:

- `prompts/validation-agent.md`

Validation Agent tasks include:

- Running tests and validation commands
- Validating roadmap acceptance criteria
- Checking application startup and `/actuator/health`
- Validating ontology loading
- Validating SPARQL match/no-match behavior (including cross-plant queries)
- Validating REST API behavior
- Validating Kubernetes YAML parsing
- Validating Docker Compose YAML parsing
- Validating Docker and Docker Compose behavior when required by the phase
- Writing `spec-validation-report.md` or `validation-report.md` in the active feature folder

Validation Agent should not implement features unless explicitly requested.

---

## Specialist Subagents

Specialist prompts live under `prompts/subagents/`.

Use them when a review or validation task needs focused analysis:

- `prompts/subagents/spec-reviewer.md`: feature-spec completeness and implementability
- `prompts/subagents/spec-validation.md`: feature-spec execution readiness
- `prompts/subagents/spring-boot-reviewer.md`: Java 25, Spring Boot 4, REST, layering, configuration
- `prompts/subagents/security-reviewer.md`: API input safety, dependency risk, secrets, Docker/Kubernetes hardening
- `prompts/subagents/concurrency-reviewer.md`: Jena model sharing, lifecycle, caching, thread-safety, deterministic output
- `prompts/subagents/sparql-optimizer.md`: SPARQL/Jena query performance and RDF data shape
- `prompts/subagents/test-coverage-reviewer.md`: acceptance-criteria and edge-case test coverage
- `prompts/subagents/mtp-parser-reviewer.md`: MTP AML file parsing, VDI/VDE/NAMUR 2658 schema, CSS-SDA triple generation
- `prompts/subagents/uaserver-datasheet-reviewer.md`: OPC UA Server Datasheet parsing, IDTA 02009-1-0 submodel mapping
- `prompts/subagents/ontology-inference-reviewer.md`: OWL reasoner inference, OfferedCapability derivation, axiom correctness
- `prompts/subagents/k8s-manifest-reviewer.md`: Kubernetes YAML generation, nodeSelector from DeploymentContext
- `prompts/subagents/rdf-ontology-reviewer.md`: Turtle/RDF ontology consistency, class hierarchy, property alignment

---

## Default Workflow

Use this workflow unless the human asks otherwise:

1. Human selects or approves a roadmap task.
2. Builder Agent creates a feature specification using `skills/feature-spec` when requested or required.
3. Reviewer Agent reviews the feature spec and writes `spec-review-report.md` in the feature folder.
4. Builder Agent addresses required spec review findings.
5. Reviewer Agent re-reviews the feature spec when needed.
6. Validation Agent validates the feature spec and writes `spec-validation-report.md`.
7. Builder Agent addresses validation blockers.
8. Builder Agent implements the approved task.
9. Builder Agent updates `tasks.md` and roadmap markers only when evidence supports the update.
10. Reviewer Agent reviews the latest git diff.
11. Builder Agent fixes required review findings.
12. Validation Agent validates tests and acceptance criteria.
13. Human performs final review.
14. Human decides whether to commit, push, merge, or deploy.

---

## How to Activate a Role

When the human says:

- "Act as Builder Agent"
- "Use Builder Agent"
- "Run Builder Agent"
- "Implement this roadmap task"

Read and follow:

- `prompts/builder-agent.md`

When the human says:

- "Act as Reviewer Agent"
- "Use Reviewer Agent"
- "Run Reviewer Agent"
- "Review this diff"
- "Review this feature spec"

Read and follow:

- `prompts/reviewer-agent.md`

When the human says:

- "Act as Validation Agent"
- "Use Validation Agent"
- "Run Validation Agent"
- "Validate this implementation"
- "Validate this feature spec"

Read and follow:

- `prompts/validation-agent.md`

If the requested role is unclear, ask which role to use before changing files.

---

## Project-Specific Engineering Rules

Agents must:

- Keep the MVP small, executable, and aligned with `specs/roadmap.md`.
- Keep semantic matching logic in RDF/Turtle and SPARQL where possible.
- Avoid hardcoded Java service selection rules.
- Keep ontology and query files versioned and reviewable.
- Generate Docker Compose and Kubernetes YAML deterministically.
- Add tests for positive and negative semantic matching scenarios.
- Validate generated YAML by parsing it when YAML generation changes.
- Use clear errors for missing/invalid ontology files and query failures.
- Keep Java/Spring code layered according to `specs/tech-stack.md`.
- Add concise JavaDoc to new or modified public classes and public contract methods.
- Add parser and inference tests alongside matching tests for every affected phase.

Agents must not:

- Introduce SQL databases, Redis, Kafka, queues, cloud services, or new infrastructure without explicit approval.
- Implement real OPC UA communication in the MVP unless the human expands scope.
- Implement safety-critical control logic.
- Replace SPARQL matching rules with Java logic when the rules belong in semantic assets.
- Add unrelated features or broad refactors.
- Remove or bypass tests to make a build pass.
- Hide failures or mark blocked checks as passed.

---

## Safety Rules

Agents must not:

- Push code unless explicitly asked.
- Commit automatically unless explicitly asked.
- Merge branches.
- Deploy.
- Rewrite git history unless explicitly asked.
- Store secrets, tokens, certificates, or private keys in the repository.
- Mark roadmap tasks complete without validation evidence.
- Revert user changes unless explicitly instructed.

---

## Prompt Logging

Prompt logging is optional unless the human explicitly asks for it or an active feature workflow requires it.

If prompt logging is used, store prompts by scope:

- General/non-feature prompts: `prompts/YYYY-MM-DD.md`
- Feature-specific prompts: `specs/YYYY-MM-DD-<feature-name>/prompts.md`

Feature-specific prompts should not be duplicated in the general prompt log.

Use this entry format:

```markdown
## YYYY-MM-DD HH:MM Europe/Berlin
<prompt text>
```

---

## Roadmap and Task Tracking

`specs/roadmap.md` tracks planned work and high-level status.

`tasks.md` records completed work and implementation history when present.

Only update roadmap markers when acceptance criteria have validation evidence or the human explicitly instructs the update.

Use this roadmap marker style:

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS] Currently being worked on
- [ ] [BLOCKED] Blocked by dependency, missing decision, or environment issue
```

---

## Report Naming

Current report naming standard per feature folder:

- `spec-review-report.md`
- `spec-validation-report.md`
- `review-report.md`
- `validation-report.md`

Do not rename historical report files unless explicitly requested.
