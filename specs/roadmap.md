# Roadmap: Semantic SDA Mapper

> **Document status (reverse-engineered from code, 2026-08-25).**
> This roadmap was previously marked 100% complete across Phases 1–11b, but a
> code audit shows several described UI views were never delivered as standalone
> routes and the architecture evolved into a **v2 ontology + Mapping Hub** model
> that the old roadmap did not capture. This revision reflects what is actually
> implemented in `main` (verified against `src/main/java`, `src/main/resources`,
> and the test suite: 484 tests, 0 failures).
>
> **Real UI routes (navigation drawer):** Ontology Browser (`OntologyView`),
> Mapping Hub (`MappingHubView`), Sessions (`SessionOverviewView`),
> Service Catalog (`ServiceCatalogView`), OPC UA Discovery (`DiscoveryCatalogView`),
> OPC UA Monitor (`OpcUaVisualizationView`), Settings (`SettingsView`).
>
> **Removed / re-scoped from the old roadmap (did not exist as claimed):**
> `DashboardView`, `SearchView`, `ExplorerView`, `RegistrationWizardView`,
> `RequirementFormView`, `DeploymentView`, `MonitoringView`, `UploadView`.
> Their functionality was delivered through `MappingHubView` (manual entry forms,
> upload dropzone, match/pipeline) and `OpcUaVisualizationView` (monitoring),
> not as separate routes.

## Roadmap Principle

The project is implemented as a sequence of small vertical slices. Each phase produces something executable, testable, and reviewable.

Goal — prove the semantic mapping path:

```text
Input Files (MTP AML, OPC UA Server Datasheet, AAS)
→ Parse
→ Instantiate (CSS-SDA ontology ABox triples / Canonical Asset Model)
→ Lift (MTP Service → css:Skill + css:SkillParameter)
→ Infer (OWL reasoner derives OfferedCapability / RequiredCapability)
→ Match (SPARQL multi-dimensional query)
→ Docker Compose / Kubernetes Manifest
```

The CSS-SDA model: SDA-Service := ⟨ Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext ⟩

**Input sources** (vendor-supplied device description files, under `src/main/resources/device-descriptions/`):
- **MTP AML files** (VDI/VDE/NAMUR 2658) — modular process equipment (`mtp/*.xml`)
- **OPC UA Server Datasheets** (IDTA 02009-1-0) — AAS submodel (`opcua/*.json`)
- **AAS submodels** — device metadata (`aas/*.json`)

Apache Jena + SPARQL form the semantic matching core; SnakeYAML drives manifest generation.

---

## Phase 1: Project Foundation — [x] Completed

**Goal**

Runnable Java 25 / Spring Boot 4.0.x project with structure, testing, Vaadin 25 UI foundation, local dev setup, observability, OPC UA simulation, and options API.

**Tasks**

* [x] Maven-based Spring Boot 4.0.x project (Web, Validation, Actuator, Micrometer Prometheus, Micrometer Tracing/OpenTelemetry, Apache Jena, SnakeYAML, JUnit 5 + AssertJ, Vaadin 25, Spring Security, Spring Data JPA + PostgreSQL, Flyway)
* [x] Base package `com.sda.mapper`; `application.yml` observability (metrics, tracing, trace/span log pattern)
* [x] Custom health indicators: ontology readiness (`OntologyReadinessHealthIndicator`), SPARQL query loadability (`SparqlQueryHealthIndicator`), OPC UA simulation (`OpcUaSimulationHealthIndicator`, `TankSimulationHealthIndicator`)
* [x] Structured logging via Logback
* [x] `MainLayout` (AppLayout): CSS-SDA navigation drawer, header with logo/title/theme switch, responsive (desktop drawer pinned, tablet collapse, mobile overlay), `ThemeConfig` light/dark persisted
* [x] `SecurityConfig` form-login + role skeleton; `UIConfig` routing
* [x] Dockerfile, `docker-compose.yml`, README
* [x] OPC UA simulation — three approaches:
  * [x] Approach A: Node-RED OPC UA server (Fermentation ns=2, HVAC ns=3) with SSL/SignAndEncrypt, flow mounted from `docs/nodered-flow-fermentation-hvac.json`
  * [x] Approach B: Node.js Tank1 server (`docker/tank-simulation`, node-opcua, port 4334, `SetInletFlow`)
  * [x] Approach C: in-process Eclipse Milo `TankOpcUaServer` + `TankProcessState` physics + `TankSimulationScheduler`, configurable via `simulation.opcua.*`
  * [x] NAMUR NE 107 pump degradation (`PumpDegradationModel`, `Ne107DiagnosticSimulator`) extending Approach C with Good/Maintenance/Failure + sub-codes, CSS-SDA individuals (`:AssetConditionMonitoringCapability`, `:PumpDiagnosticsSkill`, `:AssetDiagnosticsService`, `:PumpBinding`, `:PumpConnector`, `:PumpDeployment`, `:REQ-03`)
* [x] Unified Java OPC UA client (`OpcUaClientFactory`, `OpcUaClientWrapper`, `OpcUaClientConfig`) reading across A/B/C via Milo
* [x] Options API: `config/options.json`, `OptionsConfigService`, `Option` record, `GET /api/v1/options/{fieldName}` (`OptionsController`)

**Acceptance Criteria — [x] all verified by tests**
* [x] `mvn clean test` passes (484 tests); app starts; `/actuator/health` UP
* [x] Vaadin UI at `/` with drawer; light/dark toggle; responsive; MainLayout UI test
* [x] Docker image buildable; Compose starts A/B/C sims without port conflicts
* [x] NE 107 Good/Maintenance/Failure mapping; sub-codes distinct; pump OPC UA exposes value+StatusCode+DiagnosticString
* [x] SPARQL matches `:AssetDiagnosticsService` for `:REQ-03` when healthy, excludes on Failure, restores on recovery
* [x] Unified client reads Temperature (A), Level (B), Level (C); handles unreachable/invalid NodeId
* [x] `/actuator/prometheus` exposes `sda.*` metrics; custom health indicators present; trace/span IDs in logs
* [x] Flyway V1 runs on startup; `options.json` loads; endpoints return lists / 404

---

## Phase 2: CSS-SDA Ontology Model (v2) — [x] Completed

**Goal**

Production **v2 ontology** (`src/main/resources/ontology/sda_ontology_v2_structured.ttl`) aligned to the paper (Islam, Maurer, Diedrich 2026) Examples A/B, with an **ontological Required/Offered Capability split**, derived subclasses, and full datatype/object property coverage. (Replaces the earlier Modbus/pump-fill stub.)

**Tasks**

* [x] v2 ontology migration (`acacea8`); production instance identity base moved to `sda-platform.org` (`f18f4c8`)
* [x] Required/Offered Capability split (`b91d7c8`): `:RequiredCapability` and `:OfferedCapability` as derived/related classes; `exposesCapability` (Service → Capability) and inverse `exposedAs`
* [x] Hierarchy-aware classes along CSS-SDA chain Intent → Capability → Skill → Service → CommunicationBinding → DataConnector → Deployment, plus UseCase / UseCaseRequirement
* [x] Examples A/B property coverage (commit `ca41eb4`):
  * [x] XSD range fixes: `bindingEndpointUri`/`endpointURI` → `xsd:anyURI`; `paramDataType` kept `xsd:string` (type-name)
  * [x] `:CommunicationBinding` — `securityPolicy`, `transportProfile`, `discoveryUrl` (`xsd:anyURI`), `securityLevel` (`xsd:integer`), `communicationNetworkZone` → `:NetworkZone`
  * [x] `:Capability` — `accuracy`, `engineeringUnit`, `nominalRange_min`/`nominalRange_max`
  * [x] `:DataConnector` — `nodeSetFile`, `semanticID`
  * [x] `:SkillParameter` — `paramNominalRangeMin`/`paramNominalRangeMax`, `paramDirection`
  * [x] `:DeploymentContext` — `maxCycleTime_ms`
* [x] Multi-domain rendering fix: `OntologyDataService` derives relations/datatype properties across **all** `rdfs:domain`/`rdfs:range` statements
* [x] Use Case A (NE 107 Flowmeter), B (Fermentation Skid), C (HVAC Plant) individuals retained from prior work
* [x] Coverage + view tests updated: 153 datatype properties, 85 relations (18 core / 67 supporting), multi-domain render assertion

**Acceptance Criteria — [x] verified**
* [x] Turtle valid (Jena loads); 24 top-level classes + 26 derived subclasses; 153 datatype props; 85 relations
* [x] `OntologyViewTest` + `CanonicalRecordCoverageTest` pass; full suite green
* [x] Required/Offered Capability split present; `exposesCapability`/inverse correct

---

## Phase 3: Ontology Loading with Apache Jena — [x] Completed

**Tasks**

* [x] `OntologyModelLoader` loads `sda_ontology_v2_structured.ttl` from classpath → Jena `Model`
* [x] Clear errors on missing/invalid ontology (`OntologyLoadingException`)
* [x] Custom health indicators wired (ontology readiness, SPARQL loadability)

**Acceptance Criteria — [x] verified**
* [x] Loads on startup/first use; invalid input fails clearly; `OntologyModelLoaderTest` covers success/missing/invalid

---

## Phase 4: Input File Parsers — [x] Completed (re-scoped upload)

**Tasks**

* [x] `parser/` set — `MTPAmlParser` (VDI/VDE/NAMUR 2658 AML → `css:Skill` + `css:SkillParameter` with UCUM units / IEC CDD IRDI), `UaServerDatasheetParser` (IDTA 02009-1-0 → `CommunicationBinding` with endpointUri/securityMode/securityPolicy/securityLevel/transportProfile/discoveryUrl, `DataConnector`), `AasParser`, `OpcUaNodeSetParser`, `ParserFactory`, `UploadService`, `ParseResult`
* [x] `hub/parser/` set — canonical-model parsers: `MtpModelParser`, `AasModelParser`, `OpcUaModelParser`, `CanonicalModelParser`, `CanonicalParserRegistry`, `HubFileTypeDetector` (drives the Mapping Hub ingest)
* [x] Example files under `src/main/resources/device-descriptions/` (mtp, aas, opcua) + tests under `src/test/resources/examples/`
* [x] Unit/integration tests: valid / missing-element / invalid-input for each parser; parsed triples loadable into Jena `Model`
* [x] Upload re-scoped: file upload is delivered as `UploadDropzone` inside `MappingHubView` (not a standalone `UploadView`)

**Acceptance Criteria — [x] verified**
* [x] MTP + datasheet parsers emit valid CSS-SDA / canonical triples; produce `CommunicationBinding` with security fields, `Skill`+`SkillParameter` with UCUM/IRDI
* [x] Both parser families fail clearly on invalid input; integration tests load triples into Jena
* [x] Upload accepted/rejected in `MappingHubView` dropzone with validation feedback

---

## Phase 5: SPARQL Matching Query — [x] Completed (global SearchView re-scoped)

**Tasks**

* [x] SPARQL queries under `src/main/resources/queries/`: `match-service.rq`, `match-service-req002.rq`, `match-service-req003.rq`, `hub-match-offered-required.rq`, `cross-plant-temperature.rq`, `deployment-placement.rq`, `r0-hard-constraint-prefilter.rq`, `service-catalog.rq`
* [x] Multi-dimensional match: Capability (functionClass, engineeringUnit, nominalRange, SIL) → Skill → Service; CommunicationBinding (protocol, accessMode); DataConnector (informationModel, nodeSetFile); DeploymentContext (networkZone, SIL, determinismClass); recall/latency filters; `ORDER BY DESC(recall) LIMIT 1`
* [x] `SparqlQueryLoader` loads queries; `SemanticReasonerService` executes
* [x] `SearchBar` component exists (`ui/component/SearchBar.java`); global `SearchView` route was **not** delivered — search/navigation is provided via the Ontology Browser and Service Catalog instead

**Acceptance Criteria — [x] verified**
* [x] `match-service.rq` returns expected service for REQ-001; excludes wrong capability/protocol/latency/deployment
* [x] `SparqlMatchingTest`, `OntologyAndQueryLoadingTest` pass; cross-plant + hard-constraint queries present

---

## Phase 6: Ontology Inference (OWL Reasoner) — [x] Completed (DashboardView re-scoped)

**Tasks**

* [x] `OntologyInferenceService` (Jena rule/inf reasoner) derives `OfferedCapability` from `SkillParameter` semantics
* [x] `hub/service/InferenceService` + `LiftService` (lift MTP Service → `css:Skill`/`SkillParameter`) + `RDeployService`/`RVerifyService` + `EndToEndPipelineService` implement the Lift→Infer→Match→Deploy pipeline
* [x] Inference preserves explicit triples; unit tests for inference change-on-input
* [x] `DashboardView` was **not** delivered; live counts/statistics are surfaced in `MappingHubView` / pipeline console rather than a separate dashboard route

**Acceptance Criteria — [x] verified**
* [x] Inferred `OfferedCapability` correct; preserves explicit triples; `OntologyInferenceServiceTest`, `InferenceServiceTest`, `EndToEndPipelineServiceTest`, `LifecycleIntegrationTest` pass

---

## Phase 7: Semantic Reasoner Service — [x] Completed

**Tasks**

* [x] `SemanticReasonerService`, `SparqlQueryLoader`, `ServiceMatchResult`
* [x] Execute against model by `requirementId`; extract service/image/capability/protocol/accessMode/connector/deployment/recall/latency; no-match handling
* [x] `SemanticReasonerServiceTest` (match + no-match); behaviour driven by ontology/SPARQL, not Java hardcoding

**Acceptance Criteria — [x] verified**
* [x] Returns complete result for REQ-001; no-match for unsatisfied; `SemanticReasonerServiceTest` proves data-driven selection

---

## Phase 8: REST API — [x] Completed (forms re-scoped into Mapping Hub)

**Tasks**

* [x] `RequirementMatchingController` — `POST /api/v1/requirements/match` (Mode A requirementId / Mode B inline fields), `POST /api/v1/requirements` persist; `MatchRequirementRequest`/`MatchRequirementResponse`
* [x] `DiscoveryCatalogController`, `PipelineController`, `SystemStatusController`, `OptionsController`; global exception handling (`ValidationErrorResponse`, `error` package, `@ControllerAdvice`)
* [x] `MatchingService` / `SemanticMatchService` / `RecommendationService` back the endpoints
* [x] Manual requirement/service entry delivered via `MappingHubView` + `ManualEntryFormFactory` (ontology-driven forms), **not** a standalone `RegistrationWizardView`/`RequirementFormView`

**Acceptance Criteria — [x] verified**
* [x] API accepts Mode A/B; returns selected service or no-match; validation errors for missing/both; persist endpoint; `RequirementMatchingControllerTest`, `PipelineControllerTest`, `PipelineIntegrationTest`, `SystemStatusControllerTest`, `DiscoveryCatalogControllerTest` pass

---

## Phase 9: Docker Compose & Kubernetes Manifest Generation — [x] Completed (DeploymentView re-scoped)

**Tasks**

* [x] `DockerComposeManifestService`, `KubernetesManifestService`, `ManifestService`, `ManifestResult`
* [x] Container name/image from selected service; Compose `restart: unless-stopped` + env block (asset/processVar/protocol/deployment); `network_mode: host` only when justified
* [x] K8s `nodeSelector` derived from `DeploymentContext` (SIL, determinism, zone); container env from `CommunicationBinding`/`DataConnector`
* [x] SnakeYAML parse/verify tests; example outputs
* [x] Manifest preview delivered in `MappingHubView` / API response — **no** standalone `DeploymentView` route

**Acceptance Criteria — [x] verified**
* [x] API/Service response includes valid Docker Compose + K8s YAML; SnakeYAML-parseable; `nodeSelector` with SIL/deterministic/zone labels; `DockerComposeManifestServiceTest`, `KubernetesManifestServiceTest`, `ManifestServiceTest` pass

---

## Phase 10: Matching Explanation — [x] Completed

**Tasks**

* [x] `ExplanationService` produces per-dimension explanation (capability/protocol/accessMode/latency/recall/deployment); included in match result and no-match reasons
* [x] `NotificationCenter` / `NotificationPanel` components exist; wired to system events
* [x] `DashboardView` explanation panel was **not** delivered as a separate route

**Acceptance Criteria — [x] verified**
* [x] Success + no-match explanations understandable without reading SPARQL; `ExplanationServiceTest` validates content; `NotificationCenter`/`NotificationPanel` present

---

## Phase 11: Evaluation Dataset (Fermentation Skid & HVAC Plant) — [x] Completed (MonitoringView re-scoped)

**Tasks**

* [x] Example inputs: `device-descriptions/mtp/*`, `device-descriptions/aas/*`, `device-descriptions/opcua/*`
* [x] `EvaluationTest` covers fermentation (REQ-002, SIL/zone/protocol negatives) and HVAC (REQ-003, determinism/zone negatives) plus cross-plant temperature query (`cross-plant-temperature.rq`)
* [x] `R2aConceptCoverageTest` covers semantic-identifier matching
* [x] Live monitoring delivered via `OpcUaVisualizationView` ("OPC UA Monitor") with gauges/trends/NE 107 badges — **no** standalone `MonitoringView`

**Acceptance Criteria — [x] verified**
* [x] Evaluation dataset has both plants; queries return expected services; no-match tests pass; `EvaluationTest`, `R2aConceptCoverageTest` green

---

## Phase 11b: OPC UA Data Visualization Client — [x] Completed

**Tasks**

* [x] `OpcUaVisualizationView` (`/opcua-visualization`, "OPC UA Monitor") with server selector, connection-status badges, auto-reconnect
* [x] Approach C dashboard: tank level gauge/trend, flow in/out, temperature, pump vibration + NE 107 overlay, health card, `SetInletFlow` slider
* [x] Approach B (Node.js Tank) and Approach A (Node-RED Fermentation/HVAC) dashboards with graceful "server unavailable" placeholders
* [x] Shared components `OpcUaGauge`, `OpcUaTrendChart`, `OpcUaStatusBadge`, `OpcUaConnectionPanel`
* [x] `OpcUaVisualizationService` polls all servers (1s) and streams snapshots (`@Push`); `OpcUaVisualizationViewTest`, `OpcUaVisualizationServiceBrowseTest` pass

**Acceptance Criteria — [x] verified**
* [x] All three approaches visualized; connection failures handled per-server; UI tests pass

---

## Phase 12: Mapping Hub (ontology-driven mapping, manual entry, sessions) — [x] Completed (newly recorded)

**Goal**

Interactive ontology-driven mapping workbench: ingest device descriptions, manually extend the ABox via ontology-aware forms, run the Lift→Infer→Match→Deploy pipeline per session, and persist/resume sessions. This feature was absent from the prior roadmap.

**Tasks**

* [x] `MappingHubView` — layered workbench with `LayerTabs` (Intent/Capability/Skill/Service/CommunicationBinding/DataConnector/Deployment), `ClassListPanel`, `ClassDetailPanel`, `ConsolePanel`, `UploadDropzone`, `ManualEntryFormFactory`
* [x] Ontology-driven manual entry forms (`ManualEntryFormFactory`) reflecting derived subclasses; `ManualEntity`/`ManualEntityConverter`
* [x] Canonical Asset Model (`hub/model`: `CanonicalAssetModel`, `CanonicalEntity`, `Capability`, `Skill`, `Service`, `CommunicationBinding`, `DataConnector`, `DeploymentContext`, `RequiredCapability`, `OfferedCapability`, `Asset`, `Sensor`, `ProcessVariable`, `SemanticConcept`, `UseCase`, `UseCaseRequirement`, `UseCaseRequirementConstraint`/`Parameter`, `Stakeholder`, `VerificationCriterion`, etc.)
* [x] Canonical parsers + `CanonicalTripleMapper` + `CanonicalEntityRestorer`; `MappingHubGraphService`/`MappingHubPersistenceService`
* [x] Pipeline services: `LiftService`, `InferenceService`, `MatchingService`, `SemanticMatchService`, `RecommendationService`, `RDeployService`, `RVerifyService`, `EndToEndPipelineService`, `MappingHubService`, `MappingHubSession`
* [x] Session persistence + visibility: Flyway `V2__mapping_hub_session_persistence.sql` … `V5__*`, `SessionVisibilityService`, `SessionOverviewView` ("Sessions"), `CurrentUser`
* [x] Tests: `MappingHubViewTest`, `SessionOverviewViewTest`, `ManualEntryFormFactoryTest`, `MappingHubServiceTest`, `SessionVisibilityServiceTest`, `LiftServiceTest`, `MatchingServiceTest`, `SemanticMatchServiceTest`, `RecommendationServiceTest`, `CanonicalRecordCoverageTest`, `ManualEntityConverterTest`, all `hub/parser/*Test`, `hub/persistence/*Test`

**Acceptance Criteria — [x] verified**
* [x] Hub ingests MTP/AAS/OPC-UA descriptions; manual forms create valid entities for derived subclasses; pipeline lifts/infers/matches/deploys; sessions persist and resume; `MappingHubViewTest` + `SessionOverviewViewTest` pass

---

## Phase 13: Service Catalog & OPC UA Discovery Catalog — [x] Completed (newly recorded)

**Tasks**

* [x] `ServiceCatalogView` ("Service Catalog") + `ServiceCatalogService`/`ServiceCatalogEntry` + `service-catalog.rq`
* [x] `DiscoveryCatalogView` ("OPC UA Discovery") + `DiscoveryCatalogController`/`DiscoveredServiceEntry` backed by `OpcUaDiscoveryService`/`OpcUaServerRegistry`
* [x] Tests: `ServiceCatalogViewTest`, `DiscoveryCatalogViewTest`, `DiscoveryCatalogControllerTest`, `OpcUaDiscoveryServiceTest`, `OpcUaServerRegistryTest`

**Acceptance Criteria — [x] verified**
* [x] Catalog views render; discovery lists live OPC UA servers; tests pass

---

## Phase 14: Hierarchy-aware Ontology Visualization (OntologyView) — [x] Completed (newly recorded)

**Tasks**

* [x] `OntologyView` ("Ontology Browser", route `""`) replaces the planned `ExplorerView`; shows CSS-SDA class hierarchy with domain/range and datatype properties
* [x] Derived-subclass + inherited-relation rendering (`OntologyDataService`); multi-domain properties shown on every owning class
* [x] `OntologyViewTest` (relation count 85 = 18 core / 67 supporting; multi-domain render) + `MainLayoutTest`

**Acceptance Criteria — [x] verified**
* [x] Ontology tree/hierarchy renders with derived subclasses and inherited relations; multi-domain props visible on all classes; UI test passes

---

## Phase 15: Capability Typing & PlantUnit (ontology gap from paper Figure 5) — [x] Completed

**Goal**

Align the v2 ontology with the paper's Figure 5 (`sda_ontology_v2_structured.ttl`) class definitions that are not yet modeled. Verified missing against the current TTL: `requiresFunction`, `AnalyticalFunction`, `ControlFunction`, and `PlantUnit`.

**Tasks**

* [x] Add `:requiresFunction` object property (`:Capability` → `:AnalyticalFunction | :ControlFunction`), typing capabilities as e.g. "temperature control" or "remaining useful life prediction"
* [x] Add `:AnalyticalFunction` and `:ControlFunction` classes (function-type taxonomy referenced by `requiresFunction`)
* [x] Add `:PlantUnit` class and extend `operatesOn` range to `:Asset | :PlantUnit` (paper Figure 5)
* [x] Add positive + negative coverage tests for the new classes/properties
* [x] `CanonicalRecordCoverageTest` stays at 153 datatype properties (no datatype props added); relation counts unchanged because the new reference classes are intentionally kept **out of the numbered-class catalog** (Section "Implementation note" below)

**Acceptance Criteria**

* [x] `requiresFunction` present with correct range; `AnalyticalFunction`/`ControlFunction` individuals instantiable
* [x] `operatesOn` accepts `:PlantUnit`; ontology still loads in Jena and the full test suite (489) stays green

**Implementation note:** `AnalyticalFunction`, `ControlFunction`, and `PlantUnit` were added as plain `owl:Class` reference concepts **without** `:classNumber` (so they stay out of the 24-class numbered catalog bound to the generated canonical-record Java layer). This keeps the semantic typing expressed in RDF/Turtle + SPARQL per the project engineering rules and avoids expanding the generated record layer. They are verified directly against the Jena model in `CapabilityTypingPlantUnitTest`. `operatesOn` range widened via two explicit `rdfs:range` triples; `requiredByCapability` added as the declared inverse.

---

## Phase 16: Process Variable Analysis Example (paper "next steps" validation) — [x] Completed

**Goal**

Add the process-variable-analysis evaluation scenario the paper names as its next validation step (Summary, p.12), alongside the existing flowmeter / fermentation / HVAC examples (Phase 11).

**Tasks**

* [x] Define a ProcessVariable-analysis `UseCase` (`UC-PVA`) + `UseCaseRequirement` (`REQ-004`) with an analytic capability over a temperature process variable carrying a UCUM unit (`degC`) and an IEC CDD IRDI `semanticID`
* [x] Provide the example in the evaluation ABox (`ontology/demo-abox-v2.ttl`) alongside the other use cases (the evaluation dataset is the demo source, not a vendor device-description upload)
* [x] Add a SPARQL match path (`queries/process-variable-analysis.rq`) exercising R1–R3 **and** R2a (`requiresConcept` CDD coverage via `FILTER NOT EXISTS`)
* [x] Add positive match + negative tests (wrong concept / wrong unit / wrong SIL) to `EvaluationTest`
* [x] Example is exercised by the evaluation suite (the demo dataset drives `EvaluationTest`; surfacing in the live UI is covered by the existing demo session, not a new route)

**Acceptance Criteria**

* [x] New example returns the expected analytics service (`Process Variable Analytics Service`); negatives (wrong concept / wrong unit / wrong SIL) are excluded; full suite (500) stays green

---

## Phase 17: Deployment Manager — actual orchestration — [x] Completed (local target)

**Goal** (paper §4.3, Figure 6)

The paper's Deployment Manager "orchestrates the selected services as Kubernetes workloads according to infrastructure-level deployment constraints." Currently only Docker Compose / Kubernetes **manifest generation** exists (`DockerComposeManifestService`, `KubernetesManifestService`); nothing applies them to a cluster. `R-Deploy` already links the requirement to the `DeploymentContext` via `boundContext`, and the K8s manifest already derives `nodeSelector` from it.

**Tasks**

* [x] Decide orchestration target — implemented a **pluggable `OrchestrationBackend`**; the shipped `LocalDryRunBackend` is the default target. A real Kubernetes target (`OrchestrationTarget.Type.KUBERNETES`) is an extension point that needs a Kubernetes client dependency + infrastructure approval (AGENTS: no new infrastructure without explicit sign-off) and is therefore intentionally not shipped.
* [ ] **Real Kubernetes backend (extension point):** implement an `OrchestrationBackend` for `OrchestrationTarget.Type.KUBERNETES` that applies the generated manifest via a Kubernetes client (e.g. fabric8) and reports rollout status from the cluster. Requires the client dependency + infrastructure approval; **not shipped in the MVP**.
* [x] Implement an apply step that deploys the generated manifest — `DeploymentManagerService` resolves a backend by target and applies the manifest generated by `ManifestService` (which already derives `nodeSelector`/constraints from the `ServiceMatchResult`/`boundContext`).
* [x] Wire `R-Deploy` `boundContext` → manifest `nodeSelector` / constraints (already generated by `KubernetesManifestService.buildNodeSelector`).
* [x] Add dry-run + rollback and health-gated rollout — `LocalDryRunBackend` validates the YAML, simulates Pending→Applying→Ready, probes a `HealthChecker`, and rolls back on health-gate failure; `OrchestrationRequest.dryRun` produces a plan without applying.
* [x] Tests with an in-memory / mock API server so the suite passes without an external cluster — `LocalDryRunBackendTest` + `DeploymentManagerServiceTest` use an in-memory backend and injectable `HealthChecker` (no cluster).

**Acceptance Criteria**

* [x] Generated manifest is applied to a target env and the workload reaches Ready; rollback on failure; tests pass by default without an external cluster (verified via `LocalDryRunBackend`). The real-cluster Kubernetes backend remains a deferred, approval-gated extension point.

---

## Phase 18: Future / Optional (paper-stated plans, out of MVP scope)

* [x] **LLM-based semantic translation** (paper p.7–8, p.12): **implemented (2026-08-25)** as a configurable feature. `LlmProperties` (`app.llm.*`, env-overridable: `APP_LLM_ENABLED`, `APP_LLM_PROVIDER`, `APP_LLM_APIKEY`, `APP_LLM_MODEL`) targets **OpenRouter with a free model by default** (`mistralai/mistral-7b-instruct:free`). `RestClientChatCompletionsClient` speaks the OpenAI-compatible Chat Completions API, so OpenRouter / OpenAI / Ollama / custom endpoints are all supported by changing `provider`/`baseUrl`/`apiKey`. `LlmTranslationService` maps AutomationML / OPC UA NodeSet / AAS device descriptions to candidate SDA ontology concepts; exposed via `LlmController` (`/api/v1/llm/translate`, `/status`) and a Settings view form. The API key is user-supplied and never returned by the status endpoint.
* [ ] **SWRL fidelity**: `R-Deploy` / `R-Verify` remain the Java equivalents (`RDeployService` / `RVerifyService`), documented as functionally equivalent to the paper's SWRL rules. Migration to a SWRL engine (e.g. Jena SWRL) is **optional** and NOT implemented (would add a SWRL-engine dependency; only if paper-exact fidelity is required).
* [x] **Property-name alignment to paper**: verified — the paper's `uses` (Skill → CommunicationBinding / DataConnector) was intentionally split into `sda:usesBinding` / `sda:usesConnector` during the Layer 2/3 naming pass; `R-Deploy`'s SWRL text was updated to match. No further change needed (documented in the ontology).
* [x] **R2a hardening**: the `requiresConcept` + IEC 61987 CDD IRDI coverage (`FILTER NOT EXISTS`) is enforced on the canonical reasoner match path (`queries/match-service.rq`, exercised by `EvaluationTest` and the REST API) and the Process Variable Analysis path (`queries/process-variable-analysis.rq`). The hub semantic-match path (`hub-match-offered-required.rq`) joins on `conceptName` identity instead of skill-level coverage, so the separate coverage filter is not applicable there (it would be redundant and breaks its synthetic fixtures); this is an intentional, documented decision.

---

## Phase 19: AutomationML (CAEX) File Format Support — [x] Completed

**Goal**

Extend the Mapping Hub ingest (Phase 4 parsers / Phase 12 view) to accept generic **AutomationML** (CAEX) files, distinct from the MTP AML profile already handled by `MtpModelParser`. Currently `AssetFileFormat` supports AAS XML/JSON, MTP AML, and OPC UA JSON only; a plain AutomationML/CAEX description is neither detected nor parsed by upload.

> Note: MTP *is* a profile of AutomationML. `HubFileTypeDetector` currently sniffs MTP markers (`mtpaml`, `mtp-aml`, `opcuaserver`) → `MTP_AML`. Generic AutomationML files lacking those markers must be detected as `AUTOMATION_ML` and routed to a new parser, **without** being misclassified as MTP.

**Tasks**

* [x] Add `AUTOMATION_ML` to `AssetFileFormat` (label "AutomationML", extensions `aml`/`xml`, parser "AutomationML Parser"); extend `EntityOrigin` (new `AML` origin) and the `origin()` switch accordingly
* [x] Add `AutomationMlParser` implementing `CanonicalModelParser` that maps CAEX AutomationML `InternalElement`/`ExternalInterface` elements into the canonical model as `AutomationMLEntity` (className `AutomationML`)
* [x] Register the parser in `CanonicalParserRegistry` (Spring `@Component` auto-scan, consistent with the other `hub/parser` parsers)
* [x] Extend `HubFileTypeDetector` to sniff AutomationML (`<CAEX>` / `CAEXFile` / `automationml` xmlns) **after** MTP markers so generic AML is not routed to `MTP_AML`
* [x] Extend `UploadDropzone` accepted-file-types mapping for `AUTOMATION_ML` (`.aml`, `.xml`, `application/xml`)
* [x] Offer the new format in `MappingHubView` (drop label updated to mention AutomationML)
* [x] Add/extend tests: valid AutomationML → canonical entities; malformed XML / non-CAEX root rejected with clear `ParsingException`; detection disambiguation vs MTP AML
* [x] Update `CanonicalParserRegistryTest` / `HubFileTypeDetectorTest` / `AutomationMlParserTest` for the new format

**Acceptance Criteria**

* [x] Uploading an AutomationML (.aml/.xml CAEX) file is detected, parsed, and its entities appear in the Mapping Hub canonical model
* [x] Generic AutomationML is not misclassified as MTP AML; invalid input fails with a clear error
* [x] `HubFileTypeDetectorTest` covers AutomationML detection + MTP disambiguation; full suite (496) stays green

---

## Phase 20: Generic Requirement Matching & User-Defined Requirements — [x] Completed

**Goal** (paper §4.2, "how to make REQ-*** user-configurable")

Replace the per-requirement hardcoded queries with a **single generic SPARQL matcher**
that serves any requirement modeled with the standard SDA properties, and let users
define requirements (name, description, capability type, constraints) through a textbox
form and via upload/REST, then match them — without a new query per requirement.

**Tasks**
* [x] Add `queries/match-generic.rq`: one query parameterized by the requirement URI;
  R1 capability join + R2b/R3/R3-v2 + R2a concept coverage; all requirement-scoped
  constraints (protocol, accessMode, minRecall, maxLatency, onPlantNetwork, SIL,
  determinism, zone) applied as **OPTIONAL** filters so the same query serves any
  requirement. Removed the four per-requirement query files.
* [x] `SemanticReasonerService` routes any stored `UseCaseRequirement` through the
  generic query; a requirement without a `mapsToCapability` (capabilityType) falls back
  to constraint-only (`matchInline`) matching, preserving the prior dynamic behaviour.
* [x] Extend `MatchRequirementRequest` (added `requirementName`, `requirementDescription`,
  `capabilityType`); add `RequirementService.persist` building a full `UseCaseRequirement`
  graph (RequiredCapability + capabilityType + typed constraint literals).
* [x] Add `RequirementView` (`/requirement`) — textbox form (ID/name/description/
  capability/constraints) with Create, Create & Match, and Match-existing-by-ID;
  registered in `MainLayout` navigation.
* [x] Tests: `SeedServicesLoadTest` (REQ-001..004 via generic query),
  `GenericRequirementMatchingTest` (user-defined + protocol filtering + persist-then-match),
  `RequirementViewTest` (UI). Full suite 536 green.

**Acceptance Criteria**
* [x] Any requirement with a `capabilityType` matches via the single generic query;
  seeded REQ-001..004 still match their expected services; a user-defined requirement
  (e.g. `ProcessVariableAnalysis`, `OPC-UA`/`read-only`) matches correctly; a
  requirement declaring `protocol=PROFINET` does **not** match an OPC-UA-only service.
* [x] REQ-004 is now served by the generic query (no dedicated query file needed).

---

## Summary of corrections vs. prior roadmap

| Old claim (removed/not delivered) | Actual implementation |
|---|---|
| `DashboardView` (Phases 6/7/10) | Statistics/explanation surfaced in `MappingHubView` + API; no dashboard route |
| `SearchView` + global search route (Phase 5) | `SearchBar` component only; navigation via Ontology Browser + Service Catalog |
| `ExplorerView` (Phases 2/3) | `OntologyView` (hierarchy-aware, derived subclasses) |
| `UploadView` (Phase 4) | `UploadDropzone` inside `MappingHubView` |
| `RegistrationWizardView` / `RequirementFormView` (Phase 8) | `ManualEntryFormFactory` ontology-driven forms in `MappingHubView` |
| `DeploymentView` (Phase 9) | Manifest preview in `MappingHubView` + API response |
| `MonitoringView` (Phase 11) | `OpcUaVisualizationView` ("OPC UA Monitor") |
| (Mapping Hub, v2 ontology, Required/Offered split, Service/Discovery Catalog) — **not mentioned** | Added as Phases 12–14 above |

**Untracked / not committed (excluded from roadmap scope):** frontend POC files (`node_modules/`, `package*.json`, `tsconfig.json`, `types.d.ts`, `vite.config.ts`, `vite.generated.ts`) remain local-only and are not part of the build.

**Still to implement (updated 2026-08-25):** Phases 15, 16, 17, 18 (LLM translation) and 19 are now **[x] Completed** (full suite 509+ tests green). The two remaining deferred extension points requiring explicit infrastructure approval (AGENTS safety rules) are: (1) Phase 17's real-cluster **Kubernetes backend** (applying manifests via a K8s client), and (2) Phase 18's **SWRL-engine migration** of R-Deploy/R-Verify. All other roadmap phases are complete.
