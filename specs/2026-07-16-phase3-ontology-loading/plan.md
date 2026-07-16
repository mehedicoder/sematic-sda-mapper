# Plan: Phase 3 — Ontology Loading with Apache Jena

## Group 1: OntologyModelLoader Service

1. Create `OntologyLoadingException` in `com.sda.mapper.error` package
2. Create `OntologyModelLoader` in `com.sda.mapper.ontology` package
   - `@Component` with `@ConstructorBinding` or `@Value` for `app.ontology.file-path`
   - `@PostConstruct` or eager init: load ontology at startup
   - `getModel()` returns the loaded Jena `Model`
   - `isLoaded()` returns readiness status
3. Register `OntologyModelLoader` as Spring `@Bean` for downstream injection
4. Update `OntologyReadinessHealthIndicator` to use `OntologyModelLoader.isLoaded()` instead of file-existence check
5. Add `app.ontology.file-path` to `application.yml` if not present

## Group 2: Unit Tests

6. Add unit test: `OntologyModelLoaderTest` — successful loading returns non-null Model with expected class count
7. Add unit test: `OntologyModelLoaderTest` — missing ontology file throws `OntologyLoadingException`
8. Add unit test: `OntologyModelLoaderTest` — invalid Turtle content throws `OntologyLoadingException`
9. Add test ontology file: `src/test/resources/ontology/invalid-syntax.ttl`

## Group 3: ExplorerView Dynamic Loading

10. Refactor `OntologyExplorerView` to inject `OntologyModelLoader`
11. Replace hardcoded `buildOntologyTree()` with Jena-driven tree builder:
    - Walk `model.listClasses()` → class nodes
    - For each class, walk `model.listSubjectsWithProperty(RDF.type, class)` → individual nodes
    - Walk `model.listProperties()` → property relationships
12. Add detail tabs: Overview, Properties, Relationships
    - Overview: label, type, URI
    - Properties: list data properties with values
    - Relationships: list object properties with targets
13. Handle empty model gracefully (show "No ontology loaded" message)

## Group 4: Vaadin UI Tests

14. Add Vaadin UI test: ExplorerView renders tree from Jena model
15. Add Vaadin UI test: clicking a node shows detail tabs
16. Add Vaadin UI test: tab switching works (Overview ↔ Properties ↔ Relationships)

## Group 5: Verification

17. Run `mvn clean test` — all tests pass
18. Verify `/actuator/health` shows ontology UP
19. Verify ExplorerView loads dynamically in browser
