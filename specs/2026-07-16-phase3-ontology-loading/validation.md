# Validation: Phase 3 — Ontology Loading with Apache Jena

## Automated Checks

- [ ] `mvn clean test` — BUILD SUCCESS (0 failures)
- [ ] `OntologyModelLoaderTest` — 3 tests pass (load, missing file, invalid content)
- [ ] ExplorerView UI test — tree renders from Jena model
- [ ] ExplorerView UI test — tab switching works
- [ ] `/actuator/health` returns ontology UP

## Manual Verification

1. Start the application (`mvn spring-boot:run`)
2. Navigate to ExplorerView (`/ontology`)
3. Verify tree is populated with CSS-SDA classes (Capability, Skill, Service, etc.)
4. Click on a class node — verify Overview tab shows label and type
5. Click Properties tab — verify data properties are listed
6. Click Relationships tab — verify object properties are listed
7. Delete or rename `ontology/css-sda-small.ttl` — restart — verify clear error message
8. Restore ontology — restart — verify tree loads again

## Definition of Done

- [ ] `OntologyModelLoader` loads ontology from classpath at startup
- [ ] Missing/invalid ontology produces `OntologyLoadingException` with clear message
- [ ] ExplorerView dynamically populates tree from Jena model
- [ ] Detail tabs show Overview, Properties, Relationships
- [ ] All unit and UI tests pass
- [ ] Health indicator reflects ontology readiness
