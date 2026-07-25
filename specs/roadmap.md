# Roadmap: Semantic SDA Mapper

## Roadmap Principle

**Phases 1–16b** (complete) delivered the semantic reasoning core: ontology modeling, input file parsing, OWL inference, SPARQL matching, manifest generation, evaluation datasets, OPC UA visualization, multi-file import, interactive completion, and file storage. **Phases 17–29** deliver the platform operations layer: persistent service registry, asset management, deployment execution, administration, workspace isolation, knowledge graph visualization, and reporting — evolving the prototype into a usable engineering platform.

Each phase is a small vertical slice that produces something executable, testable, and reviewable.

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

Apache Jena and SPARQL are part of the semantic reasoning layer.

```text
Apache Jena + SPARQL = semantic matching core
Docker Compose = generated deployment output
```

---

## Technical Stack

| Layer / Area | Selected Technology |
| :--- | :--- |
| Language & Runtime | Java 25 |
| Backend Framework | Spring Boot 4.0.x |
| UI Framework | Vaadin 25.x (Lumo Theme, Server-Side Binding) |
| Build System | Apache Maven |
| RDF Processing | Apache Jena (apache-jena-libs) |
| OWL Reasoner | Apache Jena (OWL_MEM_MICRO_RULES_INF) |
| Query Language | SPARQL |
| Ontology Format | RDF/Turtle (.ttl) |
| Database & ORM | PostgreSQL via Spring Data JPA / Hibernate |
| Database Migrations | Flyway (flyway-core, flyway-database-postgresql) |
| Security | Spring Security (Form Login, OAuth2, API Key Authentication) |
| Graph Visualization | Cytoscape.js (integrated via Vaadin frontend wrapper) |
| Dashboard Charts | ApexCharts (integrated via Vaadin frontend wrapper) |
| Observability | Micrometer, Prometheus, OpenTelemetry (OTel), Logback |
| Manifest Parsing/Export | SnakeYAML (Docker Compose YAML & Kubernetes YAML) |

---

## UI Specification (Derived from EKA-SDA-Platform-SRS.docx)

The UI is built with Vaadin 25 (server-side Java rendering) and follows the design principles, navigation structure, and screen specifications defined in the Software Requirements Specification.

### Design Principles

- The ontology hierarchy (CSS-SDA tree: Intent → Use Case/Requirement → Capability → Skill → Service → Communication Binding → Data Connector → Deployment Context) is **always visible as left-hand navigation**, so both personas can browse by concept as well as search by keyword.
- Registration and discovery are the two primary flows; monitoring status uses a consistent color/label convention aligned with **NAMUR NE 107** (green = active/good, amber = check/standby, red = failure).
- Wherever an automated step produces a proposal (translation, rule generation, deployment scripting), the UI **shows the proposal and requires explicit user confirmation** before it is committed.
- **Color is never the sole means of conveying status**; status indicators pair color with a text label (FR-UI-3, SRS §6.4).
- The layout remains usable at reduced desktop widths by **collapsing the ontology sidebar to an icon rail**; fully native mobile layout is out of scope for this version (SRS §6.4).

### Navigation Structure (SRS §6.2)

A persistent left sidebar presents the CSS-SDA tree. Selecting a node filters the main content area. A persistent **"+ Service Registration"** action is always available from the sidebar.

```
MainLayout (AppLayout)
├── Header
│   ├── CSS-SDA Logo & Banner (clickable to Home)
│   ├── Global SearchBar (free-text + filter chips)
│   ├── Active Workspace Switcher (X-Workspace-Id scoping)
│   ├── NotificationCenter Bell
│   ├── Light/Dark Theme Toggle (ThemeConfig)
│   └── User Profile & Settings Quick Menu
│
└── Navigation Drawer (CSS-SDA Hierarchy)
    ├── [+ Service Registration] ──────> RegistrationWizardView
    │
    ├── Intent & Requirements ────────> RequirementFormView
    ├── Semantic Explorer ───────────> ExplorerView
    ├── Capability & Services ───────> ServiceRegistryView / RegistrationWizardView
    ├── Discovery & Matching ────────> SearchView / MatchingAnalysisView
    ├── Import & Validation ─────────> ImportWorkspaceView / ImportWizardView
    ├── Asset Management ────────────> PlantHierarchyView / DigitalTwinView
    ├── Deployment Execution ────────> DeploymentView / DeploymentDashboardView
    ├── Workflow Engine ─────────────> WorkflowView / WorkflowDashboardView
    ├── Operations & Runtime ────────> RuntimeDashboardView / ImpactAnalysisView
    ├── Monitoring & OPC UA ─────────> MonitoringView / OpcUaVisualizationView
    ├── Service Marketplace ─────────> MarketplaceView / MarketplacePublisherView
    ├── Knowledge Graph ─────────────> GraphView
    ├── Platform Admin ──────────────> AdminView / WorkflowDefinitionView
    └── Analytics & Reports ─────────> ReportView / SettingsView
```

### Key Screens (from SRS §6.3)

#### Screen 1: Service Discovery / Search (SRS §6.3.1, UI-DISC-1 through UI-DISC-5)

Used by the **Service Requester** to find a service matching a capability or requirement.

| SRS Requirement | Implementation |
| :--- | :--- |
| UI-DISC-1: Search bar accepts free-text queries against capability, skill, and requirement names | `SearchBar` component in MainLayout header with fuzzy/autocomplete |
| UI-DISC-2: Filter chips (Capability, Communication Binding, Status) can be added/removed without retyping | `SearchView` filter chip bar |
| UI-DISC-3: Each result shown as card: service name, matched capability, binding/connector summary, live status indicator | `SearchView` result Grid with status badges |
| UI-DISC-4: "View" action on result card opens service detail/monitoring view | Navigate to `MonitoringView` or `OpcUaVisualizationView` |
| UI-DISC-5: Ontology tree click-to-filter narrows results to services under selected node | `ExplorerView` tree → filter scope on `SearchView` |

**Components:** `SearchBar`, `SearchView`, `MatchingAnalysisView` (multi-candidate radar chart + constraint violations)

#### Screen 2: Service Registration (SRS §6.3.2, UI-REG-1 through UI-REG-5)

Used by the **Service Provider** to register a new service against the CSS-SDA ontology.

| SRS Requirement | Implementation |
| :--- | :--- |
| UI-REG-1: Multi-step form: (1) Service Details, (2) Capability & Communication Binding, (3) Review & Publish | `RegistrationWizardView` 8-step wizard |
| UI-REG-2: Step 2 allows uploading an OPC UA node-set file as the service's Data Connector | Upload step in wizard with drag-and-drop |
| UI-REG-3: "Translator Assist" panel requests LLM-generated suggestion for Capability/Skill classification; marks as suggestion pending confirmation | Translator Assist panel with confirm/reject UI |
| UI-REG-4: Deployment Context field offers environments supported by Deployment Manager (Edge/Raspberry Pi, Sandbox, Kubernetes) | Dropdown populated from environment config |
| UI-REG-5: Review step summarizes all values and requires explicit publish confirmation before writing to Catalog and Knowledge Graph | Review step with "Register" confirmation button |

**Components:** `RegistrationWizardView`, `ServiceRegistryView` (lifecycle management)

#### Screen 3: Monitoring View (SRS §6.3.3, UI-MON-1 through UI-MON-4)

Reached from a result card's "View" action or directly by a Service Provider checking on a registered service.

| SRS Requirement | Implementation |
| :--- | :--- |
| UI-MON-1: Live sensor/process values from OPC UA endpoint, refreshed at configurable interval | `OpcUaVisualizationView` with ApexCharts live data |
| UI-MON-2: Condition status using NAMUR NE 107-aligned categories and colors | `OpcUaStatusBadge` with NE 107 color coding |
| UI-MON-3: Current container status (running/stopped/restarting) from Container Monitoring Service | `RuntimeDashboardView` container status cards |
| UI-MON-4: Manual start/stop request from monitoring view, subject to Container Monitoring Service rules | Action buttons in monitoring view |

**Components:** `MonitoringView`, `OpcUaVisualizationView`, `RuntimeDashboardView`, `DeploymentDashboardView`

### Non-Functional UI Requirements (SRS §5)

| Category | Requirement |
| :--- | :--- |
| Interoperability | All device connectivity conforms to OPC UA standard |
| Extensibility | New CSS-SDA classes addable via Protégé without redeployment |
| Scalability | Services run as Kubernetes-orchestrated containers |
| Usability | First-time Service Provider completes registration without external help, aided by Translator Assist |
| Auditability | Every automated mapping, rule, or deployment script generation is logged |
| Availability | Container Monitoring Service detects failed containers and reflects status within configurable time window |
| Security | Registration/deployment restricted to authenticated Service Providers; discovery/monitoring read-only for Requesters |

### Microservice Architecture (SRS §3.1)

| # | Service | Functionality | Implementation |
| :--- | :--- | :--- | :--- |
| 1 | UI Service | Service registration, discovery, monitoring | Vaadin 25 (replaces Blazor from SRS) |
| 2 | Catalog Service | System of record for registered services | PostgreSQL + Spring Data JPA |
| 3 | Translator Service | Maps OPC UA node sets to CSS-SDA classes | Apache Jena parsers + LLM assist (planned) |
| 4 | CSS-SDA Knowledge Graph | Stores CSS-SDA instances | Apache Jena in-memory + TTL persistence |
| 5 | Semantic Reasoner | SPARQL/SWRL matching | Apache Jena + SPARQL queries |
| 6 | Deployment Manager (MARGO) | K8s workflow generation | SnakeYAML manifest generation |
| 7 | Container Monitoring (MARGO) | Container status polling | Docker/K8s API integration (planned) |
| 8 | OPC UA Client (M+O) | Live sensor monitoring | Eclipse Milo OPC UA client |

---

## Phase 17: Persistent Service Registry

**Goal**

Replace the in-ontology-only service storage with a persistent, versioned service registry that supports full service lifecycle management, provider ownership, audit trail, and approval workflows.

**Tasks**

* [ ] Design and create database tables:
  * [ ] `service` (id, name, description, status, provider_id, created_at, updated_at)
  * [ ] `service_version` (id, service_id, version, image, ontology_snapshot, changelog, created_by, created_at)
  * [ ] `provider` (id, name, contact_email, organization, website, created_at)
  * [ ] `service_status` (id, service_id, status, changed_by, changed_at, reason)
* [ ] Create `ServiceRegistryService` — CRUD operations for services, versions, and providers
* [ ] Implement service lifecycle: DRAFT → REVIEW → APPROVED → PUBLISHED → DEPRECATED → RETIRED
* [ ] Implement version management: semantic versioning, version history viewer, rollback
* [ ] Implement provider management: registration, ownership verification, provider dashboard
* [ ] Implement audit trail and approval workflow
* [ ] Create `ServiceRegistryView` (Vaadin)
* [ ] Add REST API: `GET/POST/PUT/DELETE /api/v1/registry/services`, versions, providers
* [ ] Add unit and integration tests
* [ ] Add Vaadin UI test for ServiceRegistryView

**Acceptance Criteria**

* [ ] Services can be created with DRAFT status and transition through full lifecycle
* [ ] Each service version stores an ontology snapshot at the time of publication
* [ ] Version history shows all published versions with changelog
* [ ] Providers can be registered and associated with services
* [ ] Audit trail records every status change with user, timestamp, and reason
* [ ] REST API supports all CRUD operations for services, versions, and providers

---

## Phase 18: Asset & Digital Twin Management

**Goal**

Build an asset inventory system that organizes physical devices, plant hierarchy, and digital twins.

**Tasks**

* [ ] Design and create database tables for plant hierarchy (plant, area, production_line, asset, device, digital_twin)
* [ ] Create `AssetManagementService` and `DigitalTwinService`
* [ ] Create `PlantHierarchyView` (Vaadin) — interactive tree: Plant → Area → Production Line → Asset → Device
* [ ] Create `DigitalTwinView` (Vaadin) — twin list, status tracking, synchronization controls
* [ ] Add REST API: `GET/POST /api/v1/assets/*`, digital twins
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Plants, areas, production lines, assets, and devices can be organized hierarchically
* [ ] Each asset can have one or more digital twins with status tracking
* [ ] REST API covers all CRUD operations
* [ ] UI integration tests validate hierarchy interaction and twin management

---

## Phase 19: Deployment Management (Execution & Lifecycle)

**Goal**

Move from manifest generation-only to active deployment management. Implements the Deployment Manager Service (MARGO) from SRS §3.1 (FR-DEP).

**Tasks**

* [ ] Design and create database tables (environment, cluster, deployment, deployment_history)
* [ ] Create `DeploymentExecutionService` — deploy, stop, restart, rollback across Docker and Kubernetes
* [ ] Create `DeploymentDashboardView` (Vaadin) — environment cards, deployment list, action buttons
* [ ] Create `DeploymentWizardView` (Vaadin) — guided 4-step deployment flow
* [ ] Add REST API: `GET/POST /api/v1/deployments`, stop, restart, rollback, history
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Services can be deployed to configured environments (Docker or Kubernetes)
* [ ] Running deployments can be stopped, restarted, and rolled back
* [ ] Deployment history records every action with user and timestamp
* [ ] Deployment dashboard shows per-environment status cards

---

## Phase 20: End-to-End Execution Workflow

**Goal**

Connect isolated features (requirement, match, deploy, monitor) into a seamless end-to-end workflow.

**Tasks**

* [ ] Create `ExecutionWorkflowService` — orchestrates requirement → match → deploy → monitor → feedback
* [ ] Create `WorkflowView` (Vaadin) — 8-step guided wizard: Define → Match → Compare → Select → Generate → Deploy → Monitor → Feedback
* [ ] Create `CandidateComparisonPanel` — side-by-side comparison of top candidates
* [ ] Create `WorkflowDashboardView` — active sessions, history, metrics
* [ ] Add REST API: `POST/GET /api/v1/workflows`, next, rollback, history
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Users can create a workflow from requirement to deployment in a single guided flow
* [ ] Side-by-side comparison of up to 3 candidates across all CSS-SDA dimensions
* [ ] Deployment preview shows generated manifests before execution
* [ ] Monitor step shows live deployment status after deploy
* [ ] Feedback step allows users to rate and comment on match quality

---

## Phase 21: Project / Workspace Management

**Goal**

Introduce workspace isolation so multiple projects, plants, or teams can work independently without ontology pollution.

**Tasks**

* [ ] Design and create database tables (workspace, workspace_member)
* [ ] Create `WorkspaceService` — workspace CRUD, membership, ontology isolation
* [ ] Create `WorkspaceSwitcher` component in MainLayout header
* [ ] Create `WorkspaceSettingsView` (Vaadin)
* [ ] Implement `X-Workspace-Id` header support for API scoping
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Users can create multiple workspaces with independent ontologies
* [ ] SPARQL queries, services, assets, and deployments are scoped to the active workspace
* [ ] REST API supports workspace scoping via `X-Workspace-Id` header

---

## Phase 22: Administration & Role Management

**Goal**

Implement full administration features: user management, role-based access control, organizations, API keys, and platform settings.

**Tasks**

* [ ] Design and create database tables (organization, user, role, api_key, platform_setting)
* [ ] Create `AdminService`, `ApiKeyService`, `PlatformSettingsService`
* [ ] Extend `SecurityConfig` with role-based access and API key authentication
* [ ] Create `AdminView` (Vaadin) — Users, Organizations, API Keys, Settings, Audit Log tabs
* [ ] Add REST API: `GET/POST/PUT/DELETE /api/v1/admin/*`
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Users can be created, edited, and disabled with role assignment
* [ ] API keys can be generated per user with scope and expiration
* [ ] Platform settings are configurable through UI and API
* [ ] Audit log records user activity with filters

---

## Phase 23: Knowledge Graph Visualization

**Goal**

Replace the current ontology tree with an interactive, searchable knowledge graph visualization using Cytoscape.js.

**Tasks**

* [ ] Integrate Cytoscape.js into Vaadin frontend (`GraphComponent` LitElement wrapper)
* [ ] Create `GraphDataService` — serve ontology entities and relationships as graph nodes/edges
* [ ] Create `GraphView` (Vaadin) — full-screen interactive graph with zoom/pan, expand/collapse, path highlight, search, export
* [ ] Implement impact analysis overlay
* [ ] Add REST API: `GET /api/v1/graph/nodes`, edges, path, impact
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Knowledge graph renders all CSS-SDA entities and relationships
* [ ] Nodes are color-coded by type
* [ ] Zoom, pan, expand/collapse, and search work without page reload
* [ ] Impact analysis highlights all dependents of a selected service
* [ ] Dependency map is exportable as an image

---

## Phase 24: Multi-Candidate Matching Analysis

**Goal**

Extend single-result matching to ranked multi-candidate matching with similarity scores and constraint violations.

**Tasks**

* [ ] Create `RankedMatchResult` with rank, scores, violations, rejection reasons
* [ ] Extend `SemanticReasonerService` for ranked candidates
* [ ] Create `MatchingAnalysisView` (Vaadin) — ranked list, radar chart, comparison
* [ ] Create `SimilarityRadarChart` and `ConstraintViolationPanel` components
* [ ] Update `POST /api/v1/requirements/match` for multi-candidate mode
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] API returns multiple ranked candidates with overall and per-dimension scores
* [ ] Eliminated candidates returned with rejection reasons
* [ ] Radar chart visualizes per-dimension scores
* [ ] Side-by-side comparison of up to 3 candidates

---

## Phase 25: Change Impact Analysis

**Goal**

Enable users to understand the impact of service changes on requirements, deployments, and assets.

**Tasks**

* [ ] Create `ChangeImpactAnalysisService` — analyze update, deprecation, requirement change
* [ ] Define impact severity: BREAKING, WARNING, INFO
* [ ] Create `ImpactAnalysisView` (Vaadin) — summary, affected items, recommendations
* [ ] Add REST API: `GET /api/v1/impact/service`, requirement, deployment
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Impact severity classified as BREAKING, WARNING, or INFO
* [ ] Affected deployments show current status and rollback availability
* [ ] Recommended actions provided per affected item

---

## Phase 26: Live Runtime Integration

**Goal**

Connect to Docker Engine and Kubernetes clusters for live runtime monitoring. Implements the Container Monitoring Service (MARGO) from SRS §3.1 (FR-MON).

**Tasks**

* [ ] Create `DockerClientService` and `KubernetesClientService`
* [ ] Create `RuntimeMonitorService` — periodic polling, state change tracking
* [ ] Create `RuntimeDashboardView` (Vaadin) — cluster overview, container/pod list, live log viewer
* [ ] Create `LogViewer` component — real-time log stream with search, download
* [ ] Add REST API: `GET /api/v1/runtime/*`, restart, stop, logs
* [ ] Add integration tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Platform connects to Docker Engine and Kubernetes clusters securely
* [ ] Live log viewer streams logs with tail, follow, and filter
* [ ] Container/pod list shows correct status, uptime, and resource usage
* [ ] Restart and stop actions work for individual containers

---

## Phase 27: Service Marketplace

**Goal**

Create a service marketplace for discovering, comparing, and selecting automation services.

**Tasks**

* [ ] Create `MarketplaceService` — search, category browse, version comparison, ratings
* [ ] Create `MarketplaceView` (Vaadin) — search bar, category browser, detail page, favorites
* [ ] Create `MarketplacePublisherView` (Vaadin) — publish from registry, set pricing tier
* [ ] Add REST API: `GET/POST /api/v1/marketplace/*`
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Users can search published services with full-text and category filters
* [ ] Version comparison shows ontology diff between releases
* [ ] Users can rate and review services
* [ ] Publishers can publish services from the registry

---

## Phase 28: Workflow & Lifecycle Engine

**Goal**

Implement configurable workflow state machines for requirements, services, capabilities, and other entities.

**Tasks**

* [ ] Design and create database tables (workflow_definition, workflow_instance, workflow_transition)
* [ ] Create `WorkflowEngineService` — state machine validation, transition hooks, role enforcement
* [ ] Create `WorkflowDefinitionView` (Vaadin) — admin view for configuring state machines
* [ ] Create `EntityLifecyclePanel` — reusable lifecycle state, transition buttons, history timeline
* [ ] Integrate lifecycle panels into existing views
* [ ] Add REST API: `GET/POST /api/v1/workflow/*`
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] Entities follow configurable state machine lifecycles
* [ ] Transitions are validated against allowed paths
* [ ] Transition history is recorded with user, timestamp, and comment
* [ ] Workflow definitions are configurable through admin UI

---

## Phase 29: Reporting & Export

**Goal**

Generate structured reports in PDF and Excel for architecture, matching, ontology, and deployment scenarios.

**Tasks**

* [ ] Create `ReportEngineService` — Architecture, Matching, Ontology, Deployment reports
* [ ] Implement PDF generation (OpenPDF) and Excel generation (Apache POI)
* [ ] Create `ReportView` (Vaadin) — type selector, configuration, preview, export buttons
* [ ] Create `ReportSchedulerService` — schedule recurring reports
* [ ] Add REST API: `POST /api/v1/reports/generate`, schedule, archive, download
* [ ] Add unit tests and Vaadin UI tests

**Acceptance Criteria**

* [ ] PDF reports have professional layout with cover page, TOC, page numbers
* [ ] Excel reports have one sheet per entity type with auto-filter
* [ ] Reports can be exported as PDF and Excel
* [ ] Reports can be scheduled for recurring generation

---

## SRS Traceability

| SRS Section | SRS ID | Roadmap Phase | Component |
| :--- | :--- | :--- | :--- |
| §4.2 FR-UI-1 | Service Registration workflow | Phase 8 (done) | `RegistrationWizardView` |
| §4.2 FR-UI-2 | Service Discovery workflow | Phase 5 (done) | `SearchView`, `SearchBar` |
| §4.2 FR-UI-3 | Monitoring view with NAMUR NE 107 | Phase 11, 11b (done) | `MonitoringView`, `OpcUaVisualizationView` |
| §4.2 FR-UI-4 | Ontology hierarchy as navigable tree | Phase 2, 3 (done) | `ExplorerView`, `OntologyTree` |
| §4.3 FR-CAT-1 | Persist registered services | Phase 17 | `ServiceRegistryService` |
| §4.4 FR-TRA-1 | Translator Service: OPC UA → CSS-SDA | Phase 4 (done) | `MTPAmlParser`, `UaServerDatasheetParser` |
| §4.5 FR-KG-1 | Knowledge Graph storage | Phase 3, 16b (done) | `OntologyModelLoader`, `FileStorageService` |
| §4.6 FR-REA-1 | Semantic Reasoner matching | Phase 5, 7 (done) | `SemanticReasonerService` |
| §4.7 FR-DEP-1 | Deployment Manager (MARGO) | Phase 9 (done), 19 | `DockerComposeManifestService`, `DeploymentExecutionService` |
| §4.8 FR-MON-1 | Container Monitoring (MARGO) | Phase 26 | `RuntimeMonitorService` |
| §4.9 FR-OPC-1 | OPC UA Client monitoring | Phase 11b (done) | `OpcUaVisualizationService` |
| §6.3.1 UI-DISC-* | Service Discovery UI | Phase 5 (done) | `SearchView` |
| §6.3.2 UI-REG-* | Service Registration UI | Phase 8 (done) | `RegistrationWizardView` |
| §6.3.3 UI-MON-* | Monitoring View UI | Phase 11, 11b (done) | `MonitoringView`, `OpcUaVisualizationView` |
