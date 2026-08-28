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
- **Acceptance gate added**: `UseCaseAcceptanceTest` runs all 4 `docs/usecases` folders end-to-end through `MappingHubService` (processUpload → reRunPipeline → buildManifest) and asserts a non-null `complete-manifest.json` + valid `coveragePercent`; Pump case additionally asserts the NE107 match / `Pump Asset Condition Monitoring` name / ≥75% coverage.
- **Full suite: 611 tests, 0 failures, BUILD SUCCESS.** No commits made (per AGENTS.md).

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

## Acceptance Criteria
All four documented use cases under `docs/usecases` must be **testable end-to-end** through the
Mapping Hub pipeline (instantiate → lift → infer → match → catalog) and must export a
`complete-manifest.json`. This is the regression gate that proves "all four use cases can be tested":

1. **Pump Asset Health Monitoring** (`docs/usecases/Pump Asset Health Monitoring/`): catalogue_services/asset/edge_nodes TTL + `req_pump_health.ttl`. Must match the NE107 Asset Health Service, `name = "Pump Asset Condition Monitoring"`, coverage ≥ 75%.
2. **REQ-001-flowmeter-diagnostics** (`docs/usecases/REQ-001-flowmeter-diagnostics/`): `pump-aas.json`, `pump-module-mtp.xml`, `pump-opcua-datasheet.json`, `req001-uaserver-nodeset-complete.xml`, `requirement-flowdiagnostics.ttl`. Uploads + pipeline + manifest export with no errors.
3. **REQ-002-fermentation-skid** (`docs/usecases/REQ-002-fermentation-skid/`): `mixer-module-mtp.xml`, `req002-mtp-fermentation-skid-complete.xml`, `req002-mtp-fermentation-skid.xml` (flat + `<Instance>`-wrapped MTP). Uploads + pipeline + manifest export with no errors.
4. **REQ-003-hvac-plant** (`docs/usecases/REQ-003-hvac-plant/`): `req003-uaserver-ahu01-complete.json`, `req003-uaserver-ahu01.json`, `temperature-sensor-opcua-datasheet.json`. Uploads + pipeline + manifest export with no errors.

Authoritative gate test (must pass): `src/test/java/com/sda/mapper/hub/service/UseCaseAcceptanceTest.java`
— runs each folder through `MappingHubService.processUpload` → `reRunPipeline` → `buildManifest`,
asserts a non-null manifest with valid `coveragePercent`, and asserts the Pump NE107 match/name/coverage.
Supporting tests: `HubUseCaseParsingTest` (hub UI parser path), `UseCaseDocumentsParsingTest` (pipeline path),
`MappingHubAboxUploadTest` (Pump end-to-end detail).

## Phase 22 (LLM-Augmented Mapping Pipeline) — Progress
Delivered as a separate, LLM-first `AiMappingStudioView` ("AI Mapping Studio", `/ai-mapping`) that shares `MappingHubService` with the deterministic `MappingHubView` (paper §4.2/§4.3, §5). Deterministic `SemanticReasonerService` stays authoritative; LLM only supplies drafts via `addManualEntity` (human-in-the-loop Accept).
- **22a** `[x]` `LlmEntityTranslationService`: device description → `List<CanonicalEntity>` (reuses `ManualEntityConverter`).
- **22c** `[x]` `LlmEntityTranslationService.translateRequirement`: free-text → `UseCaseRequirement`/`RequiredCapability`/`VerificationCriterion` graph.
- **22g** `[x]` `AiMappingStudioView` (`/ai-mapping`): paste description/requirement → "Translate with AI" → Accept/Reject drafts → feed pipeline → export manifest; registered in `MainLayout` drawer.
- **22b** `[x]` `LlmLiftAugmentor` + `LlmLiftSuggestion` record + "Suggest lift relations (AI)" button (proposes `mapsToCapability`/`realisedBy`/`exposedAs`/`operatesOn`; validated relation/class set; Accept materialises target via `addManualEntity`); unit-tested.
- **22d** `[x]` `LlmMatchAugmentor` + `LlmGapDraftService` (+ `LlmGapDraft`/`LlmGapExplanation`/`LlmGapResult`): gap report → LLM gap-fill drafts + natural-language explanations, validated against SDA classes; served via `LlmGapDraftService`.
- **22e** `[x]` `LlmMappingAgent` (+ `LlmAgentStep`/`LlmAgentRunPlan`/`LlmAgentResult`): orchestrates 22a→22b→22d over a use case → run plan + `complete-manifest.json`; "Run with AI agent (end-to-end)" button in `AiMappingStudioView`; deterministic fallback unchanged.
- **22f** `[x]` `MappingHubService` gains opt-in LLM steps via `setLlmAugmentors(...)` + `setRunWithLlm(boolean)` (no constructor change; default path untouched). When enabled, `executeStages` runs guarded `LLM-LIFT` / `LLM-MATCH-AUGMENT` stages emitting child spans via the existing `PipelineStageObserver`; results in `getLlmLiftSuggestions()` / `getLlmGapResult()`.
- Sub-phases 22a–22g now all `[x]`. No commits made this session (per AGENTS.md). Full suite green (626 tests).

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
- `src/test/java/com/sda/mapper/hub/service/UseCaseAcceptanceTest.java` — **authoritative acceptance gate**: all 4 use cases run end-to-end + export manifest.
- `src/test/java/com/sda/mapper/parser/UseCaseDocumentsParsingTest.java` — pipeline-path verification.
- `src/test/java/com/sda/mapper/hub/parser/HubUseCaseParsingTest.java` — hub UI-path verification (all files, incl. gaps).
- `docs/usecases/REQ-002-fermentation-skid/req002-mtp-fermentation-skid.xml` — flat MTP (Gap 1).
- `docs/usecases/REQ-001-flowmeter-diagnostics/req001-uaserver-nodeset-complete.xml` — UA NodeSet XML (Gap 2).
- `tasks.md` — ABox fix + validation + cleanup recorded (Gap 1/2 not yet logged).
