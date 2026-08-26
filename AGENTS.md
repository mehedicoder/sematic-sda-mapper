## Goal
- Make all four documented use cases in `docs/usecases` fully work through the Mapping Hub UI and export a correct `complete-manifest.json` by fixing the two remaining hub-UI parser gaps (flat MTP + UA NodeSet XML).

## Constraints & Preferences
- AGENTS.md: no new deps/infra without approval; commit/push only when explicitly requested; reuse Vaadin UI test patterns.
- Docker rebuild required for Java/resource changes.
- Backward-compatible TTL vocabulary: `:title`/`:description` AND legacy `:requirementName`/`:requirementDescription` both supported.
- Hub UI uses `HubFileTypeDetector` + `CanonicalParserRegistry` (hub parsers), **not** the pipeline `UploadService`/`ParserFactory` path.
- New hub parser class must NOT share a Spring bean name with the existing pipeline `@Component com.sda.mapper.parser.OpcUaNodeSetParser` (same simple class name → `ConflictingBeanDefinitionException`). Named `OpcUaNodeSetXmlParser`.

## Progress
### Done
- **A+B**: `RequirementTtlParser` lifts `UseCase`, `Stakeholder`, `UseCaseRequirementParameter`×2, `UseCaseRequirementConstraint`×2, `VerificationCriterion` via direct construction; backward-compatible `literalOrMulti` reads.
- **C**: `ServiceMatchResult.bindingEndpointUri` added; populated from `sda:bindingEndpointUri` in `SemanticReasonerService`; `MappingHubService.toCapabilityMatch` uses real URI (fallback label).
- **D**: `match-generic.rq` binds `?networkZone` via `COALESCE(?zone rdfs:label, ?zone sda:zoneName)` (fixed `:zoneName`→`sda:zoneName` bug).
- **Gap 1 (flat MTP)**: `MtpModelParser` now handles flat MTP (no `<Instance>` wrapper) — parses `Service`/`Communication`/`DataAssembly` directly under root; added `baseName()` helper. Verified: `req002-mtp-fermentation-skid.xml` now parses via hub path.
- **Gap 2 (UA NodeSet XML)**:
  - Added `AssetFileFormat.OPCUA_XML` enum constant.
  - `HubFileTypeDetector` detects `uanodeset`/`opcfoundation.org/ua/2011/03/uanodeset` (lowercase sniff) **before** the MTP_AML check.
  - New hub parser `com.sda.mapper.hub.parser.OpcUaNodeSetXmlParser` (`@Component`, implements `CanonicalModelParser`) emits `DataConnector`(UAVariable/UAObject), `Skill`(UAMethod), `CommunicationBinding`(OPC-UA), `Service`, `SemanticConcept`(namespace URIs).
  - Extended exhaustive switches: `AssetFileFormat.origin()` (`OPCUA_JSON, OPCUA_XML -> OPC_UA`), `MappingHubView.formatClass()` (adds `OPCUA_XML -> "opcua"`), `UploadDropzone.mimeTypes()` (adds `OPCUA_XML -> xml/aml`).
- `HubUseCaseParsingTest` now registers `OpcUaNodeSetXmlParser` and includes **all** `docs/usecases` files (gap exclusions removed) — passes.
- `UseCaseDocumentsParsingTest` (pipeline path) covers all files — passes.
- `MappingHubAboxUploadTest` assertions: 5 L1 `PRESENT`, name `"Pump Asset Condition Monitoring"`, `endpointUrl` real opc.tcp URI, `networkZone` `"OT Production Zone Alpha"`.
- **Full suite: 603 tests, 0 failures, BUILD SUCCESS.** No commits made (per AGENTS.md).

### In Progress
- (none — both gaps fixed and verified)

### Blocked
- (none)

## Key Decisions
- C fixed in Java (record + constructor propagation + `toCapabilityMatch` fallback); D fixed in SPARQL (idiomatic).
- A+B via direct construction (not `ManualEntityConverter`) because hub keys don't map e.g. `stakeholderID`→`id`.
- Verified **both** parser paths: pipeline path already handled all `docs/usecases` files; hub UI path had 2 gaps, now fixed.
- Gap 1: REQ-002 MTP is flat; REQ-001 `mixer-module-mtp.xml` uses `<Instance>` wrapper — added fallback branch in `MtpModelParser`.
- Gap 2: hub detector sniff is lowercased, so token checks must be lowercase. Created `OpcUaNodeSetXmlParser` (not `OpcUaNodeSetParser`) to avoid colliding bean name with the pipeline `@Component` `com.sda.mapper.parser.OpcUaNodeSetParser` (which implements the pipeline `Parser` interface, not `CanonicalModelParser`, so it cannot be reused in the hub registry).
- Hub UI flow: `MappingHubView` dropzone → `processUpload` → "Export Complete Manifest" → `hubService.buildManifest()` → `complete-manifest.json`.

## Next Steps
1. (Optional) Manual Docker run to confirm all 4 use cases upload + export end-to-end in the live UI (rebuild image first).
2. Commit only when user requests (per AGENTS.md).
3. Optionally record Gap 1/Gap 2 work in `tasks.md`.

## Critical Context
- Manifest validation findings A–D all fixed; E (`SI_Diagnostics_01` `PARTIAL`) correct by design (source has `:instanceID`, no `:name`).
- Vocabulary inconsistency preserved: Pump `req_pump_health.ttl` uses `:title`/`:description`; REQ-001/seed/demo use `:requirementName`/`:requirementDescription`.
- Two former hub-UI gaps now resolved: (1) `req002-mtp-fermentation-skid.xml` flat MTP; (2) `req001-uaserver-nodeset-complete.xml` UA NodeSet XML detection+parsing.
- Hub parsers: `AasModelParser`, `MtpModelParser`, `OpcUaModelParser` (JSON), `OpcUaNodeSetXmlParser` (XML NodeSet), `AutomationMlParser`, `RequirementTtlParser`; `ABOX_TTL` routed via `parseAndMergeAbox`.
- REQ-002/003 have no seeded requirement in demo ABox → pipeline yields no match by design (validated at parse/lift boundary).
- Nothing committed this session. Previous push range (before session): `e16adea..4a5abf8` to `origin/main`.

## Relevant Files
- `src/main/java/com/sda/mapper/hub/parser/RequirementTtlParser.java` — A+B.
- `src/main/java/com/sda/mapper/reasoning/ServiceMatchResult.java` — C (`bindingEndpointUri`).
- `src/main/java/com/sda/mapper/reasoning/SemanticReasonerService.java` — C reads `sda:bindingEndpointUri`.
- `src/main/resources/queries/match-generic.rq` — D `COALESCE(?zone rdfs:label, ?zone sda:zoneName)`.
- `src/main/java/com/sda/mapper/hub/service/MappingHubService.java` — `toCapabilityMatch` uses `r.bindingEndpointUri()`.
- `src/main/java/com/sda/mapper/hub/parser/MtpModelParser.java` — Gap 1 (flat-structure fallback + `baseName()`).
- `src/main/java/com/sda/mapper/hub/model/AssetFileFormat.java` — added `OPCUA_XML` + `origin()` switch.
- `src/main/java/com/sda/mapper/hub/parser/HubFileTypeDetector.java` — Gap 2 detection (UANodeSet before MTP_AML).
- `src/main/java/com/sda/mapper/hub/parser/OpcUaNodeSetXmlParser.java` — **new**; Gap 2 hub parser (renamed to avoid bean collision).
- `src/main/java/com/sda/mapper/hub/ui/MappingHubView.java` — `formatClass` switch add OPCUA_XML.
- `src/main/java/com/sda/mapper/hub/ui/UploadDropzone.java` — `mimeTypes` switch add OPCUA_XML.
- `src/test/java/com/sda/mapper/hub/service/MappingHubAboxUploadTest.java` — L1/name/endpointUrl/zone assertions.
- `src/test/java/com/sda/mapper/parser/UseCaseDocumentsParsingTest.java` — pipeline-path verification.
- `src/test/java/com/sda/mapper/hub/parser/HubUseCaseParsingTest.java` — hub UI-path verification (all files, incl. gaps).
- `docs/usecases/REQ-002-fermentation-skid/req002-mtp-fermentation-skid.xml` — flat MTP (Gap 1).
- `docs/usecases/REQ-001-flowmeter-diagnostics/req001-uaserver-nodeset-complete.xml` — UA NodeSet XML (Gap 2).
- `tasks.md` — ABox fix + validation + cleanup recorded (Gap 1/2 not yet logged).
