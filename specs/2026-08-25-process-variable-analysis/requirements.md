# Requirements — Phase 16: Process Variable Analysis Example

## Scope
Add the process-variable-analysis evaluation scenario named by the paper (Summary, p.12) as the
next validation step, alongside existing flowmeter / fermentation / HVAC examples.

- Define a ProcessVariable-analysis `UseCase` + `UseCaseRequirement` (capability = analytic
  function over a process variable with UCUM unit + IEC CDD IRDI semanticID).
- Provide an example device description under `src/main/resources/device-descriptions/`
  (MTP and/or OPC UA datasheet).
- Add a SPARQL match path exercising R1–R3 and R2a (`requiresConcept` CDD coverage).
- Add positive match + negative tests (wrong concept / wrong unit / wrong SIL) to `EvaluationTest`.
- Surface the example in `MappingHubView` / `ServiceCatalogView` for demo.

## Decisions
- Reuse the existing parser + lift / infer / match pipeline; no new parser unless the example
  uses a format not yet covered.
- Encode the scenario as ABox individuals / triples consistent with the v2 ontology.

## Context
- Paper Summary (p.12): "Next steps is the validation of the approach by an implementation
  of a process variable analysis example."
- Follow the existing `EvaluationTest` pattern.

## Artifact placement
`specs/2026-08-25-process-variable-analysis/`.
