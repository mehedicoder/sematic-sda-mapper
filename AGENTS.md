## Goal
- Implement and complete the live ontology reasoning-tree feature in Mapping Hub: hide the console, stream reasoning nodes during the MATCH stage into a collapsible colour-coded tree (requirement → capability → skill → service → binding/connector/deployment/zone/safety + semantic-concept coverage), with truthful PASS/FAIL (directly-declared constraints only, else INFO/SKIPPED) — fully wired, compiled, tests written. The four `docs/usecases` acceptance gates remain green.

## Constraints & Preferences
- AGENTS.md: no new deps/infra without approval; commit/push only when explicitly requested; reuse Vaadin UI test patterns.
- Live reasoning nodes stream via `ReasoningTracePublisher` inside the existing MATCH stage using the stage `runId` (pipeline root trace `run.id` untouched); observability must never break pipeline behaviour (broadcast errors swallowed).
- Status truthfulness: PASS/FAIL only for direct requirement-declared constraints (`sda:protocol`, `accessMode`, `minRecall`, `maxLatency`, `onPlantNetwork`, `safetyIntegrityLevel`, `determinismClass`, `networkZone`); otherwise INFO; `hasConstraint`/`hasParameter` intents INFO only.
- Docker rebuild required for Java/resource changes.
- Backward-compatible TTL vocabulary: `:title`/`:description` AND legacy `:requirementName`/`:requirementDescription` both supported.

## Progress
### Done
- **Reasoning tree (current feature)**:
  - `ReasoningNode` record (13 fields, `INFO/PASS/FAIL/SKIPPED`, `NodeType` enum) + immutable `ReasoningTree` (`nodes()` emission order, `roots()`, `childrenOf()`, `size()`).
  - `ReasoningTracePublisher` (`@Component`): live broadcast, mirrors `PipelineAuditPublisher`, swallows listener failures.
  - `ReasoningTreeBuilder` (`@Component`): walks the `match-generic.rq` chain via direct model reads; per-build `Ctx` state (singleton-safe); optional `liveSink` streaming; `linkedServiceResource` matches via `serviceName` label with first-service fallback; direct-constraints map → PASS/FAIL vs INFO; SKIPPED for non-selected alternative offers; PASS/FAIL `MATCH_OUTCOME`; no-sink `build` overload.
  - `PipelineResult` (hub) gained 8th field `List<ReasoningTree> reasoningTrees` + 7-arg overload defaulting to `List.of()`.
  - `MappingHubService`: 19-arg `@Autowired` ctor now ends `(…, semanticReasonerService, sparqlQueryLoader, reasoningTreeBuilder, reasoningTracePublisher)`; 13-arg delegates with `new ReasoningTreeBuilder()`/`new ReasoningTracePublisher()`; MATCH stage builds one tree per active requirement per `model.requiredCapabilities()` via `reasoner.runMatch(catalogModel, capabilityType, requirementId, runId)` and streams node-by-node via `reasoningTracePublisher::nodeAdded`; `PipelineResult` uses `List.copyOf(reasoningTrees)`; `getReasoningTracePublisher()` added.
  - `MappingHubView`: reasoning section replaces console section (`ReasoningTreePanel`, "Ontology Reasoning (Match)", class `hub-reasoning-section`); console kept behind a field + `console.logLines(...)` in `refresh()` so `MappingHubViewTest` stays green; `refresh()` calls `reasoningTreePanel.show(lastResult.reasoningTrees())`; 4-arg ctor with `ReasoningTracePublisher`.
  - `ReasoningTreePanel`: subscribes in ctor (nodes arrive pre-attach), unsubscribes on `onDetach`; `show(List<ReasoningTree>)` resets + pins `activeRunId`; `onNode` filters by `activeRunId`, uses `ui.access(...)` when not on the UI thread; `MAX_NODES = 2000`; badges PASS/FAIL/SKIPPED/INFO via `statusClass`.
  - `hub-styles.css` appended: `.hub-reasoning-section`, `.reasoning-tree-box/_empty/_node/_label/_badge--ok|error|skipped|info`, `.reasoning-node-detail`.
  - All call sites updated: `MappingHubViewTest` (2), `MappingHubServiceRequesterJourneyTest` (pass publisher to view); big-ctor tests `UseCaseAcceptanceTest`, `MappingHubAboxUploadTest`, `MappingHubRequirementFileUploadTest`, `MappingHubManualRequirementTest`, `PipelineObservabilityTraceShapeTest` (trailing `new ReasoningTreeBuilder(), new ReasoningTracePublisher()`).
  - **Tests written & green**: `ReasoningTreeBuilderTest` (5: full-chain PASS, declared-PASS/observed-INFO, declared-violation FAIL, skipped alternative offers, live-sink order), `ReasoningTreePanelTest` (4: show/count, clear stops streaming, active-run filtering, detach unsubscribes), `ReasoningTracePublisherTest` (3: broadcast, removal, failure-swallow). Non-empty-PASS/SERVICE-tree assertions added to `MappingHubAboxUploadTest` (Pump) + `UseCaseAcceptanceTest` (`reasoningTrees()` non-null in loop; PASS outcome + SERVICE hop for Pump). Fixed two pre-existing (HEAD) test bugs surfaced by the full run: `ReasoningTracePublisherTest` re-created `received::add` (new identity) when removing a listener, and `MappingHubServiceRequesterJourneyTest` pointed `motor-aas.json` at a non-existent `/device-descriptions/aas/` path (real file: `reference-formats/`); `MappingHubViewTest.shouldRenderCoreComponents` now asserts the reasoning section title instead of the (now detached) console text.
  - **Full suite: 637 tests, 0 failures, BUILD SUCCESS** (`mvn -o test` from this machine).
- **Four-use-case baseline (prior session, kept green)**: A+B `RequirementTtlParser`; C `bindingEndpointUri`; D `COALESCE(?zone rdfs:label, ?zone sda:zoneName)`; Gap 1 flat MTP in `MtpModelParser`; Gap 2 `OpcUaNodeSetXmlParser` + `AssetFileFormat.OPCUA_XML` + `HubFileTypeDetector` + view/dropzone switches; `UseCaseAcceptanceTest` authoritative 4-folder gate; `HubUseCaseParsingTest` + `UseCaseDocumentsParsingTest`.

### In Progress
- (none — reasoning tree feature complete, tests written and passing; that work awaiting full-suite run + Docker rebuild from this machine)

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
1. (Optional) Manual Docker run to confirm all 4 use cases upload + export end-to-end in the live UI and the reasoning tree renders (rebuild image first — Java changes require it).
2. Commit/push only when user requests (per AGENTS.md).
3. Optionally record Gap 1/Gap 2 + reasoning-tree work in `tasks.md`.

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
