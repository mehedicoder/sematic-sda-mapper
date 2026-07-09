# Agent Workflow

Role-based workflow for the Semantic SDA Mapper prototype. The human engineer is the architect and final decision maker. Agents implement, review, and validate the approved roadmap.

## Roles

1. **Human Architect** — owns architecture, ontology direction, API contracts, deployment strategy, security boundaries, infrastructure decisions, and final approval.
2. **Builder Agent** — implements approved roadmap tasks. See `prompts/builder-agent.md`.
3. **Reviewer Agent** — reviews feature specs and implementation. See `prompts/reviewer-agent.md`.
4. **Validation Agent** — validates acceptance criteria and execution readiness. See `prompts/validation-agent.md`.
5. **Specialist subagents** — focused analysis under `prompts/subagents/` (spec, security, concurrency, SPARQL, parsers, inference, K8s, RDF, test coverage).

## Standard Workflow

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

## Git Rules

- Work on a feature branch.
- No pushing, committing, merging, or history rewrites unless explicitly instructed.
- Keep changes small and reviewable; avoid unrelated file changes.

## Roadmap Rules

`specs/roadmap.md` tracks planned work and high-level status. Only mark a task completed when acceptance criteria are validated or the human explicitly instructs the update. Use `[IN_PROGRESS]` and `[BLOCKED]` markers for visibility.
