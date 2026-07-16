# Mission: Semantic Requirement-to-Service Mapping for Software-Defined Automation

## Project Name

**Semantic SDA Mapper**

## Project Mission

The mission of this project is to design and implement a lightweight prototype that maps industrial automation requirements to suitable deployable software services using a small ontology, SPARQL-based semantic reasoning, and Kubernetes deployment manifest generation.

The project demonstrates a simplified Software-Defined Automation workflow where high-level automation requirements can be matched with available automation services based on capability, protocol, data connector, performance, and deployment constraints.

This prototype demonstrates the full five-step semantic mapping procedure used in Software-Defined Automation:

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

## Problem Statement

In traditional industrial automation, mapping an automation requirement to a suitable service or device is often manual, vendor-specific, and difficult to reuse.

For example, if an engineer needs to monitor the health of a flowmeter, they must manually identify:

- Which service can perform asset-health monitoring
- Which process variables are required
- Which communication protocol is supported
- Whether the service can access the required asset data
- Whether the service satisfies latency, accuracy, and deployment constraints
- How the selected service should be deployed

This project aims to show how semantic modeling can make this process more machine-understandable and partially automatable.

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

The core objective is to implement and validate the three target use cases end-to-end:

1. **Use Case A (NE 107 Flowmeter):** Parse OPC UA Server Datasheet → CSS-SDA triples → SPARQL match → select FlowmeterDiagnosticsService → reject when NE 107 status = Failure → generate deployment manifest.
2. **Use Case B (Fermentation Skid):** Parse MTP AML → CSS-SDA triples → OWL inference derives TempControl/Dosing capabilities → SPARQL match → select FermentationSkidService → reject when SIL or determinism mismatch.
3. **Use Case C (HVAC Plant):** Parse OPC UA Server Datasheet → CSS-SDA triples → SPARQL match → select HVACTempAnalysisService → cross-plant query across PlantA and PlantB → reject when networkZone mismatch.

Each use case follows the paper's five-step procedure:

```text
Input Files (MTP AML, OPC UA Server Datasheet, AAS)
→ Parse (Phase 4)
→ Instantiate (CSS-SDA ontology ABox triples — Phase 2)
→ Lift (MTP Service → css:Skill — Phase 4)
→ Infer (OWL reasoner derives OfferedCapability — Phase 6)
→ Match (SPARQL multi-dimensional query — Phase 5)
→ Docker Compose / Kubernetes Manifest (Phase 9)
```

## Roadmap Phase Mapping

The 11 roadmap phases map to the 3 use cases as follows:

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

**All 11 phases are required to deliver all 3 use cases.**

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

## Out of Scope

The project does not implement:

- A full industrial SDA platform
- Real PLC control logic
- Real production plant control
- Real OPC UA device communication in the first version
- Full AutomationML or PA-DIM parsing beyond the MTP AML scope
- Full Kubernetes deployment automation to a real cluster
- Safety-critical control execution
- Real-time deterministic control guarantees
- Full SWRL reasoning engine beyond the OWL reasoner scope
- Full lifecycle management beyond requirement mapping and manifest generation

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

## Primary Users

The primary users are:

- Researchers studying Software-Defined Automation
- Software engineers exploring semantic service discovery
- Automation engineers interested in requirement-to-service mapping
- Thesis reviewers evaluating a working prototype

## Long-Term Vision

The long-term vision is to extend the prototype into a richer semantic automation engineering tool that can support:

- Additional industrial use cases beyond the three evaluated
- More detailed ontology modeling with SHACL constraints
- Real OPC UA metadata ingestion (live endpoints instead of datasheet files)
- Full AutomationML and PA-DIM import beyond MTP AML
- SWRL or SHACL rule-based inference alongside OWL reasoning
- Kubernetes deployment execution and lifecycle management
- Runtime verification status updates
- Human-in-the-loop service approval

---

## EKA Enterprise User Interface

**Objective**

Build a modern, responsive, enterprise-grade user interface (EKA UI) on top of the CSS-SDA semantic core. The UI replaces the pure REST-API interaction with a clean, intuitive, visually appealing dashboard that enables users to explore semantic models, register services, search ontology assets, upload artifacts, monitor the platform, and visualize semantic relationships.

**Design Principles**

- Modern and lightweight, minimal visual clutter
- Card-based layouts instead of large forms
- Responsive — desktop, tablet, and mobile
- Support for light and dark themes
- Progressive disclosure and semantic navigation
- Enterprise UX inspired by Siemens Industrial Edge, SAP Fiori, and Azure Portal

**Core Pages**

| Page | Description |
|---|---|
| Dashboard | Animated cards showing registered services, capabilities, skills, mappings, digital twins, deployments, simulations, and live OPC UA sessions |
| Semantic Explorer | Split layout with ontology tree (left) and entity details with tabs for Overview, Properties, Relationships, Services, History, Mappings (right) |
| Global Search | AI-style semantic search bar with autocomplete, fuzzy, exact, and ontology-scoped search |
| Upload Center | Drag-and-drop panel for OPC UA NodeSet, AAS, MTP, AutomationML, RDF, OWL, JSON-LD, YAML |
| Service Registration Wizard | Multi-step wizard: General → Communication → Capabilities → Skills → Deployment → Semantic Mapping → Review → Register |
| Ontology Visualization | Interactive Cytoscape.js graph with zoom, pan, expand/collapse, highlight paths, search |
| Monitoring Dashboard | Real-time widgets for CPU, memory, OPC UA sessions, semantic queries, connected assets |
| Deployment View | Cards per environment (Dev, Test, Prod, Edge, Cloud) showing status, services, assets, health |
| Settings | Theme, language, time zone, notifications, accessibility, search preferences |

**Technology**

Built with Vaadin 25, Spring Security, PostgreSQL, Cytoscape.js, and ApexCharts — reusing the existing Java 25 and Spring Boot 4.0.x backend.

**Delivery Approach**

UI views are built incrementally alongside their backend dependencies across phases 1–13, not as a monolithic phase at the end. Each phase from 1 onward produces a visible UI slice alongside its backend service. Phase 13 completes the remaining cross-cutting views (ontology graph, settings), accessibility, mobile QA, and integration tests.

The REST API and all semantic core functionality remain fully usable without the UI.
