# Requirements: Phase 3 — Ontology Loading with Apache Jena

## Scope

### In Scope
- `OntologyModelLoader` service that loads the CSS-SDA ontology (Turtle) into an Apache Jena `Model`
- Load ontology from classpath path (`app.ontology.file-path` in `application.yml`)
- Return a fully populated Jena `Model` for downstream SPARQL queries and OWL inference
- Clear error handling: `OntologyLoadingException` for missing file, invalid Turtle, or parse failure
- `OntologyModelLoader` registered as a Spring `@Bean` for injection across the application
- Unit tests for: successful loading, missing file, invalid ontology content
- Wire `OntologyExplorerView` to `OntologyModelLoader` — tree loads dynamically from Jena model (classes, properties, individuals)
- ExplorerView entity detail tabs: Overview, Properties, Relationships
- Vaadin UI test for ExplorerView dynamic tree loading and tab switching

### Out of Scope
- OWL inference (Phase 6)
- SPARQL query execution (Phase 5)
- Input file parsing (Phase 4)
- Any UI views beyond ExplorerView

## Decisions
- `OntologyModelLoader` uses `ModelFactory.createDefaultModel()` and `Model.read()` for Turtle loading
- Ontology file path is externalized via `app.ontology.file-path` in `application.yml` (default: `ontology/css-sda-small.ttl`)
- The loader reads from classpath, not filesystem, for portability across environments
- ExplorerView tree is rebuilt from Jena model on each attach (no caching — model is immutable after startup)
- Detail tabs show: Overview (label + type), Properties (data properties from RDF), Relationships (object properties from RDF)

## Context
- The ontology file (`css-sda-small.ttl`) is 552 lines of Turtle with 8 core classes, NE 107 diagnostics, 3 use cases
- The existing `OntologyExplorerView` uses a hardcoded tree — this phase replaces it with a dynamic Jena-driven tree
- Follow existing patterns: `OntologyReadinessHealthIndicator` already checks ontology availability
- Exception hierarchy: `OntologyLoadingException` extends `RuntimeException` (per `specs/tech-stack.md`)

## Artifact Placement
- Spec folder: `specs/2026-07-16-phase3-ontology-loading/`
- Reports: `spec-review-report.md`, `spec-validation-report.md`, `review-report.md`, `validation-report.md`
