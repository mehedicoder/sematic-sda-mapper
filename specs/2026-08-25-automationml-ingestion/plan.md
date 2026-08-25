# Plan — Phase 19: AutomationML (CAEX) Ingestion

1. Enum
   1.1 Add `AUTOMATION_ML` to `AssetFileFormat`; add `EntityOrigin.AML`; update `origin()` switch.
2. Parser
   2.1 Create `AutomationMlParser` implementing `CanonicalModelParser`.
   2.2 Map CAEX AutomationML → canonical model (CommunicationBinding, DataConnector,
       DeploymentContext, Skill/SkillParameter as present).
3. Registry
   3.1 Ensure Spring bean registration in `CanonicalParserRegistry`.
4. Detection
   4.1 Extend `HubFileTypeDetector` to sniff CAEX and disambiguate from MTP AML.
5. UI
   5.1 Extend `UploadDropzone` accepted types; offer format in `MappingHubView`.
6. Tests
   6.1 `HubFileTypeDetectorTest`: AutomationML detection + MTP disambiguation.
   6.2 `AutomationMlParserTest`: valid parse → canonical triples; malformed rejected.
   6.3 Parsed triples load into Jena `Model`.
