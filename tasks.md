# Task History

Track completed work with validation evidence. Only the human engineer marks tasks complete.

## Format

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS]
- [ ] [BLOCKED]
```

---

## Phase 1: Project Foundation

**Status:** [x] Completed
**Branch:** `feature/2026-07-15-phase1-foundation`
**Completed:** 2026-07-15
**Tests:** 84 passing (0 failures)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 84 tests, 0 failures
- `mvn compile` — no errors
- All Phase 1 acceptance criteria marked `[x]` in `specs/roadmap.md`

### What was built

| Component | Files |
|---|---|
| Maven project (Spring Boot 4.0.7, Java 25) | `pom.xml` |
| Base package structure | `com.sda.mapper.{api,opcua,ontology,security,ui,deployment,error,reasoning}` |
| Application entry point | `SemanticSdaMapperApplication.java` |
| Vaadin UI foundation | `MainLayout.java`, `SystemStatusView.java`, `ServiceInstancesView.java`, `ThemeConfig.java`, `UIConfig.java` |
| 8 stub view classes | `OntologyExplorerView`, `RequirementFormView`, `RegistrationWizardView`, `SearchView`, `DashboardView`, `DeploymentView`, `MonitoringView`, `SettingsView` |
| Security | `SecurityConfig.java` (form-login, BCrypt, @Profile("!test")) |
| REST API | `SystemStatusController`, `OptionsController`, `OptionsConfigService`, `Option` record |
| OPC UA Server (Milo) | `TankOpcUaServer`, `TankNamespace`, `TankProcessState`, `SetInletFlow` method |
| OPC UA Client | `OpcUaClientWrapper`, `OpcUaClientFactory`, `OpcUaEndpointConfig`, `OpcUaNodeConfig` |
| NAMUR NE 107 | `PumpDegradationModel`, `Ne107DiagnosticSimulator` (with injectFault/clearFault/auto-recovery) |
| Health indicators | `OntologyReadinessHealthIndicator`, `SparqlQueryHealthIndicator`, `OpcUaSimulationHealthIndicator`, `TankSimulationHealthIndicator` |
| Ontology stub | `src/main/resources/ontology/css-sda-small.ttl` |
| SPARQL stub | `src/main/resources/queries/match-service.rq` |
| Options config | `src/main/resources/config/options.json` (7 categories) |
| Structured logging | `logback-spring.xml` (JSON prod, console dev), `logback-test.xml` |
| Observability | Micrometer Tracing + OTel bridge, OTLP exporter, Prometheus registry |
| Flyway | `V1__initial_schema.sql`, `spring.flyway.enabled=true` |
| Responsive CSS | `META-INF/resources/themes/sda-mapper/styles.css` |
| Docker | `docker/Dockerfile`, `docker-compose.yml` (nodered, tank-simulation, semantic-sda-mapper) |
| Test infrastructure | `src/test/resources/application.yml` (H2, test profile, Vaadin/security exclusions) |

### Test classes (16 total, 84 @Test methods)

| Test Class | Tests |
|---|---|
| `SemanticSdaMapperApplicationTest` | 1 |
| `ContainerHealthEndpointTest` | 1 |
| `OptionsConfigServiceTest` | 4 |
| `OptionsConfigServiceErrorTest` | 3 |
| `OptionsControllerTest` | 3 |
| `SystemStatusControllerTest` | 3 |
| `OntologyAndQueryLoadingTest` | 2 |
| `TankProcessStateTest` | 4 |
| `PumpDegradationModelTest` | 6 |
| `Ne107DiagnosticSimulatorTest` | 6 |
| `Ne107DiagnosticSimulatorExtendedTest` | 9 |
| `OpcUaClientFactoryTest` | 9 |
| `OpcUaClientWrapperTest` | 10 |
| `OpcUaEndpointConfigTest` | 10 |
| `OpcUaNodeConfigTest` | 10 |
| `MainLayoutTest` | 3 |

### Key decisions

- Spring Boot 4 uses `tools.jackson` (Jackson 3), not `com.fasterxml.jackson`
- Spring Boot 4 health classes in `org.springframework.boot.health.contributor`
- Spring Boot 4 security auto-config in `org.springframework.boot.security.autoconfigure`
- Vaadin 25.0.12 via `com.vaadin:vaadin-spring-boot-starter` (not Spring Boot managed)
- SecurityConfig uses `@Profile("!test")` to disable in test profile
- `RouterLink` requires VaadinService context — UI tests verify structure without instantiation
- Milo `UaMethodNode` requires 9-param constructor; `MethodInvocationHandler` in `sdk.server.api.methods`

---

## Phase 2: CSS-SDA Ontology Model

**Status:** [x] Completed
**Branch:** `feature/2026-07-15-phase2-css-sda-ontology` (merged to main)
**Completed:** 2026-07-15
**Tests:** 117 passing (0 failures) — ontology 538 triples, 17 classes

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 117 tests, 0 failures
- Ontology loaded: 538 triples, 17 classes
- All 3 use cases (A: Flowmeter, B: Fermentation, C: HVAC) validated via SPARQL
- All Phase 2 acceptance criteria marked `[x]` in `specs/roadmap.md`

### What was built

| Component | Files |
|---|---|
| CSS-SDA ontology (678 lines TTL) | `src/main/resources/ontology/css-sda-small.ttl` |
| 8 core classes + subclasses | Requirement, Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext, UseCase |
| Object properties (14) | mapsToCapability, realisedBy, isImplementedBy, usesBinding, usesConnector, isDeployedBy, hasNE107Status, hasSubCode, hasParameter, hasConstraint, specifiedBy, verifiedBy, requiresFunction, operatesOn, exposedAs, mapsTo |
| Data properties (24) | requirementID, title, description, rationale, acceptanceCondition, hasPriority, capabilityName, version, nominalRange_min, nominalRange_max, etc. |
| Use Case A instances | REQ-001, OC-ACM-Flowmeter, SK-FlowmeterDiagnostics, SVC-FlowmeterDiagnostics, CB-Flowmeter-OPCUA, DC-Flowmeter-FT101, DEPLOY-OnPrem |
| Use Case B instances | REQ-002, OC-TempControl, OC-DosingControl, SK-FermentationControl, SK-DosingControl, SVC-FermentationSkid, CB-Skid-A1-PROFINET, CB-Skid-A1-OPCUA, DC-Skid-A1-OPCUA, DEPLOY-Critical |
| Use Case C instances | REQ-003, OC-HVACTempMonitoring, SK-HVACTempAnalysis, SVC-HVACTempAnalysis, CB-AHU01-BACnet, CB-AHU01-OPCUA, DC-AHU01-OPCUA, CB-EM01-OPCUA, DC-EM01-OPCUA, DEPLOY-OTZone3 |
| NE 107 diagnostic ontology | NE107Status, NE107SubCode, DegradedMode classes + individuals |
| Negative candidates | SVC-WrongProtocol, SVC-WrongCapability, SVC-NE107Failure |
| SPARQL queries (4) | match-service.rq, match-service-req002.rq, match-service-req003.rq, cross-plant-temperature.rq |
| ExplorerView | Dynamic Jena-driven tree with Overview/Properties/Relationships tabs |

### Test classes (5 total, 33 @Test methods)

| Test Class | Tests |
|---|---|
| `OntologyAndQueryLoadingTest` | 7 |
| `OntologyModelLoaderTest` | 7 |
| `SparqlMatchingTest` | 10 |
| `OntologyExplorerViewTest` | 5 |
| `MainLayoutTest` | 4 |

---

## Phase 3: Ontology Loading with Apache Jena

**Status:** [x] Completed
**Branch:** `feature/2026-07-16-phase3-ontology-loading` (merged to main)
**Completed:** 2026-07-16
**Tests:** 117 passing (0 failures)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 117 tests, 0 failures
- Ontology loads at startup, fail-fast on missing/invalid file
- All Phase 3 acceptance criteria marked `[x]` in `specs/roadmap.md`

### What was built

| Component | Files |
|---|---|
| OntologyModelLoader | `src/main/java/com/sda/mapper/ontology/OntologyModelLoader.java` |
| OntologyLoadingException | `src/main/java/com/sda/mapper/error/OntologyLoadingException.java` |
| OntologyReadinessHealthIndicator | Delegates to OntologyModelLoader.isLoaded() |
| OntologyExplorerView (refactored) | Dynamic Jena tree from Model, not hardcoded |

### Test classes (1 new, 5 @Test methods)

| Test Class | Tests |
|---|---|
| `OntologyModelLoaderTest` | 5 (load, missing, invalid, getModelBeforeLoad, core classes) |

---

## Phase 4: Input File Parsers

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative across all phases

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- MTP AML parser parses valid XML with Service/Communication/DataAssembly blocks
- OPC UA Datasheet parser parses valid JSON with EndpointDescriptions/NodeSets
- UploadView accepts XML/JSON files and displays parse results

### What was built

| Component | Files |
|---|---|
| ParserException | `src/main/java/com/sda/mapper/error/ParserException.java` |
| Parser interface | `src/main/java/com/sda/mapper/parser/Parser.java` |
| ParseResult record | `src/main/java/com/sda/mapper/parser/ParseResult.java` |
| MTPAmlParser | `src/main/java/com/sda/mapper/parser/MTPAmlParser.java` |
| UaServerDatasheetParser | `src/main/java/com/sda/mapper/parser/UaServerDatasheetParser.java` |
| ParserFactory | `src/main/java/com/sda/mapper/parser/ParserFactory.java` |
| UploadService | `src/main/java/com/sda/mapper/parser/UploadService.java` |
| UploadView | `src/main/java/com/sda/mapper/ui/UploadView.java` |
| Test examples | `src/test/resources/examples/mtp-fermentation-skid.xml`, `uaserver-ahu01.json` |

### Test classes (4 new, 20 @Test methods)

| Test Class | Tests |
|---|---|
| `MTPAmlParserTest` | 6 |
| `UaServerDatasheetParserTest` | 6 |
| `ParserFactoryTest` | 4 |
| `UploadViewTest` | 4 |

---

## Phase 5: Search UI

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- SearchService finds ontology entities by text with heuristic scoring
- SearchView displays results in Grid with type filtering

### What was built

| Component | Files |
|---|---|
| SearchResult record | `src/main/java/com/sda/mapper/reasoning/SearchResult.java` |
| SearchService | `src/main/java/com/sda/mapper/reasoning/SearchService.java` |
| SearchBar component | `src/main/java/com/sda/mapper/ui/component/SearchBar.java` |
| SearchView (enhanced) | `src/main/java/com/sda/mapper/ui/SearchView.java` |

### Test classes (2 new, 10 @Test methods)

| Test Class | Tests |
|---|---|
| `SearchServiceTest` | 6 |
| `SearchViewTest` | 4 |

---

## Phase 6: Ontology Inference + Dashboard

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- OWL reasoner derives OfferedCapability from SkillParameters
- DashboardView shows 5 animated cards with live ontology counts

### What was built

| Component | Files |
|---|---|
| InferredCapability record | `src/main/java/com/sda/mapper/reasoning/InferredCapability.java` |
| OntologyInferenceService | `src/main/java/com/sda/mapper/reasoning/OntologyInferenceService.java` |
| DashboardCard component | `src/main/java/com/sda/mapper/ui/component/DashboardCard.java` |
| DashboardView (enhanced) | `src/main/java/com/sda/mapper/ui/DashboardView.java` |

### Test classes (2 new, 9 @Test methods)

| Test Class | Tests |
|---|---|
| `OntologyInferenceServiceTest` | 5 |
| `DashboardViewTest` | 4 |

---

## Phase 7: Semantic Reasoner Service

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- matchByRequirementId("REQ-001") returns SVC-FlowmeterDiagnostics
- matchInline with OPC-UA/read-only returns matching service
- No-match returned for nonexistent requirements

### What was built

| Component | Files |
|---|---|
| SparqlQueryLoader | `src/main/java/com/sda/mapper/reasoning/SparqlQueryLoader.java` |
| ServiceMatchResult record | `src/main/java/com/sda/mapper/reasoning/ServiceMatchResult.java` |
| SemanticReasonerService | `src/main/java/com/sda/mapper/reasoning/SemanticReasonerService.java` |
| SparqlExecutionException | `src/main/java/com/sda/mapper/reasoning/SparqlExecutionException.java` |

### Test classes (1 new, 10 @Test methods)

| Test Class | Tests |
|---|---|
| `SemanticReasonerServiceTest` | 10 |

---

## Phase 8: REST API

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- POST /api/v1/requirements/match with requirementId returns matched service
- POST /api/v1/requirements/match with inline fields returns matched service
- POST /api/v1/requirements persists new requirement to ontology
- 400 returned for validation errors (both modes, neither mode)

### What was built

| Component | Files |
|---|---|
| MatchRequirementRequest | `src/main/java/com/sda/mapper/api/dto/MatchRequirementRequest.java` |
| MatchRequirementResponse | `src/main/java/com/sda/mapper/api/dto/MatchRequirementResponse.java` |
| ValidationErrorResponse | `src/main/java/com/sda/mapper/api/dto/ValidationErrorResponse.java` |
| RequirementMatchingController | `src/main/java/com/sda/mapper/api/RequirementMatchingController.java` |
| RegistrationWizardView (enhanced) | 8-step wizard with API integration |
| RequirementFormView (enhanced) | Dropdowns from options.json, Match + Save buttons |

### Test classes (1 new, 5 @Test methods)

| Test Class | Tests |
|---|---|
| `RequirementMatchingControllerTest` | 5 |

---

## Phase 9: Manifest Generation

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- Docker Compose YAML contains service name, image, restart policy, env vars
- Kubernetes YAML contains Deployment, nodeSelector with SIL/deterministic/zone labels
- Both YAMLs parseable by SnakeYAML

### What was built

| Component | Files |
|---|---|
| ManifestResult record | `src/main/java/com/sda/mapper/deployment/ManifestResult.java` |
| DockerComposeManifestService | `src/main/java/com/sda/mapper/deployment/DockerComposeManifestService.java` |
| KubernetesManifestService | `src/main/java/com/sda/mapper/deployment/KubernetesManifestService.java` |
| ManifestService | `src/main/java/com/sda/mapper/deployment/ManifestService.java` |
| DeploymentView (enhanced) | Environment cards, Generate button, YAML display |

### Test classes (3 new, 19 @Test methods)

| Test Class | Tests |
|---|---|
| `DockerComposeManifestServiceTest` | 8 |
| `KubernetesManifestServiceTest` | 7 |
| `ManifestServiceTest` | 4 |

---

## Phase 10: Matching Explanation + NotificationCenter

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- ExplanationService generates human-readable match/no-match explanations
- NotificationCenter stores notifications with typed categories
- NotificationPanel shows bell icon with badge count

### What was built

| Component | Files |
|---|---|
| ExplanationService | `src/main/java/com/sda/mapper/reasoning/ExplanationService.java` |
| NotificationCenter | `src/main/java/com/sda/mapper/ui/component/NotificationCenter.java` |
| NotificationPanel | `src/main/java/com/sda/mapper/ui/component/NotificationPanel.java` |

### Test classes (1 new, 7 @Test methods)

| Test Class | Tests |
|---|---|
| `ExplanationServiceTest` | 7 |

---

## Phase 11: Evaluation Dataset + Monitoring

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- All 3 use cases (REQ-001/002/003) match correctly in EvaluationTest
- Cross-plant temperature query returns services from both plants
- MTP AML and OPC UA Datasheet parsing validated
- MonitoringView shows CPU, memory, ontology stats, uptime

### What was built

| Component | Files |
|---|---|
| MetricsService | `src/main/java/com/sda/mapper/monitoring/MetricsService.java` |
| MonitoringView (enhanced) | CPU/memory gauges, ontology stats, uptime display |
| Evaluation test examples | `src/test/resources/examples/mtp-fermentation-skid-complete.xml`, `uaserver-ahu01-complete.json` |

### Test classes (2 new, 9 @Test methods)

| Test Class | Tests |
|---|---|
| `EvaluationTest` | 6 |
| `MonitoringViewTest` | 3 |

---

## Phase 11b: OPC UA Data Visualization Client

**Status:** [x] Completed
**Completed:** 2026-07-16
**Tests:** 220 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 220 tests, 0 failures
- OpcUaVisualizationView polls Java Milo server every 2 seconds
- Tank section shows Level, FlowRateIn/Out, Temperature with color coding
- Pump section shows FlowRate, Vibration, NE 107 status badge, Health status
- Connection status indicator shows green when connected

### What was built

| Component | Files |
|---|---|
| OpcUaSnapshot record | `src/main/java/com/sda/mapper/opcua/OpcUaSnapshot.java` |
| OpcUaVisualizationService | `src/main/java/com/sda/mapper/opcua/OpcUaVisualizationService.java` |
| OpcUaVisualizationView | `src/main/java/com/sda/mapper/ui/OpcUaVisualizationView.java` |

### Test classes (1 new, 3 @Test methods)

| Test Class | Tests |
|---|---|
| `OpcUaVisualizationViewTest` | 3 |

---

## Bug Fixes & Improvements: OPC UA Multi-Server, OPC UA NodeSet Parser, End-to-End Pipeline

**Status:** [x] Completed
**Completed:** 2026-07-17
**Tests:** 253 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 251 tests, 0 failures
- All 3 requirements (REQ-001/002/003) match correctly via end-to-end pipeline
- OPC UA NodeSet XML parser parses real NodeSet format into SDA triples
- OPC UA multi-server registry enumerates built-in, external, and custom servers
- OpcUaVisualizationView displays server grid and per-server data cards
- ParserFactory routes XML files to correct parser via content sniffing

### What was built

| Component | Files |
|---|---|
| OpcUaServerRegistry | `src/main/java/com/sda/mapper/opcua/OpcUaServerRegistry.java` |
| OpcUaNodeSetParser | `src/main/java/com/sda/mapper/parser/OpcUaNodeSetParser.java` |
| EndToEndPipelineService | `src/main/java/com/sda/mapper/reasoning/EndToEndPipelineService.java` |
| OpcUaClientWrapper (browse) | `src/main/java/com/sda/mapper/opcua/OpcUaClientWrapper.java` |
| OpcUaVisualizationService (multi-server) | `src/main/java/com/sda/mapper/opcua/OpcUaVisualizationService.java` |
| OpcUaVisualizationView (server grid) | `src/main/java/com/sda/mapper/ui/OpcUaVisualizationView.java` |
| SettingsView (theme + profile) | `src/main/java/com/sda/mapper/ui/SettingsView.java` |
| MainLayout (search removed, logo removed) | `src/main/java/com/sda/mapper/ui/MainLayout.java` |
| OntologyExplorerView (dedup) | `src/main/java/com/sda/mapper/ui/OntologyExplorerView.java` |
| ParserFactory (content sniffing) | `src/main/java/com/sda/mapper/parser/ParserFactory.java` |
| ParseResult (model field) | `src/main/java/com/sda/mapper/parser/ParseResult.java` |
| MTPAmlParser (RDF triples) | `src/main/java/com/sda/mapper/parser/MTPAmlParser.java` |
| UaServerDatasheetParser (RDF triples) | `src/main/java/com/sda/mapper/parser/UaServerDatasheetParser.java` |
| UploadService (BufferedInputStream) | `src/main/java/com/sda/mapper/parser/UploadService.java` |
| SemanticReasonerService (dynamic filters) | `src/main/java/com/sda/mapper/reasoning/SemanticReasonerService.java` |
| RequirementMatchingController (manifests) | `src/main/java/com/sda/mapper/api/RequirementMatchingController.java` |
| MatchRequirementResponse (manifest fields) | `src/main/java/com/sda/mapper/api/dto/MatchRequirementResponse.java` |
| options.json (requirementType, functionClass) | `src/main/resources/config/options.json` |
| SPARQL queries (SELECT ?connector) | `src/main/resources/queries/match-service*.rq` |

### Test classes (4 new, 22 @Test methods)

| Test Class | Tests |
|---|---|
| `OpcUaServerRegistryTest` | 6 |
| `OpcUaNodeSetParserTest` | 9 |
| `EndToEndPipelineServiceTest` | 7 |
| `RequirementMatchingControllerTest` (extended) | +5 |
| `EvaluationTest` (extended) | +8 |
| `ParserFactoryTest` (extended) | +3 |
| `UploadViewTest` (extended) | +1 |

### Key decisions

- Content sniffing (4KB peek via `BufferedInputStream.mark/reset`) used instead of file extension to distinguish OPC UA NodeSet XML from MTP AML XML
- `OpcUaServerRegistry` uses probe pattern (2s timeout) to check server connectivity and supports listener pattern for UI updates
- `OpcUaClientWrapper.browse()` uses Milo 0.6.14 API: `BrowseDescription` constructor with `(NodeId, BrowseDirection, NodeId, Boolean, UInteger, UInteger)` and `BrowseResult.getReferences()` returning `ReferenceDescription[]`
- `.gitignore` updated to exclude `src/main/frontend/generated/`, `*.ipr`, `*.iws`

---

## Bug Fixes: OPC UA Read Attribute, Browse Detection, Server Grid

**Status:** [x] Completed
**Completed:** 2026-07-17
**Tests:** 253 passing (0 failures) — cumulative

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 253 tests, 0 failures
- OPC UA reads return actual values instead of `Variant{value=null}`
- Server grid height increased to 250px for better multi-server visibility
- Node browsing uses client NamespaceTable for proper type definition resolution
- Polling only refreshes selected server's browsed nodes (reduced log spam)

### What was built

| Component | Files |
|---|---|
| OpcUaClientWrapper (attribute fix) | `src/main/java/com/sda/mapper/opcua/OpcUaClientWrapper.java` |
| OpcUaVisualizationService (browse fix) | `src/main/java/com/sda/mapper/opcua/OpcUaVisualizationService.java` |
| OpcUaVisualizationView (grid + polling) | `src/main/java/com/sda/mapper/ui/OpcUaVisualizationView.java` |
| OpcUaVisualizationServiceBrowseTest | `src/test/java/com/sda/mapper/opcua/OpcUaVisualizationServiceBrowseTest.java` |

### Key decisions

- `ReadValueId(nodeId, UInteger.valueOf(13), null, null)` — explicit Value attribute ID (13) required by OPC UA; `null` attributeId causes "attribute unknown" error on server
- `isVariableNode()` uses client's NamespaceTable (not empty table) for ExpandedNodeId resolution
- Polling refreshes browsed nodes only when a server is selected in detail view

---

## Maintenance: Canonical Ontology Namespace Test Migration

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-03
**Tests:** 327 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 327 tests, 0 failures
- All tests migrated from the legacy `http://www.example.org/{sda,css}#` namespaces to the canonical TBox-only ontology `css-sda-unified.ttl` (`https://example.org/ontology/sda#`) plus a shared ABox fixture
- No remaining `http://www.example.org/(sda|css)#` references outside the read-only reference file `src/main/resources/ontology/css-sda-unified-old.ttl`
- Production `src/main` sources untouched except previously migrated files

### What was built

| Component | Files |
|---|---|
| Three-use-case ABox fixture (REQ-001/002/003, NE107, negative candidates, pump assets, R2a requiresConcept/mapsVariable/describedBy chain) | `src/test/resources/ontology/demo-abox.ttl` |
| Test helper merging the ABox into a loaded Jena model | `src/test/java/com/sda/mapper/support/DemoAboxBuilder.java` |
| ABox appended to Spring `app.ontology.file-paths` for Spring-context tests | `src/test/resources/application.yml` |

### Updated test classes

`EvaluationTest`, `SparqlMatchingTest`, `OntologyAndQueryLoadingTest`, `OntologyModelLoaderTest`, `LifecycleIntegrationTest`, `SemanticReasonerServiceTest`, `R2aConceptCoverageTest` (rewritten for the new requiresConcept/mapsVariable/describedBy chain), `RVerifyServiceTest`, `RequirementDiscoveryServiceTest`, `RDeployServiceTest`, `EndToEndPipelineServiceTest`, `OntologyInferenceServiceTest`, `DashboardViewTest`, `ServiceCatalogViewTest`, `OpcUaNodeSetParserTest`, `AasParserTest`

### Key technical decisions

- Hybrid fixture strategy: `demo-abox.ttl` shipped as a test resource and appended to the test `application.yml` `file-paths` (list-based) so Spring-managed `OntologyModelLoader` merges TBox + ABox; non-Spring unit tests use `DemoAboxBuilder.apply(loader.getModel())`.
- All test constants consolidated to `SDA_NS = "https://example.org/ontology/sda#"` and `INST_NS = "http://www.example.org/instance#"`; `CSS_NS` removed.
- `R2aConceptCoverageTest` rewritten semantically around `Skill → requiresConcept Concept`, `DataConnector → mapsVariable ProcessVariable`, `ProcessVariable → describedBy Concept`; restore test reloads the model and re-applies the ABox.
- ABox must be re-applied after every `loader.loadOntology()` in tests that reload to reset lifecycle state.

---

## Maintenance: Requirement Form Mandatory Fields (PipelineView)

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-03
**Tests:** 328 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 328 tests, 0 failures (was 327; +1 new UI test)
- Canonical ontology and `src/main/resources/ontology/css-sda-unified.ttl` untouched

### What was changed

Per the paper's Requirement model (roadmap citation "Islam, Maurer, Diedrich 2026"), a requirement is defined by its title and description. All functional parameters are optional and fall back to defaults during matching.

| Component | Files |
|---|---|
| Title + Description now mandatory (required indicators, validation, gate message); Protocol/Access Mode/Min Recall/Max Latency/Network Zone/Determinism/SIL optional with "(optional)" placeholders | `src/main/java/com/sda/mapper/ui/PipelineView.java` |
| `matchInline` falls back to defaults on empty/null params instead of returning no-match; `matchDynamicRequirement` no longer short-circuits when a requirement has no explicit params | `src/main/java/com/sda/mapper/reasoning/SemanticReasonerService.java` |
| PipelineViewTest rewritten: mandatory/optional semantics, error messages, required-indicator assertions (13 → 14 tests) | `src/test/java/com/sda/mapper/ui/PipelineViewTest.java` |
| SemanticReasonerServiceTest updated: empty/null inline params and parameterless dynamic requirements now match via defaults (renamed 3 tests) | `src/test/java/com/sda/mapper/reasoning/SemanticReasonerServiceTest.java` |

### Key technical decisions

- Mandatory-ness grounded in the project's own documentation of the paper's Requirement model (roadmap Figure 5, `docs/USER-TEST-MANUAL.md` section 5): title + description are the requirement identity; parameters/constraints are optional individuals (`hasParameter`/`hasConstraint`) in the ontology.
- The exact paper (Islam, Maurer, Diedrich 2026) is not publicly indexed; several web searches failed to retrieve it, so requiredness is derived from the project's documented model rather than a direct paper extract.
- REST Mode B semantics unchanged: `RequirementMatchingController` still rejects an empty inline body (400) before calling `matchInline`, so the default-fallback relaxation only affects the UI pipeline path and dynamically-created requirements.

---

## Maintenance: Ontology Visualizer (OntologyView)

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-03
**Tests:** 343 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 343 tests, 0 failures (was 328; +15 new view/data-service tests)
- Canonical `src/main/resources/ontology/css-sda-unified.ttl` untouched (479 triples)

### What was built

Interactive dark-themed ontology visualizer at `/visualizer` grounded in the canonical TBox: 5 layers, 24 classes, 62 object properties (prompt's 25/52 counts reconciled to the actual TBox — see decision below).

| Component | Files |
|---|---|
| Layer/class/relation records | `src/main/java/com/sda/mapper/model/OntologyLayer.java`, `OntologyClass.java`, `OntologyRelation.java` |
| Static grounded data holder with the six required queries | `src/main/java/com/sda/mapper/service/OntologyDataService.java` |
| View: header, stats pills, layer tabs, 280px class browser, inspector with neighborhood graph + core/supporting relation cards | `src/main/java/com/sda/mapper/ui/OntologyView.java` |
| Strict palette CSS (`.ontology-root`, `.section-title`, `.node`, tabs/cards/graph) with responsive stacking | `src/main/frontend/styles.css` |
| 15 tests: route/title, layer/class/relation counts, per-layer filtering, from/to relations, empty state, inspector rendering | `src/test/java/com/sda/mapper/ui/OntologyViewTest.java` |

### Key technical decisions

- Data reconciled to the canonical TBox (`css-sda-unified.ttl`), which holds **24** owl:Class and **62** owl:ObjectProperty declarations — not the prompt's stated 25/52 — to keep the visualizer fully grounded; `@Route("visualizer", layout = MainLayout.class)` avoids the `@Route("")` owned by `SystemStatusView`.
- Neighborhood graph rendered with plain `Div` + CSS flexbox (`.ontology-graph`/`.graph-side`/`.node`), no charting dependency; selected node uses `scale(1.1)` and layer pills use 13%-alpha tints (`colorHex + "22"`); core cards border `#3b82f6`, supporting `#94a3b8`.
- Class items are `Div` click targets (Vaadin `Button` hides `add(Component...)`), and `Element.getText()` returns only direct text nodes — tests use a recursive text collector.

---

## Mapping Hub: Universal Semantic Mapping Hub

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-03
**Tests:** 414 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 414 tests, 0 failures (was 343; +71 hub tests across 12 test classes)
- Application context now loads in `@SpringBootTest` after injecting the session via `@Lazy` scoped proxy
- `idea_build_project` — BUILD SUCCESS (deprecation warnings only, matching existing codebase usage)

### What was built

| Component | Files |
|---|---|
| Canonical model (interface + 25 records, model, formats, coverage/gap/recommendation types) | `src/main/java/com/sda/mapper/hub/model/*.java` |
| Parsers (detector, registry, AAS/MTP/OPC UA, `ParsingException`) | `src/main/java/com/sda/mapper/hub/parser/*.java` |
| Pipeline services (hub service, lift, inference, matching, recommendations, manifest export, session, result) | `src/main/java/com/sda/mapper/hub/service/*.java` |
| Hub UI (view, stepper, console, dashboard, layer tabs, class list/detail, manual entry factory, dropzone, recommendation bar) | `src/main/java/com/sda/mapper/hub/ui/*.java` |
| Dark-palette stylesheet | `src/main/frontend/hub-styles.css` |
| Drawer nav ("Mapping Hub" + "Ontology Browser") | `src/main/java/com/sda/mapper/ui/MainLayout.java` |
| 12 test classes (parsers, services, UI) | `src/test/java/com/sda/mapper/hub/**/*.java` |
| Fixtures for parser/hub tests | `src/test/resources/device-descriptions/` (canonical copies in `docs/usecases/`) |

### Key technical decisions

- Route is `@Route("mapping-hub", layout = MainLayout.class)`; detail panel is `ClassDetailPanel` + static `ManualEntryFormFactory` with per-class field lists.
- `MappingHubSession` stays `@VaadinSessionScope`; the singleton `MappingHubService` injects it via `@Lazy` (constructor parameter 7) so Spring creates a lazy proxy instead of resolving the Vaadin session scope at context startup — fixes the `ScopeNotActiveException` breaking all `@SpringBootTest` classes.
- `InferenceService` marks derived entities `inferred=true` via the canonical record constructors (convenience `sourceFileName` constructors default to explicit entities).
- `UploadDropzone` wraps the drop label in a `Div` (`setDropLabel` rejects a bare `Text` in Vaadin 25.2.1).
- `MappingHubView.selectLayer`/`selectClass` made public for interaction tests; `CoverageDashboard.getCount` and `ConsolePanel.getLines` added as test accessors.
- Weighted coverage is rounded to 1 decimal (`Math.round(x * 10) / 10.0`); tests assert the rounded value.

---

## Semantic Infer and Match Alignment (paper Steps 4–5)

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-04
**Tests:** 430 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 430 tests, 0 failures (was 414; +16 tests across 4 extended/new test classes)
- INFER now derives an `sda:OfferedCapability` per skill from `SkillParameter` semantics (Axiom A4: functionClass, engineeringUnit, nominalRange, SIL)
- MATCH now runs the SPARQL query `queries/hub-match-offered-required.rq` comparing each inferred `OfferedCapability` against each `RequiredCapability` across the capability, communication and deployment dimensions; matched skill, endpoint, protocol, network zone and SIL are projected
- Positive and negative semantic scenarios covered (functionClass, range containment, unit, SIL, protocol, network zone)

### What was built

| Component | Files |
|---|---|
| `OfferedCapability`, `RequiredCapability`, `CapabilityMatch` canonical records; `DeploymentContext.networkZone`; model support for all three | `src/main/java/com/sda/mapper/hub/model/*.java` |
| `SkillParameter.functionClass`; MTP/AAS parsers read `FunctionClass`; fixtures updated | `src/main/java/com/sda/mapper/hub/model/SkillParameter.java`, `src/main/java/com/sda/mapper/hub/parser/{Mtp,Aas}ModelParser.java`, `src/test/resources/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/` |
| Ontology: `OfferedCapability`/`RequiredCapability` classes + 9 datatype properties (functionClass, engineeringUnit, nominalRange_min/max, accuracy, safetyIntegrityLevel, networkZone, protocol, accessMode, endpointUrl) | `src/main/resources/ontology/css-sda-unified.ttl` |
| INFER rework — OfferedCapability per skill from primary parameter (functionClass-bearing preferred) | `src/main/java/com/sda/mapper/hub/service/InferenceService.java` |
| SPARQL match asset (offered vs required across three dimensions) | `src/main/resources/queries/hub-match-offered-required.rq` |
| Semantic MATCH service — instantiates canonical model + inferred entities into a Jena ABox, executes the query | `src/main/java/com/sda/mapper/hub/service/SemanticMatchService.java` |
| Pipeline wiring — `MappingHubService.runStages` runs the semantic match and logs each match; `PipelineResult` carries `matches` | `src/main/java/com/sda/mapper/hub/service/{MappingHubService,PipelineResult}.java` |
| Tests: `SemanticMatchServiceTest` (9), `InferenceServiceTest` (+3), `MtpModelParserTest` (+1), `OntologyAndQueryLoadingTest` (+2 vocabulary/query tests, +1 query in parse-all) | `src/test/java/com/sda/mapper/hub/service/*.java`, `src/test/java/com/sda/mapper/hub/parser/*.java`, `src/test/java/com/sda/mapper/ontology/*.java` |
| Test wiring updated for the new `MappingHubService` constructor argument | `src/test/java/com/sda/mapper/hub/{service/MappingHubServiceTest,ui/MappingHubViewTest}.java` |

### Key technical decisions

- The hub keeps its generic `Capability` inference so the 24-class coverage ledger and its 62.5%/24-class assertions stay stable; `OfferedCapability` is the paper's inferred capability sub-type consumed by the SPARQL match.
- `SemanticMatchService` builds an ABox-only in-memory Jena model (no `OntologyModelLoader` dependency) so the SPARQL path is testable without a Spring context; entity ids round-trip through a `uri → canonical id` map because sanitized URIs are not reversible.
- Until a dedicated Instantiate stage lands, the ABox builder uses documented MVP heuristics: every skill is implemented by every service, every service uses every binding and is deployed in every deployment context — exact for single-service/single-binding demo models.
- The match query treats the deployment `networkZone` and binding `endpointUrl` as OPTIONAL evidence (projected, filtered only when the requirement specifies them) so matches still resolve for models without deployment data; `functionClass` equality, unit compatibility, nominal-range containment and SIL sufficiency are hard filters.
- `DeploymentContext` gained a `networkZone` component (domain of `sda:networkZone` in the TTL); only `InferenceService` constructs it, so no other call sites were affected.

---

## Session Visibility View (user-wise list of incomplete / complete mapping sessions)

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-05
**Tests:** 451 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 451 tests, 0 failures (was 430; +21 tests across 3 new test classes and 1 extended class)
- New separate view `SessionOverviewView` at `/sessions` (own `@Route` + `@PageTitle`, `MainLayout`), drawer link "Sessions" directly under "Mapping Hub"
- Sessions are now user-attributed (`user_id` persisted via `ensureSession(UUID, String)` ON CONFLICT update; captured lazily from the authenticated principal)
- Incomplete/Complete is derived from persisted state: `COMPLETED` status → complete; `FAILED` → incomplete; otherwise complete only at `coveragePercent >= 100.0 && blockingGapCount == 0`
- Persisted sessions are resumable after restart: `resumeSession` restores `canonical_entity` rows into typed records (`CanonicalEntityRestorer`) and re-runs the pipeline to rebuild coverage/matches
- Security: admin sees all; non-admin sees own + unassigned; archive/delete/open allowed for owner or admin

### What was built

| Component | Files |
|---|---|
| Session visibility view: route, title, stylesheet, grid, summary chips, status/user/search filters, Open/Archive/Delete actions, refresh + 30s auto-refresh | `src/main/java/com/sda/mapper/hub/ui/SessionOverviewView.java` |
| Visibility scoping service — listSessions, per-user aggregates (with "Unassigned" bucket), canManage, archive, delete | `src/main/java/com/sda/mapper/hub/service/SessionVisibilityService.java` |
| Principal/admin resolution | `src/main/java/com/sda/mapper/hub/service/CurrentUser.java` |
| View data models — session overview record (with `complete()`), user summary record | `src/main/java/com/sda/mapper/hub/model/{SessionOverview,UserSessionSummary}.java` |
| Resume failure signal | `src/main/java/com/sda/mapper/hub/service/SessionResumeException.java` |
| Persistence additions — `ensureSession(UUID, String)`, `listSessions()` (single SQL + subquery counts), `archiveSession`, `loadCanonicalEntities` | `src/main/java/com/sda/mapper/hub/persistence/MappingHubPersistenceService.java` |
| Typed record reconstruction from persisted rows | `src/main/java/com/sda/mapper/hub/persistence/CanonicalEntityRestorer.java` |
| Resume orchestration — `resumeSession(UUID)`, user binding in `ensureSessionRow` | `src/main/java/com/sda/mapper/hub/service/{MappingHubService,MappingHubSession}.java` |
| Drawer link under Mapping Hub | `src/main/java/com/sda/mapper/ui/MainLayout.java` |
| Dark-palette styles for the sessions view | `src/main/frontend/hub-styles.css` |
| Owner lookup index | `src/main/resources/db/migration/V4__session_visibility_index.sql` |
| Tests: `SessionVisibilityServiceTest` (classification boundaries, scoping, aggregates, manage permissions), `SessionOverviewViewTest` (route/annotations, grid, chips, filters, actions, resume), `CanonicalEntityRestorerTest`, `MappingHubServiceTest` (+resume tests) | `src/test/java/com/sda/mapper/hub/{service,ui,persistence}/*.java` |

### Key technical decisions

- Completion is computed from persisted pipeline-run data so the view reflects durable state and is identical across restarts; `FAILED` is never complete even at 100% coverage.
- Grouping/aggregation happens in the service layer (flat `List<SessionOverview>` + per-user aggregates) so the Vaadin grid stays simple and the data is reusable by a future REST endpoint.
- Resume reuses the existing `runStages` orchestration: restore entities into the session model, then re-run LIFT/INFER/MATCH without a new persistence write or `startRun`.
- `CanonicalEntityRestorer` reconstructs typed records reflectively because every canonical record's component names match the persisted attribute keys.
- User attribution is lazy (first pipeline activity captures the principal) and back-filled on the persistence path via `ON CONFLICT DO UPDATE SET user_id`, so pre-existing unattributed sessions gain an owner on their next run.
- Notifications/navigation in the view go through `getUI().ifPresent(...)` so the view is unit-testable headless and never dereferences a null `VaadinSession`.

---

## Ontology Validation Test Suite (Categories A–E, 18-case catalog mapped onto css-sda-unified.ttl)

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-05
**Tests:** 467 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 467 tests, 0 failures (was 451; +16 tests in `SparqlMatchingTest`, 11 existing tests untouched and green)
- `mvn test -Dtest=SparqlMatchingTest` — 27 tests, 0 failures
- The uploaded Test Case Catalog targets `sda_ontology_v2_structured.ttl`, which does not exist in the repository; every catalog class name (EdgeContext, MTPSkill, HardConstrainedRequirement, MarginCompliantClusterNode, ...) was verified absent from `css-sda-unified.ttl`. All 18 cases were therefore mapped best-effort onto the real ontology vocabulary and implemented without new dependencies (Apache Jena OWL reasoner + SPARQL + existing `RDeployService`/`RVerifyService`).
- Positive and negative scenarios per category; matching logic stays in RDF/Turtle and SPARQL query files.

### What was built

| Component | Files |
|---|---|
| Extended matching/validation test class — existing 11 Use Case A/B/C + cross-plant tests preserved; added A1–A6 coherence/class-separation probes, B1–B3 auto-classification probes, C4 concept-coverage and C5 R0 hard-constraint negatives, D1–D2 R-Deploy/R-Verify lifecycle (positive + negative), E1–E2 Margo YAML → deployment placement | `src/test/java/com/sda/mapper/ontology/SparqlMatchingTest.java` |
| R0 hard-constraint pre-filter query (latency <= 60, recall >= 0.90 from `:REQ-001` constraints) | `src/main/resources/queries/r0-hard-constraint-prefilter.rq` |
| Deployment placement query — cluster-node supply satisfying the service's zone/SIL/determinism demand | `src/main/resources/queries/deployment-placement.rq` |
| Margo-style cluster-node descriptors (compatible + incompatible fixtures), lifted via SnakeYAML into ClusterNode + DeploymentContext triples | `src/test/resources/examples/margo-cluster-node-edge3.yaml`, `margo-cluster-node-edge1.yaml` |

### Key technical decisions

- No OWL API / HermiT / SnakeYAML-version additions: HermiT was dropped because the catalog's disjointness and defined-class probes were re-expressed with Jena's built-in `ReasonerRegistry.getOWLReasoner()` (same reasoner `OntologyInferenceService` uses), avoiding the `owlapi-rio` ↔ Jena 6.1.0 clash.
- Catalog concept mapping onto the repo ontology: OfferedCapability/RequiredCapability (both `⊑ Capability`) and NE107Status vs NE107SubCode are the only reasoner-provable separation/classification targets in the TTL; layer separation probes (L2 Capability/Skill/Service, L2b DeploymentContext/ClusterNode/NetworkZone/DeploymentPolicy/SafetyClass, device FieldDevice/BACnetDevice/EnergyMeter, L4 Asset/Sensor/ProcessVariable/Observation) cover the rest.
- C5 (R0 hard constraints) is expressed as a standalone SPARQL asset (`FILTER NOT EXISTS` over violated `hasConstraint` values), not Java, keeping constraint logic in the query layer.
- D1/D2 reuse the existing SWRL-equivalent services (`RDeployService`/`RVerifyService`/`SemanticReasonerService`) with additional negative scenarios (unmatched requirement stays unadvanced; failing criterion keeps `Matched`).
- E1/E2 interpret the catalog's `MarginCompliantClusterNode` as placement compatibility (DeploymentContext `containsNode` ClusterNode with equal zone/SIL/determinism), since no such class exists in the TTL.

---

## Maintenance: v2 Ontology Test Migration + Milo/Guava Test-Classpath Fix

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-05
**Tests:** 467 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn -B "-Dvaadin.skip=true" test` — BUILD SUCCESS, 467 tests, 0 failures, 0 errors
- Root cause of `OpcUaClientWrapper` `NoSuchMethodError`: surefire test classpath resolved `guava-18.0.jar` (via OWLAPI 4.5.6 transitive, nearest-wins) while the compile classpath had `guava-33.2.1-jre.jar`; Milo 0.6.14 needs `Tables.synchronizedTable` (guava ≥ 21)
- Fix: `<dependencyManagement>` pins `com.google.guava:guava:33.2.1-jre` in `pom.xml` (OWLAPI stays at 4.5.6) — `OpcUaClientWrapperTest` 10/10 green
- `src/test/resources/application.yml` now points at the v2 ABox `ontology/demo-abox-v2.ttl`; main `application.yml` already used only `sda_ontology_v2_structured.ttl`
- `demo-abox-v2.ttl`: added `:PV-Dosing-SkidA1` + `:OBS-DOSING-SkidA1-1` and extended `:DC-SkidA1-OPCUA` `sda:mapsVariable` to include `:PV-Dosing-SkidA1` (closes the REQ-002 Dosing concept-coverage chain); lowered `:SVC-Orphan` `reportedRecall` to 0.85 so the orphan never satisfies the 0.90 recall floor (was winning inline Mode B matches over `SVC-HVACTempAnalysis`)
- Remaining stale v1-namespace (`https://example.org/ontology/sda#`) test assertions migrated to v2 (`http://sda-platform.org/ontology#`, instance NS `http://www.example.org/instance#`) and to the real v2 vocabulary/individuals
- Runtime fix: `MappingHubPersistenceService.ensureSession(UUID, String)` writes the authenticated user name, but `mapping_hub_session.user_id` was created as `UUID` in V2 — new Flyway `V5__mapping_hub_session_user_id_text.sql` widens the column to `VARCHAR(255)` (PostgreSQL runtime and H2 PostgreSQL-mode tests)

### What was built / changed

| Component | Files |
|---|---|
| Guava 33.2.1-jre pin (dependencyManagement) | `pom.xml` |
| Test config → v2 ABox fixture | `src/test/resources/application.yml` |
| ABox Dosing coverage + orphan recall fix | `src/test/resources/ontology/demo-abox-v2.ttl` |
| Loader test: NS + real v2 property tests (requirement/capability/skill vocabulary) | `src/test/java/com/sda/mapper/ontology/OntologyModelLoaderTest.java` |
| Query-loading instance assertions → v2 ABox individuals (incl. negative candidate `SVC-Orphan`) | `src/test/java/com/sda/mapper/ontology/OntologyAndQueryLoadingTest.java` |
| Reasoning tests: NS + `DC-FT101-OPCUA` rename, `CRIT-REQ001-Recall`/`CRIT-REQ001-Latency` criterion names, no-criteria summary, dynamic-requirement vocabulary, inferred-capability assertions via `capabilityName()`/`sourceSkill()` | `src/test/java/com/sda/mapper/reasoning/{RDeployServiceTest,R2aConceptCoverageTest,RVerifyServiceTest,SemanticReasonerServiceTest,LifecycleIntegrationTest,OntologyInferenceServiceTest,EndToEndPipelineServiceTest}.java` |
| Evaluation/pipeline tests: NS fixes | `src/test/java/com/sda/mapper/evaluation/EvaluationTest.java` |

### Key technical decisions

- Fixed test fixtures and the ABox, not production code — production code was verified v2-clean across reasoners, parsers and all SPARQL query files.
- Guava resolved via dependencyManagement rather than excluding OWLAPI's transitive or bumping OWLAPI, keeping the pinned OWLAPI 4.5.6 and satisfying Milo 0.6.14.
- Completed the REQ-002 chain in the demo ABox (`SK-FermentationControl → requiresConcept PA-DIM-DosingConcept → PV-Dosing-SkidA1 → describedBy DC-SkidA1-OPCUA → SVC-FermentationSkid`) instead of relaxing the R2a concept-coverage assertions.
- Rewrote stale v1-property expectations to the real v2 vocabulary (`requirementName`/`requirementDescription`/`capabilityVersion`/`skillID`/`skillName`/`skillVersion`); dropped assertions on v1-only/absent constructs (`title`, `rationale`, `acceptanceCondition`, `hasPriority`, `nominalRange_*`, `engineeringUnit`, `OfferedCapability`).

---

## Maintenance: v2 Ontology Production Migration — Jena-Derived Visualizer Catalog + UseCaseRequirement Record Renames

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-06
**Tests:** 468 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn -B clean "-Dvaadin.skip=true" test` — BUILD SUCCESS, 468 tests, 0 failures, 0 errors
- `OntologyDataService` derives the ontology-visualizer catalog at runtime from the loaded v2 TBox (`sda_ontology_v2_structured.ttl`, 1602 triples) instead of hardcoded lists, so the catalog cannot drift from the ontology
- Hub records renamed to the v2 vocabulary: `Requirement` → `UseCaseRequirement`, `RequirementParameter` → `UseCaseRequirementParameter`, `RequirementConstraint` → `UseCaseRequirementConstraint` (IntelliJ rename refactoring applied to all usages including string literals)
- v1 → v2 aliases retained in `CanonicalEntityRestorer` and `ManualEntityConverter` so persisted v1 rows remain restorable without a data migration
- `demo-abox-v2.ttl` requirement literals updated to the `UseCaseRequirement` suffix, fixing `OntologyModelLoaderTest.shouldContainRequirementProperties`
- No roadmap markers changed: this work is production-code maintenance with no open roadmap task/acceptance criteria of its own

### What was built / changed

| Component | Files |
|---|---|
| Jena-derived visualizer catalog — layers (via `:ontologyLayer`), classes (via `:classNumber`), relations (labeled `owl:ObjectProperty` with class domain/range), core vs supporting via `CORE_PROPERTIES`; `@Autowired` ctor taking `OntologyModelLoader` + no-arg ctor loading the default TTL | `src/main/java/com/sda/mapper/service/OntologyDataService.java` |
| Renamed records (files renamed) | `src/main/java/com/sda/mapper/hub/model/UseCaseRequirement.java`, `UseCaseRequirementParameter.java`, `UseCaseRequirementConstraint.java` |
| Cascade updates for renamed records (typed lists, switches, `REQUIRED_FIELDS` keys, form specs, dropdown refs) | `src/main/java/com/sda/mapper/hub/model/CanonicalAssetModel.java`, `hub/service/MatchingService.java`, `hub/ui/ManualEntryFormFactory.java`, `hub/ui/ClassDetailPanel.java` |
| v1 aliases + deterministic canonical-constructor invocation (`getDeclaredConstructor(componentTypes)` instead of `getConstructors()[0]`) | `src/main/java/com/sda/mapper/hub/persistence/CanonicalEntityRestorer.java` |
| v1 aliases in the converter switch | `src/main/java/com/sda/mapper/hub/model/ManualEntityConverter.java` |
| Derived-data assertions (24 classes / 89 relations; 22 core / 67 supporting; Service 9 outgoing / 9 incoming) | `src/test/java/com/sda/mapper/ui/OntologyViewTest.java` |
| v2 form element IDs (`hub-field-usecaserequirement-*`) | `src/test/java/com/sda/mapper/hub/ui/ManualEntryFormFactoryTest.java` |
| v2 requirement literals for REQ-001/002/003 | `src/test/resources/ontology/demo-abox-v2.ttl` |

### Key technical decisions

- The visualizer reads the TBox instead of a static snapshot so layer/class/relation data always matches the versioned, reviewable ontology file; class annotations (`:classNumber`, `:ontologyLayer`) in the TTL are the single source of truth.
- v1 className aliases live in the restorer/converter rather than migrating persisted rows, because the DB stores class names as strings; new sessions use the v2 names.
- `CanonicalEntityRestorer` resolves the record's canonical constructor deterministically via `getDeclaredConstructor(RecordComponent[] types)` — `getConstructors()[0]` ordering is unspecified across JDKs and produced XML-parse failures in `CanonicalEntityRestorerTest`.
- Fixture updated (not the test): the ABox is the v2 vocabulary's instance data, so instance labels/names follow the class rename to `UseCaseRequirement`.

---

## Maintenance: Instance-Namespace Cleanup — `example.org` Removed from Production Identity Bases

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-06
**Tests:** 468 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn -B clean "-Dvaadin.skip=true" test` — BUILD SUCCESS, 468 tests, 0 failures, 0 errors (unchanged count; pure identity-base rename)
- Production identity bases moved off the RFC 2606 documentation domain `example.org` onto the project domain: instance base `http://www.example.org/instance#` → `http://sda-platform.org/instance#`, mapping-hub persistence base → `http://sda-platform.org/mapping-hub/`, lifted-concept base → `http://sda-platform.org/concept/`
- `example.org` now remains only in test/example fixture data and vendor semantic-IRI literals (AAS identifiers, MTP AML XML namespaces, `connectorSemanticIRI`, `mtp/descriptor` literal) plus historical `tasks.md` records

### What was built / changed

| Component | Files |
|---|---|
| Instance base constant `http://www.example.org/instance#` → `http://sda-platform.org/instance#` | 10 production classes (`RequirementMatchingController`, `RDeployService`, `RVerifyService`, `SemanticReasonerService` incl. inline `PREFIX`, `EndToEndPipelineService`, `SemanticMatchService`, `AasParser`, `MTPAmlParser`, `OpcUaNodeSetParser`, `UaServerDatasheetParser`), 4 SPARQL queries (`match-service*.rq`, `r0-hard-constraint-prefilter.rq`) |
| Instance base in ABox + tests | `demo-abox-v2.ttl` (prefix + header comment), `DemoAboxBuilder` javadoc, 9 test classes (INST_NS constants and inline URIs) |
| Mapping-hub persistence base → `http://sda-platform.org/mapping-hub/` | `CanonicalTripleMapper.java` |
| Lifted-concept base → `http://sda-platform.org/concept/` | `LiftService.java` |

### Key technical decisions

- Instance base, queries and the ABox fixture were moved **together**: they share one identity space, so a base change in production code alone would have desynchronized matching (production injects instances at the base the queries/ABox must resolve).
- Left `example.org` untouched in fixture/vendor data (AAS `identifier`/`globalAssetId`/`value` IRIs, MTP AML `xmlns`, `connectorSemanticIRI`, `https://example.org/mtp/descriptor` literal) — those are sample/vendor-document values, exactly what `example.org` is reserved for.
- No persisted-data migration needed at this stage: the change is identity-base rename only, and the MVP database is dev/demo (persisted hub triples at the old base would need re-running through the pipeline).

---

## Maintenance: Ontology-Driven Manual Entry Forms + Derived Subclass Handling

**Status:** [x] Completed
**Branch:** `feature/2026-08-06-hub-ontology-driven`
**Completed:** 2026-08-06
**Tests:** 479 passing (0 failures, 0 errors, 0 skipped)

### Completion Evidence

- `mvn -B clean "-Dvaadin.skip=true" test` — BUILD SUCCESS, 479 tests, 0 failures, 0 errors (was 468; +11 tests)
- Manual entry forms are now derived from the v2 TBox (`owl:DatatypeProperty` domains + object-property domains) instead of hardcoded per-class field lists
- All v2 derived subclasses (AASSkill, AARequirement, ...) render in the class browser indented under their numbered parent with a "derived" pill and a "subclass of <parent>" badge

### What was built / changed

| Component | Files |
|---|---|
| Datatype property derivation from the v2 TBox (`owl:DatatypeProperty` + known-class `rdfs:domain`) | `OntologyDataService.java` (new `deriveDatatypeProperties`, `getDatatypeProperties()`, `getDatatypeProperties(className)`) |
| New `OntologyDatatypeProperty` record (name, className, xsdType) | `model/OntologyDatatypeProperty.java` (new) |
| Ontology-first field hub-key mapping (`instanceID → name`, `contactInfo → contact`, ...) | `hub/model/AttributeBindings.java` (new) |
| Ontology-driven `ManualEntryFormFactory` — field IDs/forms/rejections derived from ontology, `Field` variants (modelRef, curated, hubKey, section, staticText, prefill), instance-based `supports`/`create` | `hub/ui/ManualEntryFormFactory.java` |
| Derived subclass flattening (numbered class + subclasses per layer), derived-cap → parent redirect, factory injection | `hub/ui/MappingHubView.java` |
| Indented derived rows + "derived" pill, coverage/count via parent | `hub/ui/ClassListPanel.java` |
| "subclass of <parent>" badge, parent-based coverage resolution | `hub/ui/ClassDetailPanel.java` |
| Required attributes validated against ontology datatype properties at construction | `hub/service/MatchingService.java` |
| Styles for derived pills and subclass badge | `frontend/hub-styles.css` |
| Derived hierarchy / datatype-property / field-rejection / validation tests | `hub/model/CanonicalRecordCoverageTest.java` (new, 3), `ui/OntologyViewTest.java` (19), `hub/ui/MappingHubViewTest.java` (17), `hub/ui/ManualEntryFormFactoryTest.java` (8), `hub/service/MatchingServiceTest.java` (9) |
| Roadmap Phase 11c-5 entry | `specs/roadmap.md` |

### Key technical decisions

- Manual entry forms are derived from the ontology rather than hardcoded Java field lists, consistent with the AGENTS rule to keep semantic rules in RDF/Turtle/SPARQL and avoid hardcoded Java selection rules.
- Derived subclasses reuse the numbered parent's coverage entry because instances are always typed on the numbered parent class; the browser shows them as indented sub-rows instead of 24+ separate entries.
- Selecting a derived cap auto-redirects to the numbered parent for gap actions (only stackable-to-parent leaves call `selectParent` directly), so users never see an empty action panel for a derived class.
- `MatchingService` validates required attributes against ontology datatype properties at construction so ontology drift fails fast with a clear `IllegalStateException` instead of silently missing coverage.

---

## Phase 15: Capability Typing & PlantUnit (SDA-OM extension)

**Status:** [x] Completed
**Branch:** `feature/2026-08-25-remaining-phases`
**Completed:** 2026-08-25
**Tests:** 489 passing (0 failures, 0 errors, 0 skipped) — was 484; +5 new tests

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 489 tests, 0 failures
- Ontology TBox extended with capability-typing + PlantUnit reference concepts; verified via `CapabilityTypingPlantUnitTest` (Jena model, positive + negative)
- `OntologyViewTest` counts unchanged — new reference classes kept out of the numbered catalog to avoid regenerating 46 canonical-record Java classes (`CanonicalRecordCoverageTest` requires a Java class per numbered class)

### What was built

| Component | Files |
|---|---|
| TTL: `:requiresFunction` + inverse `:requiredByCapability` (Capability → `:AnalyticalFunction | :ControlFunction`); `:AnalyticalFunction`/`:ControlFunction`/`:PlantUnit` as plain `owl:Class` reference concepts; widened `:operatesOn` range to `:Asset | :PlantUnit` | `src/main/resources/ontology/sda_ontology_v2_structured.ttl` |
| Capability-typing TBox coverage test (positive: Capability requires AnalyticalFunction; negative: Capability does not map to Asset; inverse asymmetry) | `src/test/java/com/sda/mapper/service/CapabilityTypingPlantUnitTest.java` |

### Key technical decisions

- New classes kept non-numbered (no `:classNumber`) so they remain RDF/SPARQL-native reference concepts validated by the Jena model, not by the numbered visualizer contract — keeps the 24-class catalog and `OntologyViewTest` (85 relations / 24 classes) stable and avoids 46 generated canonical-record Java classes (`CanonicalRecordCoverageTest` would otherwise require a Java class per numbered class).

---

## Phase 16: Process Variable Analysis (paper "next steps" validation)

**Status:** [x] Completed
**Branch:** `feature/2026-08-25-remaining-phases`
**Completed:** 2026-08-25
**Tests:** 500 passing (0 failures, 0 errors, 0 skipped) — cumulative; +4 new `EvaluationTest` methods (18 total in `EvaluationTest`)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 500 tests, 0 failures
- `UC-PVA` / `REQ-004` matches `SVC-ProcessVariableAnalytics` via the process-variable analysis path; 3 negative scenarios (wrong concept / wrong unit / wrong SIL) excluded
- SPARQL path exercises R1–R3 + R2a (`requiresConcept` CDD coverage via `FILTER NOT EXISTS`)

### What was built

| Component | Files |
|---|---|
| PVA example (`UC-PVA`, `REQ-004`, capability/skill/service/connector/PV with UCUM `degC` + IEC CDD IRDI `semanticID`, deployed in OT-Zone-1) | `src/test/resources/ontology/demo-abox-v2.ttl` |
| SPARQL match path (R1–R3 + R2a) | `src/main/resources/queries/process-variable-analysis.rq` |
| Positive + 3 negative `EvaluationTest` methods | `src/test/java/com/sda/mapper/evaluation/EvaluationTest.java` |

### Key technical decisions

- Example data lives in the evaluation ABox (the demo data source) rather than a vendor device-description upload; PVA deployed in OT-Zone-1 so the existing `shouldMatchModeBWithNetworkZoneFilter` (OT-Zone-3) stays uniquely HVAC and no regression occurs.
- Validated the paper's "process variable with unit + semanticID" next-step item via SPARQL, keeping matching logic in RDF/Turtle + queries per engineering rules.

---

## Phase 19: AutomationML (CAEX) Ingestion

**Status:** [x] Completed
**Branch:** `feature/2026-08-25-remaining-phases`
**Completed:** 2026-08-25
**Tests:** 496 passing (0 failures, 0 errors, 0 skipped) at this phase boundary (489 → 496; +7); full suite 500 after Phase 16

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 496 tests, 0 failures (`AutomationMlParserTest` + extended `HubFileTypeDetectorTest` / `CanonicalParserRegistryTest` green)
- Hub file-type detector sniffs CAEX/automationml **after** MTP markers (no false MTP classification)
- `AutomationMlParser` lifts CAEX `InternalElement`/`ExternalInterface` into canonical AutomationML entities consumed by the hub pipeline

### What was built

| Component | Files |
|---|---|
| `AssetFileFormat.AUTOMATION_ML` + `EntityOrigin.AML` | `src/main/java/com/sda/mapper/hub/model/AssetFileFormat.java`, `src/main/java/com/sda/mapper/hub/model/EntityOrigin.java` |
| CAEX sniff (after MTP markers) + `origin()` switch | `src/main/java/com/sda/mapper/hub/parser/HubFileTypeDetector.java` |
| `AutomationMlParser` (`CanonicalModelParser`) + `AutomationMLEntity` (className `"AutomationML"`) | `src/main/java/com/sda/mapper/hub/parser/AutomationMlParser.java`, `src/main/java/com/sda/mapper/hub/model/AutomationMLEntity.java` |
| `CanonicalAssetModel` `automationMLEntities` bucket + getter | `src/main/java/com/sda/mapper/hub/model/CanonicalAssetModel.java` |
| `UploadDropzone` mime types + `MappingHubView` drop label/`formatClass` | `src/main/java/com/sda/mapper/hub/ui/UploadDropzone.java`, `src/main/java/com/sda/mapper/hub/ui/MappingHubView.java` |
| Sample CAEX fixture | `src/test/resources/device-descriptions/automationml/sample-caex.aml` |
| Tests: `AutomationMlParserTest` (new), `HubFileTypeDetectorTest` (+), `CanonicalParserRegistryTest` (+) | `src/test/java/com/sda/mapper/hub/parser/*.java` |

### Key technical decisions

- Generic CAEX mapped to a single new `AutomationML` canonical entity type (`caexElementType` stored in attributes) rather than forced into MTP-specific Skill/Service vocabulary, keeping it format-agnostic.
- Sniffing checks MTP markers (`RoleClassLibrary`/`Service`) FIRST and falls through to CAEX/automationml, so AML files are not misclassified as MTP.

---

## Phase 17: Deployment Manager — actual orchestration

**Status:** [x] Completed
**Branch:** `feature/2026-08-25-remaining-phases`
**Completed:** 2026-08-25
**Tests:** 509 passing (0 failures, 0 errors, 0 skipped) cumulative; +9 new tests (`LocalDryRunBackendTest` 6, `DeploymentManagerServiceTest` 3)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 509 tests, 0 failures
- `DeploymentManagerService` generates manifests via `ManifestService` (nodeSelector derived from `boundContext`) and applies them via the resolved `OrchestrationBackend`
- `LocalDryRunBackend`: valid manifest → `READY`; failing `HealthChecker` → `ROLLED_BACK`; dry-run → `PLANNED`; invalid YAML → `FAILED`; observer records every transition; only supports `LOCAL_DRY_RUN`

### What was built

| Component | Files |
|---|---|
| Rollout lifecycle enum | `src/main/java/com/sda/mapper/deployment/orchestration/RolloutStatus.java` |
| Health probe status + checker interface + NoOp default | `src/main/java/com/sda/mapper/deployment/orchestration/{HealthStatus,HealthChecker,NoOpHealthChecker}.java` |
| Orchestration target / request / result records | `src/main/java/com/sda/mapper/deployment/orchestration/{OrchestrationTarget,OrchestrationRequest,OrchestrationResult}.java` |
| Rollout observer + backend interface | `src/main/java/com/sda/mapper/deployment/orchestration/{RolloutObserver,OrchestrationBackend}.java` |
| Dependency-free local dry-run backend (validate, health-gate, rollback, dry-run) | `src/main/java/com/sda/mapper/deployment/orchestration/LocalDryRunBackend.java` |
| Deployment Manager wiring `ManifestService` → backend | `src/main/java/com/sda/mapper/deployment/orchestration/DeploymentManagerService.java` |
| Tests (in-memory backend, injectable `HealthChecker`, no cluster) | `src/test/java/com/sda/mapper/deployment/orchestration/{LocalDryRunBackendTest,DeploymentManagerServiceTest}.java` |

### Key technical decisions

- Pluggable `OrchestrationBackend`: the shipped `LocalDryRunBackend` needs no external cluster/client, so the suite is green by default and health-gated rollback is exercised via an injectable `HealthChecker` (NoOp = UP, or a failing checker to drive rollback).
- Real Kubernetes backend (`OrchestrationTarget.Type.KUBERNETES`) left as an extension point requiring a Kubernetes client dependency + infrastructure approval, consistent with AGENTS "no new infrastructure without explicit sign-off."
- `nodeSelector` / deployment constraints are already derived by `KubernetesManifestService` from the `ServiceMatchResult` (which carries the R-Deploy `boundContext`), so no extra wiring was needed.

---

## Phase 18: Future / Optional (paper-stated plans)

**Status:** [x] Completed — LLM-based semantic translation implemented & configurable (OpenRouter free default); SWRL-engine migration still deferred/optional
**Branch:** `feature/2026-08-25-remaining-phases`
**Completed:** 2026-08-25
**Tests:** 523 passing (0 failures, 0 errors, 0 skipped) — +14 new LLM tests

### Completion Evidence

- Property-name alignment verified: paper `uses` intentionally split into `sda:usesBinding` / `sda:usesConnector` (Layer 2/3 naming pass; `R-Deploy` SWRL text updated) — no code change required.
- R2a concept-coverage (`requiresConcept` + CDD IRDI) enforced on the canonical reasoner match path (`queries/match-service.rq`, exercised by `EvaluationTest`/REST) and the Process Variable Analysis path (`queries/process-variable-analysis.rq`). Hub match path joins on `conceptName` identity, so the separate coverage filter is not applicable there (documented; adding it broke its synthetic fixtures).
- **LLM-based semantic translation implemented (2026-08-25):** `LlmProperties` (`app.llm.*`, env-overridable `APP_LLM_ENABLED`/`APP_LLM_PROVIDER`/`APP_LLM_APIKEY`/`APP_LLM_MODEL`) targets **OpenRouter with a free model by default**. `RestClientChatCompletionsClient` speaks the OpenAI-compatible Chat Completions API, so OpenRouter / OpenAI / Ollama / custom endpoints are all selectable by `provider`/`baseUrl`/`apiKey`. `LlmTranslationService` maps AutomationML / OPC UA NodeSet / AAS device descriptions to candidate SDA ontology concepts; exposed via `LlmController` (`/api/v1/llm/translate`, `/status`) and a Settings view form. API key is user-supplied, never returned by status. Tests use a stub `ChatCompletionsClient` (no external call).
- SWRL-engine migration of R-Deploy/R-Verify intentionally **deferred** (optional; would add a SWRL-engine dependency; only if paper-exact fidelity is required).

### What was built / changed

| Component | Files |
|---|---|
| LLM configuration (OpenRouter free default) | `src/main/java/com/sda/mapper/llm/LlmProperties.java`, `src/main/resources/application.yml` (`app.llm.*`) |
| Provider presets + chat transport interface | `src/main/java/com/sda/mapper/llm/LlmProvider.java`, `ChatCompletionsClient.java` |
| OpenAI-compatible client (OpenRouter headers) | `src/main/java/com/sda/mapper/llm/RestClientChatCompletionsClient.java` |
| Translation service + records + exception | `src/main/java/com/sda/mapper/llm/{LlmTranslationService,ConceptHint,TranslationResult,LlmNotConfiguredException}.java` |
| REST API | `src/main/java/com/sda/mapper/api/LlmController.java`, `api/dto/{LlmTranslateRequest,LlmStatusResponse}.java` |
| Settings view LLM config form | `src/main/java/com/sda/mapper/ui/SettingsView.java` |
| Tests (stubbed transport, no external call) | `src/test/java/com/sda/mapper/llm/{LlmTranslationServiceTest,LlmPropertiesTest,RestClientChatCompletionsClientExtractTest}.java`, `src/test/java/com/sda/mapper/api/LlmControllerTest.java` |
| Roadmap Phase 18 markers updated | `specs/roadmap.md` |

### Key technical decisions

- R2a hardening scoped to the paths where skill-level `requiresConcept` coverage is meaningful; the hub semantic-match engine matches Offered vs Required capability by `conceptName` identity, making the coverage filter redundant there.
- LLM translation and SWRL migration are documented extension points, not implemented, to respect the AGENTS safety rules (no cloud services / no new infrastructure without approval) and the MVP scope.

---

## Summary

| Phase | Status | Tests | Date |
|-------|--------|-------|------|
| Phase 1: Project Foundation | [x] Completed | 84 | 2026-07-15 |
| Phase 2: CSS-SDA Ontology Model | [x] Completed | 117 | 2026-07-15 |
| Phase 3: Ontology Loading | [x] Completed | 117 | 2026-07-16 |
| Phase 4: Input File Parsers | [x] Completed | 220 | 2026-07-16 |
| Phase 5: Search UI | [x] Completed | 220 | 2026-07-16 |
| Phase 6: Ontology Inference + Dashboard | [x] Completed | 220 | 2026-07-16 |
| Phase 7: Semantic Reasoner Service | [x] Completed | 220 | 2026-07-16 |
| Phase 8: REST API | [x] Completed | 220 | 2026-07-16 |
| Phase 9: Manifest Generation | [x] Completed | 220 | 2026-07-16 |
| Phase 10: Explanation + NotificationCenter | [x] Completed | 220 | 2026-07-16 |
| Phase 11: Evaluation + Monitoring | [x] Completed | 220 | 2026-07-16 |
| Phase 11b: OPC UA Visualization | [x] Completed | 220 | 2026-07-16 |
| Bug Fixes: Multi-Server, NodeSet, Pipeline | [x] Completed | 251 | 2026-07-17 |
| Bug Fixes: Read Attribute, Browse, Grid | [x] Completed | 253 | 2026-07-17 |
| Maintenance: Canonical Ontology Namespace Test Migration | [x] Completed | 327 | 2026-08-03 |
| Maintenance: Requirement Form Mandatory Fields (PipelineView) | [x] Completed | 328 | 2026-08-03 |
| Maintenance: Ontology Visualizer (OntologyView) | [x] Completed | 343 | 2026-08-03 |
| Mapping Hub: Universal Semantic Mapping Hub | [x] Completed | 414 | 2026-08-03 |
| Semantic Infer and Match Alignment | [x] Completed | 430 | 2026-08-04 |
| Session Visibility View | [x] Completed | 451 | 2026-08-05 |
| Ontology Validation Test Suite (Categories A–E) | [x] Completed | 467 | 2026-08-05 |
| Maintenance: v2 Ontology Test Migration + Milo/Guava Test-Classpath Fix | [x] Completed | 467 | 2026-08-05 |
| Maintenance: v2 Ontology Production Migration — Jena-Derived Catalog + UseCaseRequirement Renames | [x] Completed | 468 | 2026-08-06 |
| Maintenance: Instance-Namespace Cleanup — example.org removed from production identity bases | [x] Completed | 468 | 2026-08-06 |
| Maintenance: Ontology-Driven Manual Entry Forms + Derived Subclass Handling | [x] Completed | 479 | 2026-08-06 |
| Phase 12: Documentation | [ ] Not started | — | — |
| Phase 13: EKA UI Polish | [ ] Not started | — | — |
| Phase 14: Margo Sandbox | [ ] Not started | — | — |
| Phase 15: Capability Typing & PlantUnit | [x] Completed | 489 | 2026-08-25 |
| Phase 16: Process Variable Analysis | [x] Completed | 500 | 2026-08-25 |
| Phase 17: Deployment Manager Orchestration (local target) | [x] Completed | 509 | 2026-08-25 |
| Phase 18: Future/Optional (property+R2a done; LLM implemented; SWRL deferred) | [x] Completed | 523 | 2026-08-25 |
| Phase 19: AutomationML (CAEX) Ingestion | [x] Completed | 496 | 2026-08-25 |
| Phase 20: Generic Requirement Matching & User-Defined Requirements | [x] Completed | 536 | 2026-08-25 |

---

## Phase 20: Generic Requirement Matching & User-Defined Requirements

**Status:** [x] Completed
**Branch:** `feature/2026-08-25-remaining-phases`
**Completed:** 2026-08-25
**Tests:** 536 passing (0 failures)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS (536 run, 0 failures)
- `GenericRequirementMatchingTest` — seeded REQ-001..004 + user-defined `ProcessVariableAnalysis` match; `PROFINET`-only requirement correctly produces no match.
- `RequirementViewTest` — UI renders, Create/Match flows work.
- `SeedServicesLoadTest` — runtime catalog (seed-services.ttl) loaded; REQ-001..004 match expected services.

### What was built

| Component | Files |
|---|---|
| Generic matcher | `src/main/resources/queries/match-generic.rq` (replaces 4 per-requirement queries) |
| Reasoner routing | `SemanticReasonerService.java` (generic query for any requirement; inline fallback for capability-less requirements) |
| Persist API + DTO | `RequirementService.java`, `MatchRequirementRequest.java` (+requirementName, requirementDescription, capabilityType) |
| UI | `RequirementView.java` (`/requirement`), `MainLayout.java` nav link |
| Tests | `GenericRequirementMatchingTest`, `RequirementViewTest`, `SeedServicesLoadTest` |

### Key technical decisions

- Single parameterized query (`BIND(:__REQ_ID__ AS ?requirement)`) reads all requirement
  constraints as OPTIONAL filters, so no per-requirement query file is needed.
- Requirement ID is never auto-generated (user-supplied); constraints stored as typed
  SDA literals, not expression strings.
- Capability-less requirements fall back to constraint-only `matchInline` (legacy behaviour).

---

## Phase 21: End-to-End Pipeline Observability

**Status:** [x] Completed
**Branch:** `feature/2026-08-26-phase21-pipeline-observability`
**Completed:** 2026-08-26
**Tests:** 556 passing (0 failures) — up from 536 baseline

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 556 tests, 0 failures, 0 errors
- Spec cycle complete: `spec-review-report.md` (5 findings closed), `spec-validation-report.md` (PASS),
  `review-report.md` (4 findings fixed + re-review closed), `validation-report.md` (PASS)
- Live Docker walkthrough on real PostgreSQL: Flyway V6 applied; authenticated match +
  E2E pipeline (`POST /api/v1/pipeline/execute`) audited end-to-end under one run id;
  `/actuator/prometheus` scraped showing `sda_pipeline_runs_total`,
  `sda_pipeline_match_result_total`, `sda_sparql_query_seconds`
- All Phase 21 acceptance criteria marked `[x]` in `specs/roadmap.md`

### What was built

| Component | Files |
|---|---|
| Tracing/correlation | `observability/PipelineTracingContext.java` (root run observation + OTel baggage + MDC), `PipelineStageObserver.java` (per-stage span/timer/audit) |
| SPARQL visibility | `observability/TracedSparqlExecutor.java`; wired into `SemanticMatchService.java`, `SemanticReasonerService.java` |
| Audit log | Flyway `V6__pipeline_audit_log.sql`, `observability/PipelineEvent.java`, `PipelineAuditRepository.java`, `PipelineAuditPublisher.java` (failure-safe, live listeners) |
| Audit REST API | `observability/PipelineAuditController.java` (`GET /api/v1/pipeline-audits`, limit 200/1000, ProblemDetail) |
| Dimensional tags | `observability/MatchDimensionTagger.java`; `match.dimension.*` attributes on MATCH spans |
| Metrics/dashboard | `observability/PipelineMetrics.java` (`sda.pipeline.*`, `sda.sparql.*`), `docs/grafana/sda-mapper-pipeline-dashboard.json` (+ parse test) |
| Live UI console | `hub/ui/StageTimeline.java`, `PipelineTracePanel.java`, `PipelineConsolePanel.java`; embedded in `MappingHubView.java`; push via new `ui/SdaAppShell.java` |
| Wiring | `MappingHubService.java`, `EndToEndPipelineService.java`, `SemanticReasonerService.java`, `SemanticMatchService.java`, `ObservabilityConfig.java` |
| Tests | `TracedSparqlExecutorTest`, `PipelineAuditPublisherTest`, `PipelineAuditControllerTest`, `PipelineObservabilityTraceShapeTest`, `MatchDimensionTaggerTest`, `MatchDimensionSpanTagTest`, `GrafanaDashboardJsonParseTest`, `MappingHubViewObservabilityTest` |

### Key technical decisions

- Observation API used directly instead of `@Observed`: no AOP dependency exists for
  `ObservedAspect` (would silently no-op); manual observations produce identical
  traces/metrics. Zero new dependencies — the approved `micrometer-tracing-test` proved
  unnecessary because Observation handlers capture span topology natively.
- Push enabled on the app shell (`SdaAppShell`) rather than the view: Vaadin 25 rejects
  `@Push` on view classes at servlet init.
- `pipeline_audit_log` persisted via record + JdbcTemplate (codebase convention), not Hibernate.
- Audit write failures are logged at ERROR and swallowed so observability can never abort matching.
- Each requirement match inside one E2E execution is its own correlated run (documented semantics).
- **Defect caught by live validation (V1):** reasoning/E2E paths passed a null run id into the
  stage observer, violating `run_id NOT NULL`; silently dropped audit rows while matching kept
  working. Fixed by generating real run UUIDs in both paths + regression test
  (`shouldPersistReasonerMatchTransitions`) running the reasoner against a real database.

---

## Feature: ABox TTL catalogue upload (Pump Asset Health Monitoring use case)

**Status:** [x] Completed
**Completed:** 2026-08-26
**Tests:** 598 passing (0 failures) — MappingHubAboxUploadTest (2), AboxTtlParserTest (2) added; full suite green

### What was built

Enables the Mapping Hub pipeline to ingest a complete SDA ABox package (requirement + service
catalogue + asset description + edge-node topology) so a use case such as
docs/usecases/Pump Asset Health Monitoring/ can be implemented end-to-end through upload.

- New AssetFileFormat.ABOX_TTL ("ABox TTL", Turtle) for raw SDA ABox files
  (catalogue_services.ttl, catalogue_assets.ttl, dge_nodes.ttl).
- HubFileTypeDetector now falls back .ttl/.rdf to ABOX_TTL (requirement files are still
  detected as REQUIREMENT_TTL by content sniff); added an ABox content sniff.
- AboxTtlParser (Spring bean) registered so session rebuilds recognise the format; it returns an
  empty canonical model because ABox triples are merged into the shared catalogue model.
- MappingHubService merges uploaded ABox TTL triples directly into the shared reasoning model via
  SemanticReasonerService.getOntologyModel() (the same model the matcher queries), so an uploaded
  catalogue_services.ttl **updates the catalogue service/asset/edge list**. Removal of the file and
  eset() reverse the merge.
- match-generic.rq: made dfs:label on connector and deployment context OPTIONAL (they are
  presentation-only) so example packages that omit them still match — strictly more permissive.
- V8__allow_abox_ttl_upload_format.sql: extended the uploaded-file format check constraint.
- UI: UploadDropzone accepts .ttl/.rdf for ABOX_TTL; MappingHubView.formatClass covers it.

### Validation evidence

- MappingHubAboxUploadTest.shouldImplementPumpHealthUseCase uploads the 4 use-case files and asserts
  the NE107 Service_Diagnostics_v1 (OCap_AssetConditionMonitoring / Skill_NE107_Diag) is matched
  for REQ-03.
- shouldDetectAboxTtl confirms catalogue_services.ttl / dge_nodes.ttl detect as ABOX_TTL.
- Full suite: 598 passing (was 594).

### Files created / modified

| Component | Files |
|-----------|-------|
| Format + detection | hub/model/AssetFileFormat.java, hub/parser/HubFileTypeDetector.java, hub/parser/AboxTtlParser.java (new), hub/ui/UploadDropzone.java, hub/ui/MappingHubView.java |
| Merge logic | hub/service/MappingHubService.java, easoning/SemanticReasonerService.java (getOntologyModel) |
| Matching | esources/queries/match-generic.rq |
| Persistence | esources/db/migration/V8__allow_abox_ttl_upload_format.sql |
| Docs | docs/USER-TEST-MANUAL.md (4.5 ABox TTL use-case) |
| Tests | MappingHubAboxUploadTest.java (new), AboxTtlParserTest.java (new), 	est/resources/abox/pump-health/* (copied fixtures) |

### Key technical decisions

- ABox TTL is merged into the **shared** catalogue model (not the per-session canonical model) so the
  existing matchManualRequirement path (which queries the shared model) sees the uploaded supply and
  matches it. This realises the previously-identified "upload updates the catalogue" capability.
- The requirement (eq_pump_health.ttl) continues through the existing REQUIREMENT_TTL path and
  becomes an active requirement; the match is produced by the catalogue matcher with an
  AssetConditionMonitoring capability join.
- Reused SemanticReasonerService.getOntologyModel() to avoid adding a new constructor parameter to
   MappingHubService (kept test churn minimal).

---

## Maintenance: ABox → Canonical Model Projection (coverage/manifest fix) + device-descriptions cleanup

**Status:** [x] Completed
**Branch:** `main`
**Completed:** 2026-08-26
**Tests:** 600 passing (0 failures, 0 errors, 0 skipped) — up from 598; +2 new ABox parser/hub tests, 1 converter test assertion corrected

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 600 tests, 0 failures, 0 errors
- Before fix: uploading the Pump package produced a matched `Service_Diagnostics_v1` in the
  deployment manifest, but coverage reported `4.2%` with `Service`/`Asset`/`Capability`/`Skill`
  etc. `MISSING` (ABox individuals were merged into the Jena model for the matcher but never into
  the session `CanonicalAssetModel` that coverage/manifest `entities`/lift/inference read).
- After fix: `MappingHubAboxUploadTest.shouldReflectAboxEntitiesInCoverage` asserts coverage ≥ 75%
  and `Service`/`Asset`/`Capability`/`Skill` = `PRESENT` after uploading the 4 Pump files.
- `AboxTtlParserTest` (3 tests) confirms subclass→canonical resolution (`OfferedCapability`→
  `Capability`, `AnalyticsService`/`CPCService`→`Service`, `MTPSkill`/`AASSkill`/`ControlLogicSkill`/
  `OrganisationalSkill`→`Skill`, `EdgeContext`→`DeploymentContext`, `OTZone`→`NetworkZone`) and
  source provenance.
- `ManualEntityConverterTest.shouldRejectUnknownClass` updated: `InformationModel` is now a
  supported canonical class, so the "no mapping" case uses a genuine non-canonical name.
- **Live UI validation (2026-08-26):** uploaded the Pump package → `coveragePercent: 77.1`
  (was 4.2% before the fix), L2/L3/L4 all `PRESENT`, `deploymentManifest` matched
  `Service_Diagnostics_v1` for `RCAP-001`. Remaining `blockingGaps` are L1
  `UseCaseRequirementParameter` / `UseCaseRequirementConstraint` / `VerificationCriterion`,
  which the package does not supply (no parameters/constraints/verification criteria in
  `req_pump_health.ttl`); `ServiceInstance` is `PARTIAL` (display name derived from `serviceId`,
  canonical `name` empty in source TTL). Both are expected data gaps, not defects.

### What was built / changed

| Component | Files |
|---|---|
| `ManualEntity` carries real source file name (5-arg ctor; 4-arg defaults to `"manual-entry"`); `sourceFileName()` returns it | `src/main/java/com/sda/mapper/hub/model/ManualEntity.java` |
| `ManualEntityConverter.toTyped` extended to all 24 canonical classes; uses `manual.sourceFileName().orElse(MANUAL_SOURCE)` | `src/main/java/com/sda/mapper/hub/model/ManualEntityConverter.java` |
| `AboxTtlParser` rewritten to `project(Model, fileName)` — resolves each ABox individual's subclass to its canonical parent via `OntologyDataService` and returns the projected `CanonicalAssetModel`; ctor now requires `OntologyDataService` | `src/main/java/com/sda/mapper/hub/parser/AboxTtlParser.java` |
| `MappingHubService.parseAndMergeAbox` returns the projected model (still merges triples into the shared Jena model for the matcher) | `src/main/java/com/sda/mapper/hub/service/MappingHubService.java` |
| Tests | `src/test/java/com/sda/mapper/hub/parser/AboxTtlParserTest.java` (3), `src/test/java/com/sda/mapper/hub/service/MappingHubAboxUploadTest.java` (3), `src/test/java/com/sda/mapper/hub/model/ManualEntityConverterTest.java` (assertion fix) |

### device-descriptions resource cleanup (no runtime references)

- Removed `src/main/resources/device-descriptions/` (no production code referenced it; assets are
  uploaded at runtime, not shipped).
- Relocated reference-format samples to `src/test/resources/device-descriptions/reference-formats/`
  (classpath, used by tests) and `docs/usecases/reference-formats/` (canonical UAT copies).
- Relocated REQ-001 fixtures to `src/test/resources/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/`
  so the 38 classpath test references stay green; canonical copies remain in `docs/usecases/`.
- Fixed stale JavaDoc/spec/path references: `AasModelParser`, `MtpModelParser`, `OpcUaModelParser`
  (point to `docs/usecases/...`), `docs/USER-TEST-MANUAL.md`, `specs/roadmap.md`,
  `specs/2026-08-25-process-variable-analysis/requirements.md`, `tasks.md`.

### Key technical decisions

- ABox individuals are projected as `ManualEntity` → `ManualEntityConverter.toTyped` (the same
  path used by manual entry and the persisted-entity restorer) → typed canonical records, so
  `CanonicalAssetModel.addEntity` auto-converts them; this avoids duplicating 24 typed constructors
  and keeps semantic mapping in the converter/ontology rather than Java rules.
- Subclass→canonical resolution uses `OntologyDataService.getClassByName` + `parent()` walk; all
  ABox subclasses are declared in the TBox, so the projection is fully ontology-driven.
- The shared Jena merge for the SPARQL matcher is preserved unchanged, so the existing
  `deploymentManifest` matches continue to work alongside the now-correct coverage/entities.
