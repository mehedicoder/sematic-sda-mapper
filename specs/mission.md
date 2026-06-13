# Mission: Semantic Requirement-to-Service Mapping for Software-Defined Automation

## Project Name

**Semantic SDA Mapper**

## Project Mission

The mission of this project is to design and implement a lightweight prototype that maps industrial automation requirements to suitable deployable software services using a small ontology, SPARQL-based semantic reasoning, and Kubernetes deployment manifest generation.

The project demonstrates a simplified Software-Defined Automation workflow where high-level automation requirements can be matched with available automation services based on capability, protocol, data connector, performance, and deployment constraints.

This prototype is inspired by the semantic chain used in Software-Defined Automation:

```text
UseCase
→ Requirement
→ Capability
→ Skill
→ Service
→ CommunicationBinding
→ DataConnector
→ DeploymentContext
```

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

## Target Use Case

The first use case is:

**Flowmeter Asset-Health Monitoring**

Example requirement:

```text
Monitor the health of flowmeter FT-101.
The selected service must provide asset condition monitoring,
use OPC UA in read-only mode,
achieve recall >= 0.90,
have latency <= 60 ms,
and run in an on-premise plant-network deployment context.
```

## Core Objective

The core objective is to build a working prototype that can:

1. Load a small ontology written in RDF/Turtle.
2. Represent automation requirements, capabilities, skills, services, bindings, connectors, and deployment contexts.
3. Run SPARQL queries to find a service that satisfies a given requirement.
4. Return the selected service with an explanation.
5. Generate a Kubernetes deployment manifest for the selected service.
6. Provide tests that prove correct matching and rejection behavior.

## In Scope

The project includes:

- A small CSS-SDA-inspired ontology
- RDF/Turtle files for requirements and service catalogue data
- SPARQL queries for semantic matching
- Java 25 backend implementation
- Spring Boot 4.0.x REST API
- Apache Jena-based RDF and SPARQL processing
- Kubernetes YAML generation
- Unit and integration tests
- Docker Compose for local development
- Documentation for usage and evaluation

## Out of Scope

The project does not implement:

- A full industrial SDA platform
- Real PLC control logic
- Real production plant control
- Real OPC UA device communication in the first version
- Full MTP, NOA, PA-DIM, AAS, or AutomationML parsing
- Full Kubernetes deployment automation to a real cluster
- Safety-critical control execution
- Real-time deterministic control guarantees
- Full SWRL reasoning engine in the MVP

## Success Criteria

The project is successful when:

- A requirement can be submitted through an API.
- The system can load ontology and catalogue data.
- The system can run a SPARQL matching query.
- The system can select the correct service for a valid requirement.
- The system rejects services that fail capability, protocol, latency, recall, or deployment constraints.
- The system generates a Kubernetes deployment YAML for the selected service.
- Tests verify positive and negative matching scenarios.
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
  "kubernetesManifest": "apiVersion: apps/v1..."
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

- Multiple industrial use cases
- More detailed ontology modeling
- Real OPC UA metadata ingestion
- MTP or AAS import
- SHACL validation
- Kubernetes deployment execution
- Runtime verification status updates
- Human-in-the-loop service approval
