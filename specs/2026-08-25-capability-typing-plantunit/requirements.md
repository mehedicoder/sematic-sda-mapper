# Requirements — Phase 15: Capability Typing & PlantUnit

## Scope

Align `src/main/resources/ontology/sda_ontology_v2_structured.ttl` with the paper's
Figure 5 class definitions that are currently missing:

- Add `:requiresFunction` object property (`:Capability` → `:AnalyticalFunction | :ControlFunction`)
- Add `:AnalyticalFunction` and `:ControlFunction` classes (function-type taxonomy,
  e.g. "temperature control" / "remaining useful life prediction")
- Add `:PlantUnit` class and extend `:operatesOn` range to `:Asset | :PlantUnit`

**In scope:** ontology edits + test-count updates + positive/negative coverage tests.
**Out of scope:** SWRL, new UI, parser changes.

## Decisions

- Keep all additions inside the existing v2 TTL conventions (rdfs:label/comment/domain/range).
- `requiresFunction` is a single object property whose range is a union of the two new classes
  (mirrors paper Figure 5 `requiresFunction → :AnalyticalFunction | :ControlFunction`).
- `AnalyticalFunction` / `ControlFunction` are plain `owl:Class` individuals can later instantiate.
- Update `CanonicalRecordCoverageTest` expected datatype/object property counts to match.

## Context

- Paper §4.2, Figure 5 (Requirement & Capability class snippets).
- Follow existing TTL style; the file is loaded by `OntologyModelLoader` and visualized by
  `OntologyDataService` (multi-domain aware).
- Tests must keep the full suite (currently 484+) green.

## Artifact placement

All spec artifacts live under `specs/2026-08-25-capability-typing-plantunit/`.
