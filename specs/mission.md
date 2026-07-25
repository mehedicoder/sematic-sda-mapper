# Mission: Semantic Requirement-to-Service Mapping for Software-Defined Automation

## Project Name

**Semantic SDA Mapper**

## Project Mission

The mission of this project is to evolve the semantic reasoning prototype into a usable engineering platform that maps industrial automation requirements to deployable software services using a small ontology, SPARQL-based semantic reasoning, and platform-grade operational management.

The project demonstrates a complete Software-Defined Automation workflow where high-level automation requirements can be matched with available automation services based on capability, protocol, data connector, performance, and deployment constraints — and where the resulting service can be registered, deployed, monitored, and managed through a unified platform.

This prototype demonstrates the full five-step semantic mapping procedure:

```text
Input Files (MTP AML, OPC UA Server Datasheet, AAS)
→ Parse
→ Instantiate (CSS-SDA ontology ABox triples)
→ Lift (MTP Service → css:Skill)
→ Infer (OWL reasoner derives OfferedCapability)
→ Match (SPARQL multi-dimensional query)
→ Docker Compose / Kubernetes Manifest
```

And the platform operations layer that makes it usable in practice:

```text
Platform Layer
→ Persistent Service Registry
→ Asset & Digital Twin Management
→ Deployment Management
→ Multi-File Semantic Import
→ Import Completion & Validation
→ Execution Workflow Orchestration
→ Administration & Role Management
→ Project / Workspace Isolation
→ Reporting & Export
```

The CSS-SDA model: SDA-Service := ⟨ Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext ⟩

## Problem Statement

In traditional industrial automation, mapping an automation requirement to a suitable service or device is often manual, vendor-specific, and difficult to reuse.

For example, if an engineer needs to monitor the health of a flowmeter, they must manually identify:

- Which service can perform asset-health monitoring
- Which process variables are required
- Which communication protocol is supported
- Whether the service can access the required asset data
- Whether the service satisfies latency, accuracy, and deployment constraints
- How the selected service should be deployed

Furthermore, once a service is selected, the engineer must manage its entire lifecycle:

- Register the service with version history and provider metadata
- Track which physical assets and digital twins it interacts with
- Deploy, stop, restart, and roll back the service across environments
- Monitor runtime health and redeploy on failure
- Understand the impact of service updates on existing requirements and deployments
- Work within project or workspace boundaries to avoid ontology pollution

This project aims to show how semantic modeling combined with a platform operations layer can make this process fully machine-understandable and largely automatable.

## Target Use Cases

The project targets three use cases that span critical and non-critical automation domains, each with different input file formats:

### Use Case A: Flowmeter Asset-Health Monitoring (Legacy Baseline)

```text
Monitor the health of flowmeter FT-101.
The selected service must provide asset condition monitoring,
use OPC UA in read-only mode,
achieve recall >= 0.90,
have latency <= 60 ms,
and run in an on-premise plant-network deployment context.
```

### Use Case B: Fermentation Skid (Skid-A1) — Critical Process Control

Source: MTP AML file (VDI/VDE/NAMUR 2658)

- Reaction vessel with agitator and cooling jacket
- CPC category, SIL-2, PROFINET, OPC UA
- Skills: FermentationControl, DosingControl
- Inferred capabilities: TempControl, Dosing
- Batch step 3 requires SIL-2 temperature-controlled fermentation

### Use Case C: HVAC Plant (AHU-01 + EM-01) — Non-Critical Monitoring & Optimization

Source: OPC UA Server Datasheet (IDTA 02009-1-0 AAS submodel)

- AHU controller with BACnet gateway + energy meter with native OPC UA
- M+O category, SoftRealTime, OT Zone 3
- Different security modes per device (Sign vs SignAndEncrypt)
- Cross-plant queries find temperature services across both plants

## Core Objective

The core objective is to implement and validate the three target use cases end-to-end, and to provide the platform infrastructure required to operate them in an engineering environment:

1. **Use Case A (NE 107 Flowmeter):** Parse OPC UA Server Datasheet → CSS-SDA triples → SPARQL match → select FlowmeterDiagnosticsService → reject when NE 107 status = Failure → generate deployment manifest.
2. **Use Case B (Fermentation Skid):** Parse MTP AML → CSS-SDA triples → OWL inference derives TempControl/Dosing capabilities → SPARQL match → select FermentationSkidService → reject when SIL or determinism mismatch.
3. **Use Case C (HVAC Plant):** Parse OPC UA Server Datasheet → CSS-SDA triples → SPARQL match → select HVACTempAnalysisService → cross-plant query across PlantA and PlantB → reject when networkZone mismatch.
4. **Platform Operations:** Provide service registry, asset management, deployment management, multi-file import, administration, project workspaces, and reporting on top of the semantic core.

## Roadmap Phase Mapping

### Completed Phases (1–14)

| Phase | Use Case A | Use Case B | Use Case C | Description |
|---|---|---|---|---|
| 1 | Done | Done | Done | Project foundation, OPC UA simulation, NE 107 diagnostics |
| 2 | **Primary** | Yes | Yes | CSS-SDA ontology with all 3 use case instances |
| 3 | Yes | Yes | Yes | Jena model loader |
| 4 | Yes | **Primary** | Yes | MTP AML parser (B), OPC UA Datasheet parser (A, C) |
| 5 | **Primary** | Yes | Yes | SPARQL matching query (paper Table I) |
| 6 | Yes | **Primary** | Yes | OWL reasoner (infers capabilities from skill parameters) |
| 7 | Yes | Yes | Yes | Semantic reasoner service (orchestrates parse→infer→match) |
| 8 | Yes | Yes | Yes | REST API (POST /api/v1/requirements/match) |
| 9 | Yes | Yes | Yes | Docker Compose + Kubernetes manifest generation |
| 10 | Yes | Yes | Yes | Matching explanation (multi-dimensional) |
| 11 | Yes | Yes | Yes | Evaluation dataset validation |
| 11b | Yes | Yes | Yes | OPC UA data visualization client |
| 12 | — | — | — | Documentation and thesis support |
| 13 | — | — | — | EKA UI polish, graph, settings |
| 14 | — | — | — | Margo Sandbox deployment context |

### Future Platform Phases (15+)

| Phase | Priority | Description |
|---|---|---|
| 15 | 🔴 High | Multi-File Semantic Import |
| 16 | 🔴 High | Interactive Import Completion & Completeness Analysis |
| 17 | 🔴 High | Persistent Service Registry |
| 18 | 🔴 High | Asset & Digital Twin Management |
| 19 | 🔴 High | Deployment Management (Execution & Lifecycle) |
| 20 | 🔴 High | End-to-End Execution Workflow |
| 21 | 🟠 Medium | Project / Workspace Management |
| 22 | 🟠 Medium | Administration & Role Management |
| 23 | 🟠 Medium | Knowledge Graph Visualization |
| 24 | 🟠 Medium | Multi-Candidate Matching Analysis |
| 25 | 🟠 Medium | Change Impact Analysis |
| 26 | 🟠 Medium | Live Runtime Integration |
| 27 | 🟢 Lower | Service Marketplace |
| 28 | 🟢 Lower | Workflow & Lifecycle Engine |
| 29 | 🟢 Lower | Reporting & Export |

These platform phases build directly on the semantic foundation completed in Phases 1–14 and move the project from a research prototype to a usable engineering platform.

## In Scope

The project includes:

- A small CSS-SDA-inspired ontology with full dimension support
- MTP AML parser (VDI/VDE/NAMUR 2658) that generates CSS-SDA triples
- OPC UA Server Datasheet parser (IDTA 02009-1-0) that generates CSS-SDA triples
- OWL reasoner for inferring offered capabilities from skill parameters
- SPARQL queries for multi-dimensional semantic matching
- Cross-plant SPARQL queries across multiple input file sources
- Java 25 backend implementation
- Spring Boot 4.0.x REST API
- Apache Jena-based RDF, SPARQL, and OWL processing
- Docker Compose manifest generation (for edge/fieldbus deployment)
- Kubernetes YAML generation with nodeSelector from deployment context
- Unit, integration, and UI tests for all components
- Docker Compose for local development (application + OPC UA simulation)
- Node-RED-based OPC UA server simulation in Docker Compose
- Evaluation dataset with three use cases (flowmeter, fermentation skid, HVAC plant)
- Documentation for academic and engineering evaluation
- Vaadin 25 enterprise UI with dashboard, explorer, search, upload, monitoring, and deployment views
- **Multi-file semantic import** with import sessions, file classification, validation, merge preview, and conflict detection
- **Interactive import completion** — guided wizard for missing information, smart file suggestions, completeness scoring
- **Persistent service registry** — service lifecycle (draft/published/retired), version history, provider management, audit trail
- **Asset & digital twin inventory** — plant hierarchy, physical devices, digital twin registration and synchronization
- **Deployment management** — deploy, stop, restart, rollback with history and environment tracking
- **End-to-end execution workflow** — requirement → match → deploy → monitor → feedback
- **Project/workspace isolation** — multi-tenant separation of ontologies, services, and assets
- **Administration** — users, roles, organizations, permissions, API keys, settings
- **Knowledge graph visualization** — interactive Cytoscape.js graph with search, highlight, and dependency analysis
- **Multi-candidate matching** — ranked results with similarity scores, constraint violations, and rejection reasons
- **Change impact analysis** — service updates across requirements, deployments, and assets
- **Live runtime integration** — Docker Engine and Kubernetes pod monitoring, container health, logs
- **Service marketplace** — search, browse, categories, favorites, version comparison
- **Lifecycle workflow engine** — draft → review → approved → published → deprecated
- **Reporting** — architecture, matching, ontology, and deployment reports (PDF, Excel)

## Out of Scope

The project does not implement:

- A full industrial SDA platform covering every MTP and IEC standard
- Real PLC control logic
- Real production plant control
- Real OPC UA device communication beyond simulation
- Full AutomationML or PA-DIM parsing beyond the MTP AML scope
- Full Kubernetes deployment automation to a real cluster without manual confirmation
- Safety-critical control execution
- Real-time deterministic control guarantees
- Full SWRL reasoning engine beyond the OWL reasoner scope
- Redis, Kafka, queues, or cloud-native event streaming
- Real hardware integration or fieldbus communication beyond OPC UA simulation

## Success Criteria

The project is successful when:

- A requirement can be submitted through an API.
- The system can load ontology and catalogue data.
- MTP AML input files can be parsed into valid CSS-SDA triples.
- OPC UA Server Datasheet input files can be parsed into valid CSS-SDA triples.
- OWL reasoner can derive offered capabilities from parsed skill parameters.
- The system can run a SPARQL matching query across all CSS-SDA dimensions.
- The system can run cross-plant queries spanning multiple input sources.
- The system can select the correct service for a valid requirement.
- The system rejects services that fail capability, protocol, latency, recall, or deployment constraints.
- The system generates a Docker Compose manifest for the selected service.
- The system generates a Kubernetes deployment YAML with nodeSelector for the selected service.
- Tests verify positive and negative matching scenarios across all three use cases.
- The project can be run locally by another developer.
- **The system supports multi-file import sessions with validation, conflict detection, and merge preview.**
- **The system detects missing semantic information and guides users to complete it via wizard or additional file upload.**
- **Services can be registered with version history, lifecycle status, and provider metadata.**
- **Assets, devices, and digital twins can be managed through the platform UI.**
- **Services can be deployed, stopped, restarted, and rolled back with deployment history.**
- **The platform supports user roles, permissions, and multi-tenant workspace isolation.**
- **Semantic matches are ranked and explainable across all CSS-SDA dimensions.**
- **Change impact analysis traces service updates through requirements, deployments, and assets.**

## Example Expected Result

Input:

```json
{
  "requirementId": "REQ_001"
}
```

Output:

```json
{
  "matched": true,
  "service": "FlowmeterDiagnosticsService",
  "image": "registry.local/flowmeter-diagnostics:1.0",
  "capability": "AssetConditionMonitoringCapability",
  "protocol": "OPC-UA",
  "accessMode": "read-only",
  "connector": "FlowmeterFT101Connector",
  "deploymentContext": "OnPremPlantNetwork",
  "recall": 0.94,
  "latencyMs": 45,
  "kubernetesManifest": "apiVersion: apps/v1...",
  "dockerComposeManifest": "services:\n  flowmeter-diagnostics...",
  "matchingExplanation": "The service matches..."
}
```

Cross-plant query example — find all temperature services:

```sparql
SELECT ?service ?plant WHERE {
  ?service a sda:Service ;
           css:offersUseOf / css:hasParameter / sda:engineeringUnit "°C" .
  ?service sda:belongsToSite ?plant .
}
```

## Engineering Principles

The project follows these principles:

- Keep the first version small and executable.
- Prefer a working vertical slice over broad incomplete modeling.
- Separate ontology data from Java code.
- Keep matching rules visible and reviewable as SPARQL files.
- Make service selection explainable.
- Keep deployment generation simple and deterministic.
- Treat the prototype as a research and engineering demonstration, not a production SDA platform.
- **Add platform capabilities incrementally on top of the semantic core, never by replacing it.**
- **Leverage the existing database and REST API layer for registry, asset, and deployment persistence.**
- **Keep workspace isolation at the data level (ontologies, databases) rather than introducing complex infrastructure.**

## Primary Users

The primary users are:

- Researchers studying Software-Defined Automation
- Software engineers exploring semantic service discovery
- Automation engineers interested in requirement-to-service mapping
- Thesis reviewers evaluating a working prototype
- **Platform engineers evaluating the transition from semantic prototype to operational engineering platform**

## Long-Term Vision

The long-term vision is to extend the prototype into a fully operational semantic automation engineering platform that can support:

- Real OPC UA metadata ingestion (live endpoints instead of datasheet files)
- Full AutomationML and PA-DIM import beyond MTP AML
- SWRL or SHACL rule-based inference alongside OWL reasoning
- Kubernetes deployment execution and lifecycle management
- Runtime verification status updates
- Human-in-the-loop service approval
- Multi-team collaborative workspace environment
- Integration with CI/CD pipelines for service deployment
- Industrial edge deployment via Margo Sandbox and similar frameworks
- Marketplace ecosystem where vendors publish and version their automation services
- Cross-plant and cross-enterprise semantic discovery and orchestration
