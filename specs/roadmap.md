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

Create a runnable Java 25 and Spring Boot 4.0.x project with basic structure, testing, local development setup, and automation pipeline foundation.

**Tasks**

* [ ] Create Maven-based Spring Boot 4.0.x project
* [ ] Configure Java 25
* [ ] Add Spring Web dependency
* [ ] Add Spring Validation dependency
* [ ] Add Spring Actuator dependency
* [ ] Add Apache Jena dependency
* [ ] Add SnakeYAML dependency
* [ ] Add JUnit 5 and AssertJ test support
* [ ] Create base package structure
* [ ] Add health endpoint through Spring Actuator
* [ ] Add Dockerfile
* [ ] Add docker-compose.yml
* [ ] Add README with local run instructions
* [ ] Add OPC UA server service to `docker-compose.yml`
* [ ] Configure OPC UA server endpoint and security settings
* [ ] Add OPC UA server health check
* [ ] Add network configuration for service-to-server communication
* [ ] Add integration test that verifies OPC UA server availability
* [ ] Add Node-RED service to `docker-compose.yml`
 * [ ] Install OpcUa-Server node in Node-RED
 * [ ] Mount `docs/opc-us.json` as a volume in the Node-RED container so the flow loads automatically on start
 * [ ] Use `docs/opc-us.json` flow to simulate OPC UA server

**Acceptance Criteria**

* [ ] `mvn clean test` passes
* [ ] Application starts locally
* [ ] `/actuator/health` returns `UP`
* [ ] Docker image can be built
* [ ] Application can be started through Docker Compose
* [ ] OPC UA server starts via `docker compose up`
* [ ] OPC UA server is reachable from the application container
* [ ] OPC UA server health endpoint is available

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

**Acceptance Criteria**

* [ ] Ontology file is valid Turtle
* [ ] Ontology contains all CSS-SDA dimensions
* [ ] Ontology supports MTPModule and FieldDevice physical asset types
* [ ] Ontology contains CommunicationBinding with full security properties (mode, policy, level, discovery)
* [ ] Ontology contains DeploymentContext with SIL, determinism class, cycle time, and runtime
* [ ] Ontology contains OfferedCapability and RequiredCapability for match inference
* [ ] Ontology can be loaded by Apache Jena in a test
* [ ] Ontology contains at least three example devices across two use cases

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

**Acceptance Criteria**

* [ ] Ontology loads successfully during application startup or first use
* [ ] Invalid ontology input fails with a clear exception
* [ ] Tests cover success and failure cases

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

**Acceptance Criteria**

* [ ] MTP parser generates valid CSS-SDA triples from a real AML source
* [ ] OPC UA Datasheet parser generates valid CSS-SDA triples from a real datasheet source
* [ ] Parsers produce `CommunicationBinding` triples with endpoint, security mode, and security policy
* [ ] MTP parser produces `css:Skill` + `css:SkillParameter` with UCUM units and IRDI
* [ ] Datasheet parser produces `CommunicationBinding` with securityLevel, transportProfile, discoveryUrl
* [ ] Both parsers fail with clear errors on invalid input
* [ ] Parsed triples can be loaded into the same Apache Jena Model as the ontology schema

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

**Acceptance Criteria**

* [ ] Query returns `FlowmeterDiagnosticsService` for `REQ_001`
* [ ] Query excludes service with wrong capability (functionClass mismatch)
* [ ] Query excludes service with wrong protocol
* [ ] Query excludes service with excessive latency
* [ ] Query excludes service with incompatible deployment context
* [ ] Query evaluates CommunicationBinding, DataConnector, and DeploymentContext simultaneously

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

**Acceptance Criteria**

* [ ] Inferred `OfferedCapability` instance has correct `functionClass`, `engineeringUnit`, and `nominalRange`
* [ ] Inference preserves existing explicit capability triples
* [ ] Changing SkillParameter input changes inferred capability output
* [ ] Inference completes within acceptable time for the MVP dataset
* [ ] Reasoner can be enabled/disabled for testing without inference

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

**Acceptance Criteria**

* [ ] Reasoner returns a complete result for `REQ_001`
* [ ] Reasoner returns no-match for unsatisfied requirements
* [ ] Reasoner does not hardcode service selection in Java
* [ ] Matching behavior is controlled by ontology data and SPARQL rules
* [ ] Tests prove that changing ontology data changes the selected service without Java code changes

---

## Phase 8: REST API

**Goal**

Expose requirement matching through a REST endpoint.

**Tasks**

* [ ] Create `RequirementMatchingController`
* [ ] Create `MatchRequirementRequest`
* [ ] Create `MatchRequirementResponse`
* [ ] Add request validation
* [ ] Add endpoint `POST /api/v1/requirements/match`
* [ ] Return matched service details
* [ ] Return no-match response when no service satisfies the requirement
* [ ] Add global exception handling
* [ ] Add integration tests with MockMvc or WebTestClient

**Acceptance Criteria**

* [ ] API accepts a requirement ID
* [ ] API returns selected service for valid matching requirement
* [ ] API returns no-match response for unsatisfied requirement
* [ ] API returns validation error for blank requirement ID
* [ ] Integration tests pass

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

**Acceptance Criteria**

* [ ] Matching API response includes valid Docker Compose YAML
* [ ] Matching API response includes valid Kubernetes YAML
* [ ] YAML contains selected service name and specific target image
* [ ] YAML includes injected operational runtime metadata from CommunicationBinding and DeploymentContext
* [ ] Kubernetes YAML contains `nodeSelector` with `sda.ontology/sil`, `sda.ontology/deterministic`, `sda.ontology/zone`
* [ ] YAML can be parsed successfully using SnakeYAML
* [ ] Tests verify runtime property mappings

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

**Acceptance Criteria**

* [ ] Successful match response explains why the service was selected
* [ ] No-match response explains that constraints were not satisfied
* [ ] Explanation is understandable without reading the SPARQL query
* [ ] Tests verify explanation content for successful and failed matches

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
* [ ] Document optional Margo Sandbox integration
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

## Phase 13: EKA Enterprise User Interface

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
* [ ] Document optional Margo Sandbox integration
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

## Optional Phase 14: Margo Sandbox Deployment Context

**Goal**

Use Margo Sandbox as an optional industrial edge deployment/interoperability context. This phase must not replace the semantic reasoning work. It only validates that the selected service or generated deployment artifact can fit into an edge deployment workflow.

**Tasks**

* [ ] Study the Margo Sandbox application deployment workflow
* [ ] Define how the selected service maps to a Margo-style application package
* [ ] Add a simple Margo deployment descriptor or package metadata if required
* [ ] Document limitations of the Margo Sandbox integration
* [ ] Keep Margo integration optional and separate from the MVP

**Acceptance Criteria**

* [ ] Margo integration does not replace Apache Jena or SPARQL
* [ ] Margo integration is documented as deployment-context validation only
* [ ] The core MVP still works without Margo

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
Phase 13: EKA Enterprise User Interface
```

Margo Sandbox integration is optional (Optional Phase 14). Runtime Verification Status is optional (Optional Phase 15). Dagger automation is optional (Optional Phase 16).

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
* Unit and integration tests
* Optional Dagger automation pipeline
* Evaluation dataset with fermentation skid and HVAC plant examples + cross-plant queries
* Documentation for academic evaluation
* Optional Margo Sandbox deployment-context extension
* EKA Enterprise User Interface (Vaadin 25 with dashboard, explorer, search, registration wizard, ontology graph, monitoring, upload center, settings, and light/dark mode)
