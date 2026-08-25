# Validation — Phase 15

## Automated
- `mvn test` (at least `CanonicalRecordCoverageTest`) passes.
- Ontology loads in Jena; `requiresFunction` present with correct range.
- `AnalyticalFunction` / `ControlFunction` / `PlantUnit` instantiable.

## Manual
- Open `OntologyView`; Capability detail shows `requiresFunction`; new classes appear in hierarchy.

## Definition of done
- Paper Figure 5 `requiresFunction` + `PlantUnit` modelled; full suite green.
