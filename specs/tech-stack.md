# Tech Stack: Semantic SDA Mapper

## Runtime Platform

| Area | Technology |
|---|---|---|
| Programming language | Java 25 |
| Backend framework | Spring Boot 4.0.x |
| Build tool | Maven |
| API style | REST |
| RDF processing | Apache Jena |
| OWL reasoning | Apache Jena OWL_MEM_MICRO_RULES_INF |
| Semantic query language | SPARQL |
| Ontology format | RDF/Turtle |
| Input file format (MTP) | XML (AML Archive, VDI/VDE/NAMUR 2658) |
| Input file format (Datasheet) | JSON (AAS, IDTA 02009-1-0) |
| Deployment manifest formats | Docker Compose YAML + Kubernetes YAML |
| Testing | JUnit 5, AssertJ, Spring Boot Test |
| Local development | Docker Compose |
| Containerization | Docker |
| Deployment targets | Docker Compose edge/fieldbus + Kubernetes-compatible YAML |

## Java Version

The project uses:

```text
Java 25
```

Reason:

- Java 25 is suitable for a current research prototype.
- It aligns with the Spring Boot 4 generation.
- It allows the project to use modern Java language and runtime features.

## Spring Boot Version

The project uses:

```text
Spring Boot 4.0.x
```

Recommended starting version:

```text
Spring Boot 4.0.6 or latest available 4.0.x patch version
```

Reason:

- Spring Boot 4.0.x supports modern Java versions including Java 25.
- Spring Boot 4 is based on the Spring Framework 7 generation.
- It is suitable for new Java 25-based applications.
- It aligns well with modern Jakarta EE and cloud-native application development.

## Core Dependencies

### Spring Boot

Required starters:

```xml
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-actuator
spring-boot-starter-test
```

Purpose:

- `spring-boot-starter-web`: REST API
- `spring-boot-starter-validation`: request validation
- `spring-boot-starter-actuator`: health and observability endpoints
- `spring-boot-starter-test`: unit and integration testing

### Apache Jena

Required dependency:

```xml
org.apache.jena:apache-jena-libs
```

Purpose:

- Load RDF/Turtle ontology files
- Execute SPARQL queries
- Process semantic matching results
- Represent ontology individuals and relationships
- Run OWL reasoner for capability inference (OWL_MEM_MICRO_RULES_INF)

### XML Parsing (MTP AML)

Required dependency:

```xml
org.apache.jena:jena-core (bundled with apache-jena-libs)
```

MTP AML files are XML archives (VDI/VDE/NAMUR 2658). Parsing uses Java's built-in XML APIs (SAX/DOM) or a lightweight XML library assured by the Spring Boot BOM.

### JSON Parsing (OPC UA Server Datasheet)

Required dependency:

```xml
spring-boot-starter-json (included via spring-boot-starter-web)
```

OPC UA Server Datasheet files follow the IDTA 02009-1-0 AAS submodel JSON schema. Jackson (bundled with Spring Boot) handles parsing.

### YAML Support

Recommended dependency:

```xml
org.yaml:snakeyaml
```

Purpose:

- Generate Kubernetes deployment YAML
- Validate generated YAML structure in tests

## Suggested Maven Properties

```xml
<properties>
    <java.version>25</java.version>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

## Suggested Project Structure

```text
semantic-sda-mapper
│
├── pom.xml
├── README.md
├── specs/
│   ├── mission.md
│   ├── tech-stack.md
│   └── roadmap.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/semanticsda/
│   │   │       ├── SemanticSdaApplication.java
│   │   │       ├── api/
│   │   │       │   ├── RequirementMatchingController.java
│   │   │       │   └── dto/
│   │   │       │       ├── MatchRequirementRequest.java
│   │   │       │       └── MatchRequirementResponse.java
│   │   │       ├── parser/
│   │   │       │   ├── ParserFactory.java
│   │   │       │   ├── MTPAmlParser.java
│   │   │       │   └── UaServerDatasheetParser.java
│   │   │       ├── inference/
│   │   │       │   └── OntologyInferenceService.java
│   │   │       ├── reasoning/
│   │   │       │   ├── SemanticReasonerService.java
│   │   │       │   ├── SparqlQueryLoader.java
│   │   │       │   └── ServiceMatchResult.java
│   │   │       ├── ontology/
│   │   │       │   ├── OntologyModelLoader.java
│   │   │       │   └── OntologyResourceProperties.java
│   │   │       ├── deployment/
│   │   │       │   ├── DockerComposeManifestService.java
│   │   │       │   ├── KubernetesManifestService.java
│   │   │       │   └── ManifestResult.java
│   │   │       └── error/
│   │   │           ├── ApiErrorResponse.java
│   │   │           └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── ontology/
│   │       │   └── css-sda-small.ttl
│   │       ├── queries/
│   │       │   └── match-service.rq
│   │       └── k8s/
│   │           └── deployment-template.yaml
│   │
│   └── test/
│       ├── java/
│       │   └── com/example/semanticsda/
│       │       ├── parser/
│       │       │   ├── MTPAmlParserTest.java
│       │       │   └── UaServerDatasheetParserTest.java
│       │       ├── inference/
│       │       │   └── OntologyInferenceServiceTest.java
│       │       ├── reasoning/
│       │       │   └── SemanticReasonerServiceTest.java
│       │       ├── deployment/
│       │       │   ├── DockerComposeManifestServiceTest.java
│       │       │   └── KubernetesManifestServiceTest.java
│       │       └── api/
│       │           └── RequirementMatchingControllerIT.java
│       └── resources/
│           ├── ontology/
│           │   └── test-css-sda-small.ttl
│           ├── queries/
│           │   └── test-match-service.rq
│           └── examples/
│               ├── mtp-fermentation-skid.xml
│               └── uaserver-ahu01.json
│
├── docker/
│   ├── Dockerfile
│   └── examples/
│       └── flowmeter-diagnostics-compose.yaml
├── docker-compose.yml
└── k8s/
    └── examples/
        ├── flowmeter-diagnostics-deployment.yaml
        └── fermentation-skid-deployment.yaml
```

## Main Application Modules

### API Module

Package:

```text
com.example.semanticsda.api
```

Responsibilities:

- Expose REST endpoints
- Accept requirement matching requests
- Return matched service and deployment manifest
- Validate request input

Main endpoint:

```http
POST /api/v1/requirements/match
```

### Parser Module

Package:

```text
com.example.semanticsda.parser
```

Responsibilities:

- Parse MTP AML files (VDI/VDE/NAMUR 2658 XML) into CSS-SDA CommunicationBinding, DataConnector, and css:Skill triples
- Parse OPC UA Server Datasheet files (IDTA 02009-1-0 JSON) into CSS-SDA CommunicationBinding and DataConnector triples
- Select the correct parser via ParserFactory based on input file type
- Fail fast with clear errors when input files are missing, malformed, or incomplete

### Inference Module

Package:

```text
com.example.semanticsda.inference
```

Responsibilities:

- Configure and run OWL reasoner (Jena OWL_MEM_MICRO_RULES_INF) over the ontology model
- Derive `sda:OfferedCapability` instances from `css:SkillParameter` semantics (engineeringUnit, nominalRange, functionClass)
- Preserve existing explicit capability triples
- Provide enable/disable switch for testing without inference

### Ontology Module

Package:

```text
com.example.semanticsda.ontology
```

Responsibilities:

- Load RDF/Turtle files
- Build Apache Jena model
- Provide model to reasoning service
- Fail fast when ontology files are missing or invalid

### Reasoning Module

Package:

```text
com.example.semanticsda.reasoning
```

Responsibilities:

- Load SPARQL query files
- Execute semantic matching query
- Convert query result into Java DTO
- Handle no-match cases
- Keep matching logic outside Java business code as much as possible

### Deployment Module

Package:

```text
com.example.semanticsda.deployment
```

Responsibilities:

- Generate Docker Compose YAML for the selected service (edge/fieldbus scenarios)
- Generate Kubernetes deployment YAML for the selected service
- Derive Kubernetes `nodeSelector` constraints from `sda:DeploymentContext` triples (safetyIntegrityLevel, determinismClass, networkZone)
- Inject operational metadata from CommunicationBinding and DataConnector as environment variables
- Return generated manifests as text in the API response
- Validate generated YAML via SnakeYAML in tests

### Error Handling Module

Package:

```text
com.example.semanticsda.error
```

Responsibilities:

- Convert technical errors into clear API errors
- Handle invalid requirement IDs
- Handle no matching service
- Handle ontology loading failures
- Handle SPARQL query failures

## Data and Semantic Assets

### Ontology File

Location:

```text
src/main/resources/ontology/css-sda-small.ttl
```

Contains:

- Classes
- Object properties
- Data properties
- Example individuals
- Requirement examples
- Service catalogue examples

### SPARQL Query File

Location:

```text
src/main/resources/queries/match-service.rq
```

Contains:

- Requirement-to-service matching logic
- Capability match
- Binding match
- Connector match
- Deployment constraint match
- Performance filters

### Kubernetes Template

Location:

```text
src/main/resources/k8s/deployment-template.yaml
```

Contains:

- Deployment structure
- Container image placeholder
- Environment variable placeholders

## API Contract

### Match Requirement

Endpoint:

```http
POST /api/v1/requirements/match
```

Request:

```json
{
  "requirementId": "REQ_001"
}
```

Successful response:

```json
{
  "matched": true,
  "requirementId": "REQ_001",
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
  "dockerComposeManifest": "services:\n  flowmeter-diagnostics:\n    image: registry.local/flowmeter-diagnostics:1.0\n    restart: unless-stopped\n    environment:\n      ASSET_ID: FT-101\n      PROTOCOL: OPC-UA\n      DEPLOYMENT_ZONE: plant-network\n    network_mode: host",
  "matchingExplanation": "The service matches the required asset condition monitoring capability, uses OPC UA in read-only mode, satisfies recall (0.94 >= 0.90) and latency (45ms <= 60ms) constraints, and can run in the selected deployment context (on-premise plant-network)."
}
```

No-match response:

```json
{
  "matched": false,
  "requirementId": "REQ_001",
  "message": "No service satisfies the requirement constraints."
}
```

## Local Development

### Required Tools

```text
JDK 25
Maven 3.9+
Docker
Docker Compose
IntelliJ IDEA or equivalent Java IDE
```

### Run Locally

```bash
mvn clean test
mvn spring-boot:run
```

### Build Container

```bash
docker build -t semantic-sda-mapper:local -f docker/Dockerfile .
```

### Run with Docker Compose

```bash
docker compose up --build
```

## Testing Strategy

### Unit Tests

Focus areas:

- Ontology loading
- SPARQL query loading
- Input file parser tests (MTP AML valid, malformed, missing elements)
- Input file parser tests (OPC UA Server Datasheet valid, malformed, missing elements)
- OWL inference tests (correct capability derivation, parameter sensitivity)
- Reasoner positive match
- Reasoner no-match cases
- Docker Compose manifest generation and YAML parsing
- Kubernetes manifest generation with nodeSelector validation

### Integration Tests

Focus areas:

- REST API request/response behavior
- Full requirement-to-service matching flow
- Error handling
- Generated Kubernetes manifest existence

### Example Test Cases

```text
REQ_001 should match FlowmeterDiagnosticsService.
REQ_002 should not match if protocol requirement is MQTT.
REQ_003 should not match if latency constraint is too strict.
REQ_004 should select cloud service only when on-premise is not required.
REQ_005 should not match if minimum recall is higher than reported service recall.
Fermentation skid MTP AML parser generates valid CommunicationBinding triples.
Fermentation skid OWL inference derives OfferedCapability_TempControl.
HVAC plant OPC UA Server Datasheet parser generates CommunicationBinding with SignAndEncrypt security.
Cross-plant query returns temperature services from both fermentation skid and HVAC plant.
Docker Compose manifest includes restart: unless-stopped and environment block with asset ID.
Kubernetes manifest includes nodeSelector derived from DeploymentContext SIL and zone.
```

## Quality Rules

- No semantic matching logic should be hardcoded in Java if it belongs in SPARQL.
- Ontology and query files must be versioned in Git.
- The first version must be small and executable.
- Every roadmap phase must produce a testable result.
- Generated Docker Compose and Kubernetes YAML must be validated in tests.
- API responses must explain why a service was selected or why no service matched.
- Input file parsers must fail with clear, actionable errors on invalid input.
- OWL inference must preserve existing explicit capability triples.
