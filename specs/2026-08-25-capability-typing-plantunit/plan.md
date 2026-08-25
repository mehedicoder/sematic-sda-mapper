# Plan — Phase 15: Capability Typing & PlantUnit

1. Ontology (TTL)
   1.1 Add `:AnalyticalFunction` and `:ControlFunction` as `owl:Class`.
   1.2 Add `:PlantUnit` as `owl:Class`.
   1.3 Add `:requiresFunction` `owl:ObjectProperty` (domain `:Capability`, range `:AnalyticalFunction | :ControlFunction`).
   1.4 Widen `:operatesOn` range to include `:PlantUnit` (union with existing `:Asset`).
2. Tests
   2.1 Update `CanonicalRecordCoverageTest` expected relation / datatype-property counts.
   2.2 Add positive coverage assertion that the new classes/properties load.
3. Verify
   3.1 Run `mvn -q -Dtest=CanonicalRecordCoverageTest test` and the full suite.
