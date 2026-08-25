# Requirements — Phase 19: AutomationML (CAEX) File Format Support

## Scope
Extend the Mapping Hub ingest (Phase 4 parsers / Phase 12 view) to accept generic
**AutomationML** (CAEX) files, distinct from the MTP AML profile handled by `MtpModelParser`.
Currently `AssetFileFormat` supports AAS XML/JSON, MTP AML, OPC UA JSON only.

- Add `AUTOMATION_ML` to `AssetFileFormat` (label "AutomationML", ext `aml`/`xml`,
  parser "AutomationML Parser"); extend `EntityOrigin` + `origin()` switch.
- Add `AutomationMlParser` implementing `CanonicalModelParser` mapping CAEX → canonical model
  (CommunicationBinding, DataConnector, DeploymentContext, Skill/SkillParameter as present).
- Register in `CanonicalParserRegistry` (Spring bean auto-scan).
- Extend `HubFileTypeDetector` to sniff AutomationML (`<CAEX>` / `CAEXFile` / AutomationML
  xmlns) and **disambiguate** from MTP AML (MTP markers `mtpaml`/`mtp-aml`/`opcuaserver`).
- Extend `UploadDropzone` accepted types for `AUTOMATION_ML`; offer in `MappingHubView`.
- Add/extend tests (valid parse, malformed rejected, detection disambiguation, Jena load).

## Decisions
- Generic CAEX extraction path separate from MTP profile.
- `EntityOrigin.AML` added (distinct from MTP) for provenance clarity.

## Context
- MTP *is* a profile of AutomationML; detection must not misclassify generic AML as MTP.
- NOTE: `specs/mission.md` lists "Full AutomationML beyond MTP AML" as long-term; this phase
  was explicitly requested by the human architect and is scoped to generic CAEX ingestion only.

## Artifact placement
`specs/2026-08-25-automationml-ingestion/`.
