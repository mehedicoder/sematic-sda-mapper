Roadmap: Semantic SDA Mapper
Roadmap Principle
The project will be implemented as a sequence of small vertical slices. Each phase should produce something executable, testable, and reviewable.
The primary goal is to prove this semantic mapping path:
```text
Requirement
→ Capability
→ Skill
→ Service
→ CommunicationBinding
→ DataConnector
→ DeploymentContext
→ Docker Compose Manifest
```
Apache Jena and SPARQL are part of the semantic reasoning layer. Dagger is not a replacement for them. Dagger is used as an automation layer for build, test, packaging, validation, and deployment workflow.
```text
Apache Jena + SPARQL = semantic matching core
Dagger = automation and deployment pipeline
Docker Compose = generated deployment output
```
---
Phase 1: Project Foundation
Goal
Create a runnable Java 25 and Spring Boot 4.0.x project with basic structure, testing, local development setup, and automation pipeline foundation.
Tasks
[ ] Create Maven-based Spring Boot 4.0.x project
[ ] Configure Java 25
[ ] Add Spring Web dependency
[ ] Add Spring Validation dependency
[ ] Add Spring Actuator dependency
[ ] Add Apache Jena dependency
[ ] Add SnakeYAML dependency
[ ] Add JUnit 5 and AssertJ test support
[ ] Create base package structure
[ ] Add health endpoint through Spring Actuator
[ ] Add Dockerfile
[ ] Add docker-compose.yml
[ ] Add `dagger.json`
[ ] Add basic Dagger pipeline using Java SDK
[ ] Add Dagger function for running tests
[ ] Add Dagger function for building the application
[ ] Add Dagger function for building the Docker image
[ ] Add README with local run instructions
Acceptance Criteria
[ ] `mvn clean test` passes
[ ] Application starts locally
[ ] `/actuator/health` returns `UP`
[ ] Docker image can be built
[ ] Application can be started through Docker Compose
[ ] Dagger pipeline can run tests
[ ] Dagger pipeline can build the application
[ ] Dagger pipeline can build the Docker image
---
Phase 2: Minimal Ontology Model
Goal
Create a small RDF/Turtle ontology that represents the core semantic entities needed for requirement-to-service mapping.
Tasks
[ ] Create `src/main/resources/ontology/css-sda-small.ttl`
[ ] Define class `UseCase`
[ ] Define class `Requirement`
[ ] Define class `Capability`
[ ] Define class `Skill`
[ ] Define class `Service`
[ ] Define class `CommunicationBinding`
[ ] Define class `DataConnector`
[ ] Define class `DeploymentContext`
[ ] Define object property `mapsToCapability`
[ ] Define object property `realisedBy`
[ ] Define object property `isImplementedBy`
[ ] Define object property `usesBinding`
[ ] Define object property `usesConnector`
[ ] Define object property `isDeployedBy`
[ ] Define data properties for protocol, access mode, recall, latency, image, and on-premise deployment
[ ] Add example requirement `REQ_001`
[ ] Add example service `FlowmeterDiagnosticsService`
[ ] Add negative candidate services for testing
Acceptance Criteria
[ ] Ontology file is valid Turtle
[ ] Ontology contains all MVP classes
[ ] Ontology contains one valid matching service
[ ] Ontology contains at least three non-matching services
[ ] Ontology can be loaded by Apache Jena in a test
---
Phase 3: Ontology Loading with Apache Jena
Goal
Implement Java code that loads the ontology model into Apache Jena.
Tasks
[ ] Create `OntologyModelLoader`
[ ] Load ontology file from classpath
[ ] Return Apache Jena `Model`
[ ] Add clear error when ontology file is missing
[ ] Add clear error when ontology file is invalid
[ ] Add unit test for successful ontology loading
[ ] Add unit test for missing ontology file
[ ] Add unit test for invalid ontology content
Acceptance Criteria
[ ] Ontology loads successfully during application startup or first use
[ ] Invalid ontology input fails with a clear exception
[ ] Tests cover success and failure cases
[ ] Dagger pipeline can run ontology-loading tests
---
Phase 4: SPARQL Matching Query
Goal
Create the first SPARQL query that selects a service satisfying a requirement.
Tasks
[ ] Create `src/main/resources/queries/match-service.rq`
[ ] Match requirement to capability
[ ] Match capability to skill
[ ] Match skill to service
[ ] Match required protocol
[ ] Match required access mode
[ ] Match data connector
[ ] Match deployment context
[ ] Filter by minimum recall
[ ] Filter by maximum latency
[ ] Order candidates by highest recall
[ ] Limit result to one best service
[ ] Add query test with Apache Jena
Acceptance Criteria
[ ] Query returns `FlowmeterDiagnosticsService` for `REQ_001`
[ ] Query excludes service with wrong capability
[ ] Query excludes service with wrong protocol
[ ] Query excludes service with excessive latency
[ ] Query excludes service with incompatible deployment context
[ ] Dagger pipeline can run SPARQL query tests
---
Phase 5: Semantic Reasoner Service
Goal
Implement the application service that runs the SPARQL matching query and returns a Java result object.
Tasks
[ ] Create `SemanticReasonerService`
[ ] Create `SparqlQueryLoader`
[ ] Create `ServiceMatchResult`
[ ] Execute SPARQL query against ontology model
[ ] Support matching by `requirementId`
[ ] Extract selected service name
[ ] Extract container image
[ ] Extract capability
[ ] Extract protocol
[ ] Extract access mode
[ ] Extract connector
[ ] Extract deployment context
[ ] Extract recall and latency values
[ ] Return no-match result when query has no result
[ ] Add unit tests for match and no-match scenarios
Acceptance Criteria
[ ] Reasoner returns a complete result for `REQ_001`
[ ] Reasoner returns no-match for unsatisfied requirements
[ ] Reasoner does not hardcode service selection in Java
[ ] Matching behavior is controlled by ontology data and SPARQL rules
[ ] Tests prove that changing ontology data changes the selected service without Java code changes
---
Phase 6: REST API
Goal
Expose requirement matching through a REST endpoint.
Tasks
[ ] Create `RequirementMatchingController`
[ ] Create `MatchRequirementRequest`
[ ] Create `MatchRequirementResponse`
[ ] Add request validation
[ ] Add endpoint `POST /api/v1/requirements/match`
[ ] Return matched service details
[ ] Return no-match response when no service satisfies the requirement
[ ] Add global exception handling
[ ] Add integration tests with MockMvc or WebTestClient
Acceptance Criteria
[ ] API accepts a requirement ID
[ ] API returns selected service for valid matching requirement
[ ] API returns no-match response for unsatisfied requirement
[ ] API returns validation error for blank requirement ID
[ ] Integration tests pass
[ ] Dagger pipeline can run API integration tests
---
Phase 7: Docker Compose Manifest Generation
Goal
Generate a standalone Docker Compose deployment manifest for the selected service.
Tasks
[ ] Create `DockerComposeManifestService`
[ ] Create `DockerComposeManifestResult`
[ ] Generate container service name from selected service
[ ] Add container image configuration from selected service
[ ] Add `restart: unless-stopped` baseline edge policy configuration
[ ] Add environment parameters block for asset ID, process variable, protocol, and deployment context
[ ] Configure `network_mode: host` only if justified for the edge/fieldbus scenario
[ ] Add generated YAML syntax directly to the API response object
[ ] Add unit tests using SnakeYAML to parse and verify the generated output string
[ ] Add example file output under `docker/examples/`
Acceptance Criteria
[ ] Matching API response includes valid Docker Compose YAML
[ ] YAML contains selected service name and specific target image
[ ] YAML includes injected operational runtime metadata
[ ] YAML can be parsed successfully using SnakeYAML
[ ] Tests verify runtime property mappings
[ ] Dagger pipeline can generate and validate the Docker Compose manifest
---
Phase 8: Matching Explanation
Goal
Make the result explainable so a user can understand why a service was selected.
Tasks
[ ] Add `matchingExplanation` field to API response
[ ] Include capability match reason
[ ] Include protocol match reason
[ ] Include access mode match reason
[ ] Include latency constraint reason
[ ] Include recall constraint reason
[ ] Include deployment context reason
[ ] Add explanation for no-match cases where possible
[ ] Add tests for explanation content
Acceptance Criteria
[ ] Successful match response explains why the service was selected
[ ] No-match response explains that constraints were not satisfied
[ ] Explanation is understandable without reading the SPARQL query
[ ] Tests verify explanation content for successful and failed matches
---
Phase 9: Evaluation Dataset
Goal
Create several requirement and service examples to evaluate the prototype.
Tasks
[ ] Add requirement that matches flowmeter diagnostics
[ ] Add requirement with wrong protocol
[ ] Add requirement with strict latency
[ ] Add requirement with high recall threshold
[ ] Add requirement allowing cloud deployment
[ ] Add service with correct capability but wrong protocol
[ ] Add service with correct capability but too much latency
[ ] Add service with correct capability but cloud-only deployment
[ ] Add service with wrong capability
[ ] Document expected matching results
Acceptance Criteria
[ ] Evaluation dataset contains at least five requirements
[ ] Evaluation dataset contains at least five services
[ ] Test cases validate expected results
[ ] README documents evaluation scenarios
[ ] Dagger pipeline can run the full evaluation test suite
---
Phase 10: Dagger Automation and Deployment Workflow
Goal
Use Dagger to automate build, test, image creation, manifest validation, and optional deployment/testing of the generated Docker Compose output.
Tasks
[ ] Add Dagger function `test`
[ ] Add Dagger function `buildJar`
[ ] Add Dagger function `buildImage`
[ ] Add Dagger function `validateOntology`
[ ] Add Dagger function `validateSparql`
[ ] Add Dagger function `generateComposeExample`
[ ] Add Dagger function `validateCompose`
[ ] Add Dagger function `runEndToEnd`
[ ] Start application in a container for API tests
[ ] Run requirement matching API test from Dagger
[ ] Validate generated Docker Compose YAML
[ ] Document local Dagger commands
Acceptance Criteria
[ ] Dagger can run the same workflow locally and in CI
[ ] Dagger test workflow fails if ontology loading fails
[ ] Dagger test workflow fails if SPARQL matching fails
[ ] Dagger test workflow fails if Docker Compose YAML is invalid
[ ] Dagger can build a runnable container image
[ ] Dagger can execute an end-to-end matching scenario
---
Phase 11: Optional Margo Sandbox Deployment Context
Goal
Use Margo Sandbox as an optional industrial edge deployment/interoperability context. This phase must not replace the semantic reasoning work. It only validates that the selected service or generated deployment artifact can fit into an edge deployment workflow.
Tasks
[ ] Study the Margo Sandbox application deployment workflow
[ ] Define how the selected service maps to a Margo-style application package
[ ] Add a simple Margo deployment descriptor or package metadata if required
[ ] Add Dagger function `packageForMargoSandbox`
[ ] Add Dagger function `deployToMargoSandbox` if local sandbox setup is available
[ ] Document limitations of the Margo Sandbox integration
[ ] Keep Margo integration optional and separate from the MVP
Acceptance Criteria
[ ] Margo integration does not replace Apache Jena or SPARQL
[ ] Margo integration is documented as deployment-context validation only
[ ] The core MVP still works without Margo
[ ] Dagger can prepare the deployment artifact for Margo Sandbox if configured
---
Phase 12: Documentation and Thesis Support
Goal
Document the prototype clearly for academic and engineering evaluation.
Tasks
[ ] Write project overview
[ ] Document architecture
[ ] Document ontology classes
[ ] Document SPARQL matching logic
[ ] Document REST API
[ ] Document Docker Compose manifest generation strategy
[ ] Document Dagger automation workflow
[ ] Document optional Margo Sandbox integration
[ ] Document evaluation cases
[ ] Add limitations section
[ ] Add future work section
Acceptance Criteria
[ ] README explains how to run the project
[ ] Documentation explains the semantic mapping chain
[ ] Documentation clearly explains the difference between Apache Jena, SPARQL, and Dagger
[ ] Documentation includes example request and response
[ ] Documentation includes example Docker Compose manifest output
[ ] Documentation clearly states limitations
---
Optional Phase 13: Runtime Verification Status
Goal
Add a simple lifecycle status update after manifest generation.
Tasks
[ ] Add lifecycle status property to requirement
[ ] Mark requirement as `Matched` after service selection
[ ] Mark requirement as `DeploymentManifestGenerated` after YAML generation
[ ] Add endpoint to read requirement status
[ ] Add tests for status changes
Acceptance Criteria
[ ] Requirement status changes after successful match
[ ] Requirement status changes after manifest generation
[ ] Status can be queried through API
---
MVP Definition
The minimum viable prototype is complete when the following phases are done:
```text
Phase 1: Project Foundation
Phase 2: Minimal Ontology Model
Phase 3: Ontology Loading with Apache Jena
Phase 4: SPARQL Matching Query
Phase 5: Semantic Reasoner Service
Phase 6: REST API
Phase 7: Docker Compose Manifest Generation
Phase 8: Matching Explanation
Phase 9: Evaluation Dataset
Phase 10: Dagger Automation and Deployment Workflow
```
Margo Sandbox integration is optional and should be treated as an extension, not as a requirement for the MVP.
---
MVP Demo Script
Start the application.
Send request:
```http
POST /api/v1/requirements/match
```
Body:
```json
{
  "requirementId": "REQ_001"
}
```
Show selected service:
```text
FlowmeterDiagnosticsService
```
Show explanation:
```text
The service matches the required asset condition monitoring capability, uses OPC UA in read-only mode, satisfies recall and latency constraints, and can run in the selected deployment context.
```
Show generated Docker Compose manifest.
Run Dagger workflow:
```bash
dagger call test
dagger call build-image
dagger call run-end-to-end
```
---
Final Deliverables
The final project should deliver:
Working Java 25 and Spring Boot 4.0.x application
Small CSS-SDA-inspired ontology
RDF/Turtle ontology file
SPARQL matching query
Apache Jena-based semantic reasoner
Requirement matching REST API
Docker Compose manifest generation
Dagger automation pipeline
Unit and integration tests
Evaluation dataset and expected results
Documentation for academic evaluation
Optional Margo Sandbox deployment-context extension
