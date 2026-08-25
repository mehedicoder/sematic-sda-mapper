# Plan — Phase 16: Process Variable Analysis Example

1. Example data
   1.1 Add device-description file(s) under `device-descriptions/` (reuse MTP / OPC UA formats).
   1.2 Add ABox individuals / triples: UseCase, UseCaseRequirement, Capability / Skill / Service,
       CommunicationBinding, DataConnector, DeploymentContext, SemanticConcept (CDD IRDI).
2. Matching
   2.1 Ensure a SPARQL path covers R1–R3 + R2a for the new requirement.
3. Tests
   3.1 Positive: requirement returns expected analytics service.
   3.2 Negative: wrong concept / wrong unit / wrong SIL excluded.
   3.3 Extend `EvaluationTest`.
4. UI
   4.1 Confirm scenario visible in `MappingHubView` / `ServiceCatalogView`.
