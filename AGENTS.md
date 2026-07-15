# AGENTS.md

Human-led, role-based agent workflow for the Semantic SDA Mapper project. The human engineer is the architect and final decision maker. See `prompts/agent-workflow.md` for the full workflow.

## How to Activate a Role

| Say this... | Then follow |
|---|---|
| "Act as Builder Agent" / "Implement this roadmap task" | `prompts/builder-agent.md` |
| "Act as Reviewer Agent" / "Review this diff" / "Review this feature spec" | `prompts/reviewer-agent.md` |
| "Act as Validation Agent" / "Validate this implementation" / "Validate this feature spec" | `prompts/validation-agent.md` |

If the requested role is unclear, ask before changing files.

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
- Add `@DisplayName` on every test class and test method with clear, human-readable descriptions (e.g. `@DisplayName("Should return Good status for healthy pump FlowRate")`).
- Add parser and inference tests alongside matching tests for every affected phase.
- Ensure UI views are responsive and work on desktop, tablet, and mobile viewports.
- Support light and dark mode with persisted user preference.
- Follow the CSS-SDA hierarchy (Intent → Capability → Skill → Service → CommunicationBinding → DataConnector → Deployment) for navigation.
- Write Vaadin UI tests for view rendering and component interaction workflows alongside every phase that adds or modifies UI views.
- Add Micrometer metrics (counters, timers) for every service-layer method that processes requests; expose via `/actuator/prometheus`.
- Configure structured logging with trace/span correlation IDs from Phase 1 onward.
- Add custom health indicators for ontology readiness, SPARQL query loadability, and OPC UA simulation reachability.
- Add Flyway migration scripts for every schema change, never rely on `spring.jpa.hibernate.ddl-auto=update` for production schema evolution.
- Implement robust, layered exception handling with Spring's `ProblemDetail` (RFC 9457) for API errors, and clear error messages for ontology loading, SPARQL execution, input parsing, and matching failures.
- Define a custom exception hierarchy with unchecked exceptions for domain failures (`OntologyLoadingException`, `SparqlExecutionException`, `MatchingException`, etc.), and rely on `@ControllerAdvice` + `@ExceptionHandler` for consistent ProblemDetail mapping. Wrap checked exceptions at the service boundary into the appropriate custom unchecked exception.
- When creating a feature spec, create a feature branch named after the spec folder (e.g., `specs/2026-07-15-phase1-foundation` → branch `feature/2026-07-15-phase1-foundation`).

Agents must not:

- Introduce Redis, Kafka, queues, cloud services, or new infrastructure without explicit approval.
- Implement real OPC UA communication in the MVP unless the human expands scope.
- Implement safety-critical control logic.
- Replace SPARQL matching rules with Java logic when the rules belong in semantic assets.
- Add unrelated features or broad refactors.
- Remove or bypass tests to make a build pass.
- Hide failures or mark blocked checks as passed.

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

## Prompt Logging

Optional unless explicitly requested. Store by scope:
- General: `prompts/YYYY-MM-DD.md`
- Feature-specific: `specs/YYYY-MM-DD-<feature-name>/prompts.md`

## Roadmap and Task Tracking

`specs/roadmap.md` tracks planned work. `tasks.md` records history. Use this marker style:

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS]
- [ ] [BLOCKED]
```

Only update markers with validation evidence or explicit human instruction.

### Task Status Update Workflow

When completing work on a phase or feature, agents MUST update both files:

1. **`specs/roadmap.md`** — Mark each completed task `[x]` and each completed acceptance criteria `[x]` in the relevant phase section.
2. **`tasks.md`** — Append a completion entry under the phase heading with:
   - Status (`[x] Completed`)
   - Branch name
   - Completion date
   - Test count and pass status
   - Completion evidence (`mvn clean test` output, key decisions)
   - Summary table of files created/modified
   - Key technical decisions made during implementation

Example `tasks.md` entry:

```markdown
## Phase N: Feature Name

**Status:** [x] Completed
**Branch:** `feature/YYYY-MM-DD-phaseN-name`
**Completed:** YYYY-MM-DD
**Tests:** N passing (0 failures)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS
- Acceptance criteria all marked [x] in specs/roadmap.md

### What was built

| Component | Files |
|---|---|
| ... | ... |
```

## Report Naming

Feature folder reports: `spec-review-report.md`, `spec-validation-report.md`, `review-report.md`, `validation-report.md`. Do not rename historical files.
