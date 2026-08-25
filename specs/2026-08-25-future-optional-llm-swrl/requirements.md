# Requirements — Phase 18: Future / Optional (paper-stated plans)

Items from the paper that are explicitly future work or optional fidelity:

1. **LLM-based semantic translation** (paper p.7–8, p.12): "It is planned to do this translation
   based on LLMs" for AutomationML / OPC UA NodeSets / AAS → ontology, and "Agentic AI"
   engineering. `specs/mission.md` Out of Scope + AGENTS: **do not implement without approval**.
2. **SWRL fidelity**: R-Deploy / R-Verify are implemented in Java (`RDeployService` /
   `RVerifyService`) rather than a SWRL engine. Functionally equivalent; migrate to SWRL only
   if paper-exact fidelity required.
3. **Property-name alignment**: paper uses `uses` (Skill → CommunicationBinding / DataConnector);
   verify current `usesBinding` / `usesConnector` naming and align if needed.
4. **R2a hardening**: ensure `requiresConcept` + IEC 61987 CDD IRDI coverage (`FILTER NOT EXISTS`)
   is enforced on every match path, not only selected queries.

## Status
- LLM item: **blocked** (approval required).
- SWRL / property-name / R2a: **optional**, implement only if prioritized.

## Artifact placement
`specs/2026-08-25-future-optional-llm-swrl/`.
