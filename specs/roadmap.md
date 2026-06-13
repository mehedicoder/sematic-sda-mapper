# Roadmap: Semantic SDA Mapper

## Roadmap Principle

The project will be implemented as a sequence of small vertical slices.

Each phase should produce something executable, testable, and reviewable.

The first goal is not to build a full Software-Defined Automation platform. The first goal is to prove the semantic mapping path:

```text
Requirement
→ Capability
→ Skill
→ Service
→ CommunicationBinding
→ DataConnector
→ DeploymentContext
→ Kubernetes Manifest
```

## Phase 1: Project Foundation

### Goal

Create a runnable Java 25 and Spring Boot 4.0.x project with basic structure, testing, and local development setup.

### Tasks

- [ ] Create Maven-based Spring Boot 4.0.x project
- [ ] Configure Java 25
- [ ] Add Spring Web dependency
- [ ] Add Spring Validation dependency
- [ ] Add Spring Actuator dependency
- [ ] Add Apache Jena dependency
- [ ] Add SnakeYAML dependency
- [ ] Add JUnit 5 and AssertJ test support
- [ ] Create base package structure
- [ ] Add health endpoint through Spring Actuator
- [ ] Add Dockerfile
- [ ] Add docker-compose.yml
- [ ] Add README with local run instructions

### Acceptance Criteria

- [ ] `mvn clean test` passes
- [ ] Application starts locally
- [ ] `/actuator/health` returns `UP`
- [ ] Docker image can be built
- [ ] Application can be started through Docker Compose

## Phase 2: Minimal Ontology Model

### Goal

Create a small RDF/Turtle ontology that represents the core semantic entities needed for requirement-to-service mapping.

### Tasks

- [ ] Create `src/main/resources/ontology/css-sda-small.ttl`
- [ ] Define class `UseCase`
- [ ] Define class `Requirement`
- [ ] Define class `Capability`
- [ ] Define class `Skill`
- [ ] Define class `Service`
- [ ] Define class `CommunicationBinding`
- [ ] Define class `DataConnector`
- [ ] Define class `DeploymentContext`
- [ ] Define object property `mapsToCapability`
- [ ] Define object property `realisedBy`
- [ ] Define object property `isImplementedBy`
- [ ] Define object property `usesBinding`
- [ ] Define object property `usesConnector`
- [ ] Define object property `isDeployedBy`
- [ ] Define data properties for protocol, access mode, recall, latency, image, and on-premise deployment
- [ ] Add example requirement `REQ_001`
- [ ] Add example service `FlowmeterDiagnosticsService`
- [ ] Add negative candidate services for testing

### Acceptance Criteria

- [ ] Ontology file is valid Turtle
- [ ] Ontology contains all MVP classes
- [ ] Ontology contains one valid matching service
- [ ] Ontology contains at least three non-matching services
- [ ] Ontology can be loaded by Apache Jena in a test

## Phase 3: Ontology Loading Service

### Goal

Implement Java code that loads the ontology model into Apache Jena.

### Tasks

- [ ] Create `OntologyModelLoader`
- [ ] Load ontology file from classpath
- [ ] Return Apache Jena `Model`
- [ ] Add clear error when ontology file is missing
- [ ] Add clear error when ontology file is invalid
- [ ] Add unit test for successful ontology loading
- [ ] Add unit test for missing ontology file
- [ ] Add unit test for invalid ontology content

### Acceptance Criteria

- [ ] Ontology loads successfully during application startup or first use
- [ ] Invalid ontology input fails with a clear exception
- [ ] Tests cover success and failure cases

## Phase 4: SPARQL Matching Query

### Goal

Create the first SPARQL query that selects a service satisfying a requirement.

### Tasks

- [ ] Create `src/main/resources/queries/match-service.rq`
- [ ] Match requirement to capability
- [ ] Match capability to skill
- [ ] Match skill to service
- [ ] Match required protocol
- [ ] Match required access mode
- [ ] Match data connector
- [ ] Match deployment context
- [ ] Filter by minimum recall
- [ ] Filter by maximum latency
- [ ] Order candidates by highest recall
- [ ] Limit result to one best service
- [ ] Add query test with Apache Jena

### Acceptance Criteria

- [ ] Query returns `FlowmeterDiagnosticsService` for `REQ_001`
- [ ] Query excludes service with wrong capability
- [ ] Query excludes service with wrong protocol
- [ ] Query excludes service with excessive latency
- [ ] Query excludes service with incompatible deployment context

## Phase 5: Semantic Reasoner Service

### Goal

Implement the application service that runs the SPARQL matching query and returns a Java result object.

### Tasks

- [ ] Create `SemanticReasonerService`
- [ ] Create `SparqlQueryLoader`
- [ ] Create `ServiceMatchResult`
- [ ] Execute SPARQL query against ontology model
- [ ] Support matching by `requirementId`
- [ ] Extract selected service name
- [ ] Extract container image
- [ ] Extract capability
- [ ] Extract protocol
- [ ] Extract access mode
- [ ] Extract connector
- [ ] Extract deployment context
- [ ] Extract recall and latency values
- [ ] Return no-match result when query has no result
- [ ] Add unit tests for match and no-match scenarios

### Acceptance Criteria

- [ ] Reasoner returns a complete result for `REQ_001`
- [ ] Reasoner returns no-match for unsatisfied requirements
- [ ] Reasoner does not hardcode service selection in Java
- [ ] Matching behavior is controlled by ontology data and SPARQL rules

## Phase 6: REST API

### Goal

Expose requirement matching through a REST endpoint.

### Tasks

- [ ] Create `RequirementMatchingController`
- [ ] Create `MatchRequirementRequest`
- [ ] Create `MatchRequirementResponse`
- [ ] Add request validation
- [ ] Add endpoint `POST /api/v1/requirements/match`
- [ ] Return matched service details
- [ ] Return no-match response when no service satisfies the requirement
- [ ] Add global exception handling
- [ ] Add integration tests with MockMvc or WebTestClient

### Acceptance Criteria

- [ ] API accepts a requirement ID
- [ ] API returns selected service for valid matching requirement
- [ ] API returns no-match response for unsatisfied requirement
- [ ] API returns validation error for blank requirement ID
- [ ] Integration tests pass

## Phase 7: Kubernetes Manifest Generation

### Goal

Generate a Kubernetes deployment manifest for the selected service.

### Tasks

- [ ] Create `KubernetesManifestService`
- [ ] Create `KubernetesManifestResult`
- [ ] Generate deployment name from selected service
- [ ] Add container image from selected service
- [ ] Add environment variable for asset ID
- [ ] Add environment variable for process variable
- [ ] Add environment variable for protocol
- [ ] Add environment variable for deployment context
- [ ] Add generated YAML to API response
- [ ] Add unit tests for generated YAML
- [ ] Add example output under `k8s/examples/`

### Acceptance Criteria

- [ ] Matching API response includes Kubernetes YAML
- [ ] YAML contains selected service name
- [ ] YAML contains selected image
- [ ] YAML contains connector metadata
- [ ] YAML can be parsed as valid YAML
- [ ] Tests verify manifest generation

## Phase 8: Matching Explanation

### Goal

Make the result explainable so a user can understand why a service was selected.

### Tasks

- [ ] Add `matchingExplanation` field to API response
- [ ] Include capability match reason
- [ ] Include protocol match reason
- [ ] Include access mode match reason
- [ ] Include latency constraint reason
- [ ] Include recall constraint reason
- [ ] Include deployment context reason
- [ ] Add explanation for no-match cases where possible
- [ ] Add tests for explanation content

### Acceptance Criteria

- [ ] Successful match response explains why the service was selected
- [ ] No-match response explains that constraints were not satisfied
- [ ] Explanation is understandable without reading the SPARQL query

## Phase 9: Evaluation Dataset

### Goal

Create several requirement and service examples to evaluate the prototype.

### Tasks

- [ ] Add requirement that matches flowmeter diagnostics
- [ ] Add requirement with wrong protocol
- [ ] Add requirement with strict latency
- [ ] Add requirement with high recall threshold
- [ ] Add requirement allowing cloud deployment
- [ ] Add service with correct capability but wrong protocol
- [ ] Add service with correct capability but too much latency
- [ ] Add service with correct capability but cloud-only deployment
- [ ] Add service with wrong capability
- [ ] Document expected matching results

### Acceptance Criteria

- [ ] Evaluation dataset contains at least five requirements
- [ ] Evaluation dataset contains at least five services
- [ ] Test cases validate expected results
- [ ] README documents evaluation scenarios

## Phase 10: Documentation and Thesis Support

### Goal

Document the prototype clearly for academic and engineering evaluation.

### Tasks

- [ ] Write project overview
- [ ] Document architecture
- [ ] Document ontology classes
- [ ] Document SPARQL matching logic
- [ ] Document REST API
- [ ] Document Kubernetes manifest generation
- [ ] Document evaluation cases
- [ ] Add limitations section
- [ ] Add future work section

### Acceptance Criteria

- [ ] README explains how to run the project
- [ ] Documentation explains the semantic mapping chain
- [ ] Documentation includes example request and response
- [ ] Documentation includes example Kubernetes manifest
- [ ] Documentation clearly states limitations

## Optional Phase 11: Runtime Verification Status

### Goal

Add a simple lifecycle status update after manifest generation.

### Tasks

- [ ] Add lifecycle status property to requirement
- [ ] Mark requirement as `Matched` after service selection
- [ ] Mark requirement as `DeploymentManifestGenerated` after YAML generation
- [ ] Add endpoint to read requirement status
- [ ] Add tests for status changes

### Acceptance Criteria

- [ ] Requirement status changes after successful match
- [ ] Requirement status changes after manifest generation
- [ ] Status can be queried through API

## Optional Phase 12: Simulated OPC UA Metadata

### Goal

Add simulated plant metadata to make the example closer to industrial automation.

### Tasks

- [ ] Add simulated OPC UA endpoint metadata
- [ ] Add asset `FT-101`
- [ ] Add process variable `flow-rate`
- [ ] Add unit metadata
- [ ] Add connector mapping to service result
- [ ] Add tests for connector metadata extraction

### Acceptance Criteria

- [ ] Service match includes asset ID
- [ ] Service match includes process variable
- [ ] Service match includes unit
- [ ] Generated Kubernetes YAML includes connector information

## MVP Definition

The minimum viable prototype is complete when the following phases are done:

```text
Phase 1: Project Foundation
Phase 2: Minimal Ontology Model
Phase 3: Ontology Loading Service
Phase 4: SPARQL Matching Query
Phase 5: Semantic Reasoner Service
Phase 6: REST API
Phase 7: Kubernetes Manifest Generation
```

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
The service matches the required asset condition monitoring capability,
uses OPC UA in read-only mode,
satisfies recall and latency constraints,
and can run in the on-premise plant network.
```

5. Show generated Kubernetes deployment YAML.

## Final Deliverables

The final project should deliver:

- Working Java 25 and Spring Boot 4.0.x application
- Small CSS-SDA-inspired ontology
- SPARQL matching query
- Requirement matching REST API
- Kubernetes deployment YAML generation
- Unit and integration tests
- Docker-based local setup
- Documentation for academic evaluation
- Evaluation scenarios and results
