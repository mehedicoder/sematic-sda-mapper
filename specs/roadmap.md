# Roadmap: Semantic SDA Mapper

## Roadmap Principle

The project will be implemented as a sequence of small vertical slices. Each phase should produce something executable, testable, and reviewable.

The primary goal is to prove this semantic mapping path:

```text
Input Files (MTP AML, OPC UA Server Datasheet, AAS)
→ Parse
→ Instantiate (CSS-SDA ontology ABox triples)
→ Lift (MTP Service → css:Skill)
→ Infer (OWL reasoner derives OfferedCapability)
→ Match (SPARQL multi-dimensional query)
→ Docker Compose / Kubernetes Manifest
```

The CSS-SDA model: SDA-Service := ⟨ Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext ⟩

**Input sources** (vendor-supplied device description files):
- **MTP AML files** (VDI/VDE/NAMUR 2658) — modular process equipment with Service/Parameter/Communication blocks
- **OPC UA Server Datasheets** (IDTA 02009-1-0) — AAS submodel with EndpointDescriptions, Configuration, Identification
- **AAS submodels** — device metadata including SIL, lifecycle, engineering units (UCUM), semantic IDs (IEC CDD IRDI)

**Five-step mapping procedure** — applied to every input file:
1. **Parse** — read and validate the input file against its schema
2. **Instantiate** — create CommunicationBinding, DataConnector, DeploymentContext ABox triples
3. **Lift** — transform service descriptions into css:Skill + css:SkillParameter instances with UCUM units and IRDI identifiers
4. **Infer** — OWL reasoner derives sda:OfferedCapability from SkillParameter semantics (key novel step)
5. **Match** — SPARQL query compares OfferedCapability against RequiredCapability across all CSS-SDA dimensions simultaneously

Apache Jena and SPARQL are part of the semantic reasoning layer. Dagger is not a replacement for them. Dagger is used as an automation layer for build, test, packaging, validation, and deployment workflow.

```text
Apache Jena + SPARQL = semantic matching core
Dagger = automation and deployment pipeline
Docker Compose = generated deployment output
```

---

## Phase 1: Project Foundation

**Goal**

Create a runnable Java 25 and Spring Boot 4.0.x project with basic structure, testing, Vaadin 25 UI foundation, local development setup, and automation pipeline foundation.

**Tasks**

* [ ] Create Maven-based Spring Boot 4.0.x project
  * [ ] Add Spring Web, Validation, Actuator dependencies
  * [ ] Add Micrometer Prometheus registry for metrics export
  * [ ] Add Micrometer Tracing with OpenTelemetry bridge for distributed tracing
  * [ ] Add Apache Jena dependency
  * [ ] Add SnakeYAML dependency
  * [ ] Add JUnit 5 and AssertJ test support
  * [ ] Add Vaadin 25 Spring Boot starter and Lumo theme dependency
  * [ ] Add Spring Security dependency
  * [ ] Add Spring Data JPA and PostgreSQL dependencies
  * [ ] Add Flyway database migration dependencies
* [ ] Create base Java package structure under `com.sda.mapper`
* [ ] Add health endpoint through Spring Actuator
* [ ] Configure `application.yml` for observability (metrics, tracing, log pattern with trace/span IDs)
* [ ] Add custom health indicators for ontology readiness, SPARQL query loadability, OPC UA simulation reachability
* [ ] Configure structured logging with Logback for JSON output in production
* [ ] Add Vaadin `MainLayout` with AppLayout structure:
  * [ ] Header with CSS-SDA logo, title, theme switch, user profile
  * [ ] Collapsible navigation drawer following CSS-SDA hierarchy (Intent → Use Cases → Requirements → Capability → Capabilities → Skills → Service Layer → Services → Communication Bindings → Data Connectors → Deployment → Assets → Digital Twins → Monitoring → Administration)
  * [ ] Main workspace with responsive routing
* [ ] Add `ThemeConfig` with Lumo light/dark mode toggle and persisted preference
* [ ] Add `SecurityConfig` with form-login and role-based access skeleton
* [ ] Add `UIConfig` with Vaadin routing configuration
* [ ] Implement responsive layout:
  * [ ] Desktop: Drawer always visible
  * [ ] Tablet: Drawer collapses automatically
  * [ ] Mobile: Drawer becomes overlay
* [ ] Add Dockerfile
* [ ] Add docker-compose.yml
* [ ] Add README with local run instructions
* [ ] Add OPC UA server service to `docker-compose.yml`
* [ ] Configure OPC UA server endpoint and security settings
* [ ] Add OPC UA server health check
* [ ] Add network configuration for service-to-server communication
* [ ] Add integration test that verifies OPC UA server availability
* [ ] Add Vaadin UI test for MainLayout rendering and navigation drawer interaction
* [ ] Add Flyway migration directory `src/main/resources/db/migration/` with `V1__initial_schema.sql`
* [ ] Configure `spring.flyway.enabled=true` and baseline settings in `application.yml`
* [ ] Add Node-RED service to `docker-compose.yml`
 * [ ] Install OpcUa-Server node in Node-RED
 * [ ] Mount `docs/opc-us.json` as a volume in the Node-RED container so the flow loads automatically on start
 * [ ] Use `docs/opc-us.json` flow to simulate OPC UA server
* [ ] Create `src/main/resources/config/options.json` with configurable dropdown values (see `specs/config-options.md`)
* [ ] Create `OptionsConfigService` to load options from classpath
* [ ] Create `Option` record (`value`, `label`)
* [ ] Add `GET /api/v1/options/{fieldName}` endpoint to serve dropdown options
* [ ] Add unit test: options load correctly from JSON
* [ ] Add unit test: missing `options.json` fails fast on startup
* [ ] Add unit test: malformed JSON throws clear error
* [ ] Add unit test: `GET /api/v1/options/{fieldName}` returns correct list
* [ ] Add unit test: `GET /api/v1/options/nonexistent` returns 404

**Acceptance Criteria**

* [ ] `mvn clean test` passes
* [ ] Application starts locally
* [ ] `/actuator/health` returns `UP`
* [ ] Vaadin UI accessible at `http://localhost:8080/` with MainLayout and navigation drawer
* [ ] Light/dark mode toggle works
* [ ] Navigation drawer follows CSS-SDA hierarchy and is collapsible
* [ ] UI is responsive on desktop, tablet, and mobile viewports
* [ ] Vaadin UI test for MainLayout rendering and drawer navigation passes
* [ ] Docker image can be built
* [ ] Application can be started through Docker Compose
* [ ] OPC UA server starts via `docker compose up`
* [ ] OPC UA server is reachable from the application container
* [ ] OPC UA server health endpoint is available
* [ ] `/actuator/prometheus` returns metrics including JVM, request, and custom `sda.*` metrics
* [ ] Custom health indicators (ontology, SPARQL, OPC UA) appear in `/actuator/health`
* [ ] Log output includes trace and span correlation IDs `[traceId,spanId]`
* [ ] Flyway migrations run automatically on application startup
* [ ] `flyway_schema_history` table is created in PostgreSQL after first startup
* [ ] `options.json` loads successfully on application startup
* [ ] `GET /api/v1/options/protocol` returns the list of configured protocols
* [ ] `GET /api/v1/options/nonexistent` returns 404

---

## Phase 2: CSS-SDA Ontology Model

**Goal**

Create an RDF/Turtle ontology based on the CSS-SDA formal model: SDA-Service := ⟨ Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext ⟩. The ontology serves as a unifying abstraction above all input file formats (MTP AML, OPC UA Server Datasheet, AAS).

**Tasks**

* [ ] Create `src/main/resources/ontology/css-sda-small.ttl`
* [ ] Define core CSS-SDA classes from the formal model:
  * [ ] `sda:UseCase`, `sda:Requirement`
  * [ ] `css:Capability` (with subclasses `sda:OfferedCapability`, `sda:RequiredCapability`)
  * [ ] `css:Skill`, `css:SkillParameter`, `css:SkillInterface`
  * [ ] `sda:Service`
  * [ ] `sda:CommunicationBinding` (protocol, endpointUri, securityMode, securityPolicy, securityLevel, transportProfile, discoveryUrl, networkZone)
  * [ ] `sda:DataConnector` (informationModel, nodeSetFile, dataFormat, semanticID/IRDI)
  * [ ] `sda:DeploymentContext` (safetyIntegrityLevel, determinismClass, maxCycleTime_ms, requiredRuntime, networkZone)
* [ ] Define physical asset classes: `sda:MTPModule`, `sda:FieldDevice`, `sda:BACnetDevice`, `sda:EnergyMeter`
* [ ] Define object properties: `mapsToCapability`, `realisedBy`, `isImplementedBy`, `usesBinding`, `usesConnector`, `isDeployedBy`, `hasBinding`, `hasConnector`, `hasContext`, `offersUseOf`, `hasParameter`, `locatedIn`, `belongsToSite`, `hasAAS`, `hasSemanticID`
* [ ] Define data properties: protocol, accessMode, recall, latency, engineeringUnit (UCUM), semanticID (IEC CDD IRDI), nominalRange_min, nominalRange_max, accuracy, image, onPremise, endpointUri, securityMode, securityPolicy, securityLevel, transportProfile, discoveryUrl, networkZone, safetyIntegrityLevel, determinismClass, maxCycleTime_ms, requiredRuntime, softwareVersion, productUri
* [ ] Add example instances from PDF:
  * [ ] Fermentation skid `Skid-A1` (MTPModule, SIL-2, PROFINET, OPC UA)
  * [ ] AHU controller `AHU-01` (FieldDevice, BACnet, OPC UA gateway, M+O)
  * [ ] Energy meter `EM-01` (EnergyMeter, native OPC UA, non-critical)
* [ ] Add `REQ_001` requirement and `FlowmeterDiagnosticsService`
* [ ] Add negative candidate services for testing

* [ ] Create `ExplorerView` with split layout:
  * [ ] Left: Static ontology tree component showing CSS-SDA class hierarchy (Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext, physical asset types)
  * [ ] Right: Entity details with Overview tab
* [ ] Create `OntologyTree` component with expandable/collapsible nodes
* [ ] Add Vaadin UI test for ExplorerView rendering and tree interaction

**Acceptance Criteria**

* [ ] Ontology file is valid Turtle
* [ ] Ontology contains all CSS-SDA dimensions
* [ ] Ontology supports MTPModule and FieldDevice physical asset types
* [ ] Ontology contains CommunicationBinding with full security properties (mode, policy, level, discovery)
* [ ] Ontology contains DeploymentContext with SIL, determinism class, cycle time, and runtime
* [ ] Ontology contains OfferedCapability and RequiredCapability for match inference
* [ ] Ontology can be loaded by Apache Jena in a test
* [ ] Ontology contains at least three example devices across two use cases
* [ ] ExplorerView shows CSS-SDA class hierarchy in tree sidebar
* [ ] Tree nodes are expandable and collapsible
* [ ] Selecting a tree node displays entity details in the right panel
* [ ] ExplorerView UI test validates tree rendering and node selection

---

## Phase 3: Ontology Loading with Apache Jena

**Goal**

Implement Java code that loads the ontology model into Apache Jena.

**Tasks**

* [ ] Create `OntologyModelLoader`
* [ ] Load ontology file from classpath
* [ ] Return Apache Jena `Model`
* [ ] Add clear error when ontology file is missing
* [ ] Add clear error when ontology file is invalid
* [ ] Add unit test for successful ontology loading
* [ ] Add unit test for missing ontology file
* [ ] Add unit test for invalid ontology content
* [ ] Wire `ExplorerView` to `OntologyModelLoader` — tree loads dynamically from Jena model
* [ ] Add entity detail tabs: Overview, Properties, Relationships
* [ ] Add Vaadin UI test for ExplorerView dynamic tree loading and tab switching

**Acceptance Criteria**

* [ ] Ontology loads successfully during application startup or first use
* [ ] Invalid ontology input fails with a clear exception
* [ ] Tests cover success and failure cases
* [ ] ExplorerView ontology tree populates dynamically from loaded model
* [ ] Selecting an entity shows Properties and Relationships tabs
* [ ] Missing ontology file shows user-friendly error in ExplorerView
* [ ] ExplorerView UI test validates dynamic loading and tab interaction

---

## Phase 4: Input File Parsers — MTP AML and OPC UA Server Datasheet

**Goal**

Implement parsers that read vendor-supplied device description files (MTP AML, OPC UA Server Datasheet) and generate CSS-SDA ontology instances (ABox triples) as specified in the five-step procedure (Steps 1–3: Parse, Instantiate, Lift).

**Tasks**

* [ ] Create `MTPAmlParser` for VDI/VDE/NAMUR 2658 AML archives:
  * [ ] Parse `<Service>` blocks into `css:Skill` + `css:SkillParameter` instances with UCUM units and IEC CDD IRDI identifiers
  * [ ] Parse `<Communication>/<OpcUaServer>` into `sda:CommunicationBinding` triples (endpoint, security mode, policy, NodeSet)
  * [ ] Parse `<DataAssembly>/<OpcUaNode>` into `sda:DataConnector` triples
  * [ ] Handle missing or invalid AML structure with clear errors
* [ ] Create `UaServerDatasheetParser` for IDTA 02009-1-0 AAS submodels:
  * [ ] Parse `Configuration/NodeSets` into `sda:DataConnector`
  * [ ] Parse `EndpointDescriptions` into `sda:CommunicationBinding` (endpointUri, securityMode, securityPolicy, securityLevel, transportProfile, discoveryUrl)
  * [ ] Parse `Identification` into device metadata (productUri, softwareVersion)
  * [ ] Handle missing or invalid datasheet structure with clear errors
* [ ] Create `ParserFactory` to select parser based on input file type
* [ ] Add test MTP AML example file under `src/test/resources/examples/`
* [ ] Add test OPC UA Server Datasheet example file under `src/test/resources/examples/`
* [ ] Add unit tests for MTP AML parsing (valid, missing elements, invalid XML)
* [ ] Add unit tests for OPC UA Server Datasheet parsing (valid, missing elements, invalid JSON)
* [ ] Add integration test that loads parsed triples into Apache Jena Model
* [ ] Create `UploadView` with drag-and-drop upload panel:
  * [ ] Support OPC UA NodeSet, AAS, MTP, AutomationML, RDF, OWL, JSON-LD, YAML
  * [ ] Upload progress bar and validation results display
  * [ ] Preview parsed triples after successful upload
* [ ] Add Vaadin UI test for UploadView file selection, validation, and progress display

**Acceptance Criteria**

* [ ] MTP parser generates valid CSS-SDA triples from a real AML source
* [ ] OPC UA Datasheet parser generates valid CSS-SDA triples from a real datasheet source
* [ ] Parsers produce `CommunicationBinding` triples with endpoint, security mode, and security policy
* [ ] MTP parser produces `css:Skill` + `css:SkillParameter` with UCUM units and IRDI
* [ ] Datasheet parser produces `CommunicationBinding` with securityLevel, transportProfile, discoveryUrl
* [ ] Both parsers fail with clear errors on invalid input
* [ ] Parsed triples can be loaded into the same Apache Jena Model as the ontology schema
* [ ] UploadView accepts valid file types and rejects invalid types with clear messages
* [ ] UploadView shows progress bar during upload
* [ ] UploadView previews parsed triples on success
* [ ] UploadView UI test validates file type acceptance, rejection, and progress display

---

## Phase 5: SPARQL Matching Query

**Goal**

Create SPARQL queries that select a service satisfying a requirement by matching across all CSS-SDA dimensions simultaneously (Capability, CommunicationBinding, DeploymentContext). Follow the PDF's multi-dimensional matching pattern.

**Tasks**

* [ ] Create `src/main/resources/queries/match-service.rq`
* [ ] Match requirement to capability via `sda:functionClass`, `sda:engineeringUnit`, `sda:nominalRange`, `sda:safetyIntegrityLevel`
* [ ] Match capability to skill (css:Skill via css:offersUseOf)
* [ ] Match skill to service
* [ ] Match required protocol via `sda:CommunicationBinding/sda:protocol`
* [ ] Match required access mode
* [ ] Match data connector (informationModel, nodeSetFile)
* [ ] Match deployment context (networkZone, safetyIntegrityLevel, determinismClass)
* [ ] Filter by minimum recall
* [ ] Filter by maximum latency
* [ ] Order candidates by highest recall
* [ ] Limit result to one best service
* [ ] Add query test with Apache Jena
* [ ] Create global `SearchBar` component:
  * [ ] AI-style semantic search with autocomplete, fuzzy search, exact search, recent searches
  * [ ] Filters: Requirement, Capability, Skill, Service, Deployment, Asset
  * [ ] Add `SearchBar` to `MainLayout` header
* [ ] Create `SearchView` with results Grid:
  * [ ] Columns: Name, Type, Description, Ontology Path, Similarity Score, Linked Services, Actions
  * [ ] Sorting, filtering, pagination, column chooser, CSV/Excel export, context menu
* [ ] Add Vaadin UI test for SearchBar autocomplete and SearchView result grid interaction

**Acceptance Criteria**

* [ ] Query returns `FlowmeterDiagnosticsService` for `REQ_001`
* [ ] Query excludes service with wrong capability (functionClass mismatch)
* [ ] Query excludes service with wrong protocol
* [ ] Query excludes service with excessive latency
* [ ] Query excludes service with incompatible deployment context
* [ ] Query evaluates CommunicationBinding, DataConnector, and DeploymentContext simultaneously
* [ ] SearchBar shows autocomplete suggestions as user types
* [ ] SearchView displays results Grid with correct columns
* [ ] Filters scope search to selected entity type
* [ ] Results support sorting and pagination
* [ ] SearchBar and SearchView UI tests validate autocomplete and grid interaction

---

## Phase 6: Ontology Inference — OWL Reasoner

**Goal**

Implement the OWL reasoner step (Step 4: Infer from the five-step procedure) that derives `sda:OfferedCapability` instances from `css:SkillParameter` semantics. This is the key architectural novelty — the capability is inferred, not parsed.

**Tasks**

* [ ] Create `OntologyInferenceService`
* [ ] Configure OWL reasoner (e.g., Apache Jena OWL_MEM_MICRO_RULES_INF or EL reasoner)
* [ ] Define OWL axiom A4 for capability derivation: SkillParameter (engineeringUnit, nominalRange, functionClass) → OfferedCapability
* [ ] Register `OfferedCapability` and `RequiredCapability` in the ontology
* [ ] Run inference after parser-generated triples are loaded
* [ ] Ensure inference does not overwrite existing explicit `OfferedCapability` triples
* [ ] Add unit test: parser-generated triples produce expected `OfferedCapability` after inference
* [ ] Add unit test: changing SkillParameter semantics changes inferred capability
* [ ] Create `DashboardView` with animated cards:
  * [ ] Registered Services, Capabilities, Skills, Semantic Mappings
  * [ ] Each card with icon, title, value, trend, status color; clickable to navigate
* [ ] Create `DashboardCard` component
* [ ] Wire DashboardView to `OntologyInferenceService` for live counts
* [ ] Add Vaadin UI test for DashboardView card rendering and navigation

**Acceptance Criteria**

* [ ] Inferred `OfferedCapability` instance has correct `functionClass`, `engineeringUnit`, and `nominalRange`
* [ ] Inference preserves existing explicit capability triples
* [ ] Changing SkillParameter input changes inferred capability output
* [ ] Inference completes within acceptable time for the MVP dataset
* [ ] Reasoner can be enabled/disabled for testing without inference
* [ ] DashboardView loads and displays animated cards with correct backend data
* [ ] Cards show live counts from inference results
* [ ] Clicking a card navigates to the corresponding view
* [ ] DashboardView UI test validates card rendering and click navigation

---

## Phase 7: Semantic Reasoner Service

**Goal**

Implement the application service that runs the SPARQL matching query and returns a Java result object.

**Tasks**

* [ ] Create `SemanticReasonerService`
* [ ] Create `SparqlQueryLoader`
* [ ] Create `ServiceMatchResult`
* [ ] Execute SPARQL query against ontology model
* [ ] Support matching by `requirementId`
* [ ] Extract selected service name
* [ ] Extract container image
* [ ] Extract capability
* [ ] Extract protocol
* [ ] Extract access mode
* [ ] Extract connector
* [ ] Extract deployment context
* [ ] Extract recall and latency values
* [ ] Return no-match result when query has no result
* [ ] Add unit tests for match and no-match scenarios
* [ ] Wire DashboardView cards to `SemanticReasonerService` — live match counts and statistics
* [ ] Create `StatisticsPanel` component for match/success/failure rates
* [ ] Add Vaadin UI test for DashboardView statistics display

**Acceptance Criteria**

* [ ] Reasoner returns a complete result for `REQ_001`
* [ ] Reasoner returns no-match for unsatisfied requirements
* [ ] Reasoner does not hardcode service selection in Java
* [ ] Matching behavior is controlled by ontology data and SPARQL rules
* [ ] Tests prove that changing ontology data changes the selected service without Java code changes
* [ ] DashboardView shows live match statistics from reasoner
* [ ] DashboardView statistics UI test validates correct count display

---

## Phase 8: REST API

**Goal**

Expose requirement matching through a REST endpoint.

**Tasks**

* [ ] Create `RequirementMatchingController`
* [ ] Create `MatchRequirementRequest` supporting two modes:
  * [ ] Mode A: `{ "requirementId": "REQ_001" }` — lookup existing requirement from ontology
  * [ ] Mode B: inline fields (`functionClass`, `protocol`, `accessMode`, `engineeringUnit`, `safetyIntegrityLevel`, `networkZone`, `determinismClass`, `minRecall`, `maxLatencyMs`, `nominalRange_min`, `nominalRange_max`) — match on-the-fly without persisting a requirement
* [ ] Create `MatchRequirementResponse`
* [ ] Add request validation (requirementId OR inline fields required, not both)
* [ ] Add endpoint `POST /api/v1/requirements/match`
* [ ] Add endpoint `POST /api/v1/requirements` to persist a new requirement to the ontology
* [ ] Return matched service details
* [ ] Return no-match response when no service satisfies the requirement
* [ ] Add global exception handling
* [ ] Add integration tests with MockMvc or WebTestClient (both modes)
* [ ] Create `RegistrationWizardView` (multi-step):
  * [ ] Step 1: General Information
  * [ ] Step 2: Communication
  * [ ] Step 3: Capabilities (dropdowns populated from `GET /api/v1/options/{field}`)
  * [ ] Step 4: Skills
  * [ ] Step 5: Deployment
  * [ ] Step 6: Semantic Mapping
  * [ ] Step 7: Review
  * [ ] Step 8: Register
  * [ ] Navigation: Previous, Next, Save Draft, Cancel
  * [ ] On Register, call REST API to persist new service in ontology model
* [ ] Add Vaadin UI test for RegistrationWizardView step navigation and form interaction
* [ ] Create `RequirementFormView` for creating and matching requirements:
  * [ ] Dropdown: functionClass (from `GET /api/v1/options/functionClass`)
  * [ ] Dropdown: protocol (from `GET /api/v1/options/protocol`)
  * [ ] Dropdown: accessMode (from `GET /api/v1/options/accessMode`)
  * [ ] Dropdown: engineeringUnit (from `GET /api/v1/options/engineeringUnit`)
  * [ ] Dropdown: safetyIntegrityLevel (from `GET /api/v1/options/safetyIntegrityLevel`)
  * [ ] Dropdown: networkZone (from `GET /api/v1/options/networkZone`)
  * [ ] Dropdown: determinismClass (from `GET /api/v1/options/determinismClass`)
  * [ ] Numeric input: nominalRange_min
  * [ ] Numeric input: nominalRange_max
  * [ ] Numeric input: minRecall (0.0–1.0)
  * [ ] Numeric input: maxLatencyMs
  * [ ] Button: "Match" — calls `POST /api/v1/requirements/match` with inline fields
  * [ ] Button: "Save Requirement" — calls `POST /api/v1/requirements` to persist, then match by ID
  * [ ] Results panel: shows matched service details or no-match message
  * [ ] All dropdown options loaded from configurable `options.json` via `GET /api/v1/options/{field}`
* [ ] Add Vaadin UI test for RequirementFormView dropdown loading, form fill, and match result display

**Acceptance Criteria**

* [ ] API accepts a requirement ID (Mode A)
* [ ] API accepts inline requirement fields (Mode B)
* [ ] API returns selected service for valid matching requirement
* [ ] API returns no-match response for unsatisfied requirement
* [ ] API returns validation error when neither requirementId nor inline fields are provided
* [ ] API returns validation error when both requirementId and inline fields are provided
* [ ] `POST /api/v1/requirements` persists a new requirement to the ontology
* [ ] Integration tests pass for both modes
* [ ] RegistrationWizardView navigates through all 8 steps without errors
* [ ] RegistrationWizardView calls REST API on final Register step
* [ ] Draft saving preserves form state across sessions
* [ ] RegistrationWizardView UI test validates all steps and final registration
* [ ] RequirementFormView loads all dropdown options from `GET /api/v1/options/{field}`
* [ ] RequirementFormView dropdowns are populated from configurable `options.json`
* [ ] RequirementFormView "Match" button returns correct service or no-match
* [ ] RequirementFormView "Save Requirement" persists and then matches
* [ ] RequirementFormView UI test validates dropdown loading, form interaction, and match results

---

## Phase 9: Docker Compose and Kubernetes Manifest Generation

**Goal**

Generate deployment manifests for the selected service. Support both Docker Compose (for edge/fieldbus scenarios) and Kubernetes (with nodeSelector constraints derived from DeploymentContext triples as described in the PDF).

**Tasks**

* [ ] Create `DockerComposeManifestService`
* [ ] Create `KubernetesManifestService`
* [ ] Create `ManifestResult`
* [ ] Generate container service name from selected service
* [ ] Add container image configuration from selected service
* [ ] Docker Compose: Add `restart: unless-stopped` baseline edge policy configuration
* [ ] Docker Compose: Add environment parameters block for asset ID, process variable, protocol, and deployment context
* [ ] Docker Compose: Configure `network_mode: host` only if justified for the edge/fieldbus scenario
* [ ] Kubernetes: Generate Pod or Deployment spec from selected service
* [ ] Kubernetes: Derive `nodeSelector` constraints from `sda:DeploymentContext` triples (safetyIntegrityLevel, determinismClass, networkZone)
* [ ] Kubernetes: Add container environment variables from CommunicationBinding and DataConnector
* [ ] Add unit tests using SnakeYAML to parse and verify generated Docker Compose YAML
* [ ] Add unit tests using SnakeYAML or official K8s client to verify generated Kubernetes YAML
* [ ] Add example file output under `docker/examples/` and `k8s/examples/`
* [ ] Create `DeploymentView`:
  * [ ] Cards per environment: Development, Testing, Production, Edge, Cloud
  * [ ] Each card with status, services, assets, health, region, version
  * [ ] Click to view generated Docker Compose and Kubernetes manifests
* [ ] Create `ManifestDisplay` component with syntax-highlighted YAML viewer
* [ ] Add Vaadin UI test for DeploymentView card rendering and manifest display

**Acceptance Criteria**

* [ ] Matching API response includes valid Docker Compose YAML
* [ ] Matching API response includes valid Kubernetes YAML
* [ ] YAML contains selected service name and specific target image
* [ ] YAML includes injected operational runtime metadata from CommunicationBinding and DeploymentContext
* [ ] Kubernetes YAML contains `nodeSelector` with `sda.ontology/sil`, `sda.ontology/deterministic`, `sda.ontology/zone`
* [ ] YAML can be parsed successfully using SnakeYAML
* [ ] Tests verify runtime property mappings
* [ ] DeploymentView shows environment cards with status
* [ ] Clicking a card displays generated manifests
* [ ] DeploymentView UI test validates environment cards and manifest viewer

---

## Phase 10: Matching Explanation

**Goal**

Make the result explainable so a user can understand why a service was selected.

**Tasks**

* [ ] Add `matchingExplanation` field to API response
* [ ] Include capability match reason
* [ ] Include protocol match reason
* [ ] Include access mode match reason
* [ ] Include latency constraint reason
* [ ] Include recall constraint reason
* [ ] Include deployment context reason
* [ ] Add explanation for no-match cases where possible
* [ ] Add tests for explanation content
* [ ] Add explanation panel to `DashboardView` showing match/no-match reasons per dimension
* [ ] Create `NotificationCenter` with grouped notifications:
  * [ ] Success, Information, Warning, Error, Background Tasks
* [ ] Create `NotificationPanel` component for header notification bell
* [ ] Add Vaadin UI test for DashboardView explanation panel and NotificationCenter interaction

**Acceptance Criteria**

* [ ] Successful match response explains why the service was selected
* [ ] No-match response explains that constraints were not satisfied
* [ ] Explanation is understandable without reading the SPARQL query
* [ ] Tests verify explanation content for successful and failed matches
* [ ] DashboardView shows explanation panel after match
* [ ] NotificationCenter groups notifications by type
* [ ] Explanation panel and NotificationCenter UI tests validate interaction

---

## Phase 11: Evaluation Dataset — Fermentation Skid and HVAC Plant

**Goal**

Create evaluation datasets based on the two real-world PDF examples: the fermentation skid (critical CPC, SIL-2, MTP input) and the HVAC plant (non-critical M+O, OPC UA Server Datasheet input). Include cross-plant semantic queries.

**Tasks**

* [ ] Add Example A: Fermentation Skid (Skid-A1)
  * [ ] Add MTP AML example file in `src/test/resources/examples/mtp-fermentation-skid.xml`
  * [ ] Add expected CSS-SDA triples for CommunicationBinding, DataConnector, DeploymentContext
  * [ ] Add FermentationControl and DosingControl css:Skill + css:SkillParameter instances
  * [ ] Add inferred OfferedCapability_TempControl and OfferedCapability_Dosing
  * [ ] Add RequiredCapability for batch step 3 (temperature-controlled fermentation)
  * [ ] Add positive match test (query returns FermentationControl)
  * [ ] Add negative match tests (wrong SIL, wrong zone, wrong protocol)
* [ ] Add Example B: HVAC Plant (AHU-01 + EM-01)
  * [ ] Add OPC UA Server Datasheet example file in `src/test/resources/examples/uaserver-ahu01.json`
  * [ ] Add expected CSS-SDA triples for AHU-01 CommunicationBinding (Sign, Basic128Rsa15, discoveryUrl)
  * [ ] Add expected CSS-SDA triples for EM-01 CommunicationBinding (SignAndEncrypt, Basic256Sha256)
  * [ ] Add deployment context with SoftRealTime, SIL-0, OT Zone 3
  * [ ] Add positive match test for HVAC energy optimisation query
  * [ ] Add negative match test (wrong determinism class, wrong zone)
* [ ] Add cross-plant semantic query (PDF Listing B.7): find all temperature services across both plants
* [ ] Add flowmeter diagnostics requirement `REQ_001` for backward compatibility
* [ ] Document expected matching results for all evaluation scenarios

**Acceptance Criteria**

* [ ] Evaluation dataset contains both fermentation skid and HVAC plant
* [ ] Fermentation skid query returns expected service with endpoint, SIL, and zone
* [ ] HVAC plant query returns expected device with CommunicationBinding properties
* [ ] Cross-plant query returns temperature services from both plants in one query
* [ ] No-match tests pass for wrong SIL, wrong zone, wrong protocol
* [ ] Flowmeter `REQ_001` still matches correctly for backward compatibility
* [ ] README documents evaluation scenarios with expected results
* [ ] Create `MonitoringView` with real-time widgets:
  * [ ] CPU, Memory, OPC UA Sessions, Connected Assets, Semantic Queries, Notifications
  * [ ] ApexCharts with live updates
  * [ ] Wire charts to `/actuator/prometheus` metrics or Micrometer MeterRegistry directly
  * [ ] Add metric counters for match requests, parser invocations, inference runs, manifest generations
* [ ] Wire NotificationCenter to system events (match, upload, inference, manifest generation)
* [ ] Add DashboardView cards for Digital Twins, Deployments, Running Simulations, Live OPC UA Sessions
* [ ] Add summary-card navigation: OPC UA Sessions, Connected Assets
* [ ] Add Vaadin UI test for MonitoringView chart rendering and NotificationCenter event display

**Acceptance Criteria**

* [ ] MonitoringView charts reflect current backend state
* [ ] NotificationCenter receives events from all backend services
* [ ] DashboardView visualizes all 8 metrics with live data
* [ ] All three use cases (flowmeter, fermentation, HVAC) render correctly in evaluation
* [ ] MonitoringView and NotificationCenter UI tests validate chart display and event grouping

## Phase 12: Documentation and Thesis Support

**Goal**

Document the prototype clearly for academic and engineering evaluation.

**Tasks**

* [ ] Write project overview
* [ ] Document architecture (five-step mapping procedure: Parse, Instantiate, Lift, Infer, Match)
* [ ] Document input file sources (MTP AML, OPC UA Server Datasheet, AAS)
* [ ] Document ontology classes (CSS-SDA dimensions, MTPModule, FieldDevice)
* [ ] Document SPARQL matching logic (multi-dimensional, cross-plant queries)
* [ ] Document MTP vs OPC UA Server Datasheet contribution to each CSS-SDA dimension
* [ ] Document REST API
* [ ] Document Docker Compose and Kubernetes manifest generation strategy
* [ ] Document Margo Sandbox integration
* [ ] Document evaluation cases (fermentation skid, HVAC plant, cross-plant)
* [ ] Add limitations section
* [ ] Add future work section

**Acceptance Criteria**

* [ ] README explains how to run the project
* [ ] Documentation explains the semantic mapping chain
* [ ] Documentation clearly explains the difference between Apache Jena, SPARQL, and Dagger
* [ ] Documentation includes example request and response
* [ ] Documentation includes example Docker Compose manifest output
* [ ] Documentation clearly states limitations

---

## Phase 13: EKA UI Polish — Graph, Settings, and Cross-Cutting Views

**Goal**

Add the remaining three EKA UI views (ontology graph visualization, settings, settings menu) that depend on all backend phases being complete. Apply cross-cutting accessibility, mobile QA, and performance optimization across all views. Add integration tests that exercise the full UI stack.

**Tasks**

* [ ] Create `GraphView` with Cytoscape.js integration:
  * [ ] Interactive ontology graph with zoom, pan, expand/collapse nodes, highlight paths, relationship labels, search nodes, center node, fullscreen
* [ ] Create `SettingsView`:
  * [ ] User preferences: Theme, Language, Time Zone, Notifications, Accessibility, Search Preferences
* [ ] Add settings menu to `MainLayout` header
* [ ] Add user profile menu to `MainLayout` header
* [ ] Implement keyboard navigation across all views:
  * [ ] Tab order, focus indicators, shortcut keys for main actions
* [ ] Implement ARIA labels on all interactive components
* [ ] Implement high contrast mode
* [ ] Implement screen reader support across all views
* [ ] Implement focus management for modals, wizards, and dialogs
* [ ] Implement virtual scrolling for large grids (SearchView, ExplorerView entity list)
* [ ] Implement lazy view loading for all views
* [ ] Minimize DOM updates and avoid unnecessary server round trips
* [ ] Add Vaadin view rendering tests for all UI views
* [ ] Add Vaadin component interaction tests for key workflows:
  * [ ] Dashboard navigation
  * [ ] Search with autocomplete and result selection
  * [ ] Service Registration Wizard complete flow
  * [ ] Upload flow with file selection and validation
  * [ ] Explorer tree expand/collapse
  * [ ] Graph zoom and pan
  * [ ] Settings save and reload
  * [ ] Light/dark mode toggle
* [ ] Audit all views for responsive layout on mobile viewport
* [ ] Audit all views for accessibility compliance

**Acceptance Criteria**

* [ ] Vaadin view rendering tests pass for all UI views across all phases
* [ ] Vaadin component interaction tests pass for all key workflows
* [ ] Ontology graph renders nodes and supports zoom, pan, highlight, and search
* [ ] SettingsView saves preferences and restores them on reload
* [ ] User profile menu shows user name and settings link
* [ ] All views have keyboard-navigable controls
* [ ] All interactive components have ARIA labels
* [ ] High contrast mode renders all views legibly
* [ ] Screen readers can navigate all views
* [ ] Large grids use virtual scrolling for performance
* [ ] Views lazy-load to minimize initial bundle size
* [ ] Vaadin tests pass for all view rendering scenarios
* [ ] Vaadin integration tests pass for all key workflows
* [ ] Responsive layout works on mobile, tablet, and desktop viewports

---

## Phase 14: Margo Sandbox Deployment Context

**Goal**

Use Margo Sandbox as an industrial edge deployment/interoperability context. Validate that the selected service or generated deployment artifact can fit into an edge deployment workflow.

**Tasks**

* [ ] Study the Margo Sandbox application deployment workflow
* [ ] Define how the selected service maps to a Margo-style application package
* [ ] Add a simple Margo deployment descriptor or package metadata if required
* [ ] Document limitations of the Margo Sandbox integration

**Acceptance Criteria**

* [ ] Margo integration does not replace Apache Jena or SPARQL
* [ ] Margo integration is documented as deployment-context validation only

---

## Optional Phase 15: Runtime Verification Status

**Goal**

Add a simple lifecycle status update after manifest generation.

**Tasks**

* [ ] Add lifecycle status property to requirement
* [ ] Mark requirement as `Matched` after service selection
* [ ] Mark requirement as `DeploymentManifestGenerated` after YAML generation
* [ ] Add endpoint to read requirement status
* [ ] Add tests for status changes

**Acceptance Criteria**

* [ ] Requirement status changes after successful match
* [ ] Requirement status changes after manifest generation
* [ ] Status can be queried through API

---

## Optional Phase 16: Dagger Automation and Deployment Workflow

**Goal**

Use Dagger to automate build, test, image creation, manifest validation, and optional deployment/testing of the generated Docker Compose output.

**Tasks**

* [ ] Add `dagger.json`
* [ ] Add basic Dagger pipeline using Java SDK
* [ ] Add Dagger function for running tests
* [ ] Add Dagger function for building the application
* [ ] Add Dagger function for building the Docker image
* [ ] Add Dagger function `test`
* [ ] Add Dagger function `buildJar`
* [ ] Add Dagger function `buildImage`
* [ ] Add Dagger function `validateOntology`
* [ ] Add Dagger function `validateSparql`
* [ ] Add Dagger function `generateComposeExample`
* [ ] Add Dagger function `validateCompose`
* [ ] Add Dagger function `runEndToEnd`
* [ ] Start application in a container for API tests
* [ ] Run requirement matching API test from Dagger
* [ ] Validate generated Docker Compose YAML
* [ ] Document local Dagger commands

**Acceptance Criteria**

* [ ] Dagger can run the same workflow locally and in CI
* [ ] Dagger test workflow fails if ontology loading fails
* [ ] Dagger test workflow fails if SPARQL matching fails
* [ ] Dagger test workflow fails if Docker Compose YAML is invalid
* [ ] Dagger can build a runnable container image
* [ ] Dagger can run tests
* [ ] Dagger can build the application
* [ ] Dagger can build the Docker image
* [ ] Dagger can execute an end-to-end matching scenario

---

## MVP Definition

The minimum viable prototype is complete when the following phases are done:

```text
Phase 1:  Project Foundation
Phase 2:  CSS-SDA Ontology Model
Phase 3:  Ontology Loading with Apache Jena
Phase 4:  Input File Parsers — MTP AML and OPC UA Server Datasheet
Phase 5:  SPARQL Matching Query
Phase 6:  Ontology Inference — OWL Reasoner
Phase 7:  Semantic Reasoner Service
Phase 8:  REST API
Phase 9:  Docker Compose and Kubernetes Manifest Generation
Phase 10: Matching Explanation
Phase 11: Evaluation Dataset — Fermentation Skid and HVAC Plant
Phase 12: Documentation and Thesis Support
Phase 13: EKA UI Polish — Graph, Settings, and Cross-Cutting Views
Phase 14: Margo Sandbox Deployment Context
```

Runtime Verification Status is optional (Optional Phase 15). Dagger automation is optional (Optional Phase 16).

---

## MVP Demo Script

1. Start the application.

2. Send request:

```http
POST /api/v1/requirements/match
```

Body:

```json
{
  "requirementId": "REQ_001"
}
```

3. Show selected service:

```text
FlowmeterDiagnosticsService
```

4. Show explanation:

```text
The service matches the required asset condition monitoring capability, uses OPC UA in read-only mode, satisfies recall and latency constraints, and can run in the selected deployment context.
```

5. Show generated Docker Compose manifest.

6. (Optional) Run Dagger workflow if Dagger is configured:

```bash
dagger call test
dagger call build-image
dagger call run-end-to-end
```

---

## Final Deliverables

The final project should deliver:

* Working Java 25 and Spring Boot 4.0.x application
* CSS-SDA-inspired ontology with full dimensions (Capability, Skill, CommunicationBinding, DataConnector, DeploymentContext)
* RDF/Turtle ontology file
* MTP AML parser — generates CSS-SDA triples from VDI/VDE/NAMUR 2658 files
* OPC UA Server Datasheet parser — generates CSS-SDA triples from IDTA 02009-1-0 submodels
* SPARQL matching query — multi-dimensional matching across all CSS-SDA dimensions
* OWL reasoner inference — derives OfferedCapability from SkillParameter semantics
* Apache Jena-based semantic reasoner
* Requirement matching REST API
* Docker Compose and Kubernetes manifest generation
* Unit, integration, and Vaadin UI tests
* Optional Dagger automation pipeline
* Evaluation dataset with fermentation skid and HVAC plant examples + cross-plant queries
* Documentation for academic evaluation
* Margo Sandbox deployment-context extension
* EKA Enterprise User Interface (Vaadin 25 with dashboard, explorer, search, registration wizard, ontology graph, monitoring, upload center, settings, deployment view, notification center, and light/dark mode)
