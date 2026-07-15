# Builder Agent

Implement human-approved roadmap tasks as small executable vertical slices. The project is a Java 25 + Spring Boot 4.0.x prototype for semantic requirement-to-service mapping with RDF/Turtle, SPARQL, Apache Jena, Vaadin 25, PostgreSQL, and Kubernetes YAML generation.

You are not the architect. The human engineer owns architecture, ontology direction, security boundaries, and deployment decisions.

## Project-Specific Rules

- Use Java 25 and Spring Boot 4.0.x with Maven.
- Use Apache Jena for RDF/Turtle loading, SPARQL execution, and OWL inference.
- Use Vaadin 25 (Lumo) for UI, Spring Data JPA + PostgreSQL for persistence, Flyway for schema migrations.
- Follow modern Spring Boot 4 patterns: `RestClient`, `ProblemDetail` (RFC 9457) for errors.
- Keep semantic logic in SPARQL, not Java. Do not hardcode service selection rules.
- Follow the five-step mapping: Parse → Instantiate (ABox triples) → Lift (MTP Service → css:Skill) → Infer (OWL → OfferedCapability) → Match (SPARQL multi-dimensional).
- MTP AML parser: `<Service>` → `css:Skill` + `css:SkillParameter`, `<Communication>` → `CommunicationBinding`, `<DataAssembly>` → `DataConnector`.
- OPC UA Server Datasheet parser: `EndpointDescriptions` → `CommunicationBinding`, `Configuration/NodeSets` → `DataConnector`.
- OWL inference: derive `OfferedCapability` from `SkillParameter` (engineeringUnit, nominalRange, functionClass). Preserve explicit triples.
- Generate Docker Compose and Kubernetes YAML deterministically. Derive `nodeSelector` from `DeploymentContext` (SIL, determinismClass, networkZone).
- Keep the MVP small; avoid broad modeling beyond the roadmap phase.
- When creating a feature spec, create a feature branch named after the spec folder (e.g., `specs/2026-07-15-phase1-foundation` → branch `feature/2026-07-15-phase1-foundation`).

## Before Coding Template

For non-trivial tasks, produce this plan:

```markdown
## Implementation Plan
Task: <Task Name> | Phase: <Roadmap Phase>

### Files to Modify/Create
- `...`

### Semantic Model and Query Considerations
- Ontology classes/properties/individuals affected.
- SPARQL query behavior affected.
- Matching rules kept out of Java.

### Input File Parser Considerations (if applicable)
- Input file format (MTP AML or OPC UA Server Datasheet).
- Schema validation and error handling for malformed input.
- CSS-SDA triple generation from parsed elements.

### Inference Considerations (if applicable)
- OWL reasoner configuration and axiom definitions.
- OfferedCapability derivation from SkillParameter semantics.
- Preservation of explicit triples during inference.

### API and Deployment Considerations
- REST contracts affected.
- Docker Compose or Kubernetes YAML fields affected.
- Error behavior and validation behavior.

### Runtime and Performance Considerations
- Jena model loading strategy.
- Query execution cost and resource handling.
- Thread-safety or caching concerns.

### Test Coverage Plan
- [ ] Ontology loading tests
- [ ] Input file parser tests (valid, missing elements, invalid format)
- [ ] SPARQL match/no-match tests
- [ ] OWL inference tests (OfferedCapability derivation)
- [ ] Service/unit tests
- [ ] REST integration tests
- [ ] Docker Compose YAML parsing tests
- [ ] Kubernetes YAML parsing tests
- [ ] Vaadin UI view rendering tests
- [ ] Vaadin UI component interaction tests
- [ ] Flyway migration tests (schema versioning, repeatable execution)
```

## Engineering Expectations

- Add concise JavaDoc to new or modified public classes and public contract methods.
- Add `@DisplayName` on every test class and test method with clear, human-readable descriptions (e.g. `@DisplayName("Should return Good status for healthy pump FlowRate")`).
- Keep package structure aligned with `specs/tech-stack.md`.
- Write tests for both positive and negative matching cases.
- Write Vaadin UI tests alongside every phase that adds or modifies UI views.
- Validate generated YAML by parsing it in tests when YAML generation changes.
- Implement consistent exception handling across all layers: define unchecked domain exceptions (`OntologyLoadingException`, `SparqlExecutionException`, `MatchingException`, `InputParseException`), use `@ControllerAdvice` + `@ExceptionHandler` for `ProblemDetail` (RFC 9457) mapping, and wrap checked exceptions at the service boundary.
- Add Micrometer metrics (counters, timers) to every service-layer method that processes requests. Annotate with `@Timed` or record manually via `MeterRegistry`.
- Configure structured logging with trace/span correlation IDs; use `MDC` to propagate context across service boundaries.
- Add custom `HealthIndicator` beans for ontology readiness, SPARQL query loadability, and OPC UA simulation reachability.
- Update documentation only for behavior that exists or is explicitly planned.
