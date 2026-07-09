# Tech Stack: Semantic SDA Mapper

## Runtime Platform

| Area | Technology |
|---|---|---|
| Programming language | Java 25 |
| Backend framework | Spring Boot 4.0.x |
| UI framework | Vaadin 25.x (Lumo Theme) |
| Build tool | Maven |
| API style | REST (backend) + Vaadin server-side (UI) |
| RDF processing | Apache Jena |
| OWL reasoning | Apache Jena OWL_MEM_MICRO_RULES_INF |
| Semantic query language | SPARQL |
| Ontology format | RDF/Turtle |
| Input file format (MTP) | XML (AML Archive, VDI/VDE/NAMUR 2658) |
| Input file format (Datasheet) | JSON (AAS, IDTA 02009-1-0) |
| Database | PostgreSQL |
| Security | Spring Security |
| Ontology graph visualization | Cytoscape.js |
| Dashboard charts | ApexCharts |
| Deployment manifest formats | Docker Compose YAML + Kubernetes YAML |
| Testing | JUnit 5, AssertJ, Spring Boot Test, Vaadin Test |
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

### Observability

Required dependencies:

```xml
io.micrometer:micrometer-registry-prometheus
io.micrometer:micrometer-tracing-bridge-otel
io.opentelemetry:opentelemetry-exporter-otlp
```

Purpose:

- `micrometer-registry-prometheus`: expose JVM, request, and custom metrics at `/actuator/prometheus`
- `micrometer-tracing-bridge-otel`: distributed tracing with OpenTelemetry bridge
- `opentelemetry-exporter-otlp`: export traces to OTLP-compatible backends (e.g., Jaeger, Grafana Tempo)

Logging uses Logback (bundled via `spring-boot-starter`). Configure structured JSON output for production and plain-text with correlation IDs for development.

### Configuration

Required `application.yml` observability block:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, metrics
  metrics:
    tags:
      application: ${spring.application.name:semantic-sda-mapper}
  tracing:
    sampling:
      probability: 1.0
logging:
  pattern:
    console: "%d{ISO8601} %-5p [%t] %c{1.} [%X{traceId:-},%X{spanId:-}] %m%n"
```

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

### Vaadin 25

Required dependency:

```xml
com.vaadin:vaadin-spring-boot-starter
```

Purpose:

- Server-side UI framework with built-in Lumo theme
- AppLayout, Grid, FormLayout, and rich component library
- Automatic client-server data binding
- Built-in responsive and accessibility support

### PostgreSQL

Required dependency:

```xml
org.springframework.boot:spring-boot-starter-data-jpa
org.postgresql:postgresql
```

Purpose:

- Persistent storage for registered services, users, deployment records, and UI state
- JPA/Hibernate for object-relational mapping

### Database Migrations

Required dependency:

```xml
org.flywaydb:flyway-core
org.flywaydb:flyway-database-postgresql
```

Purpose:

- Version-controlled database schema evolution
- Automatic migration on application startup via Spring Boot auto-configuration
- Reproducible schema state across local dev, test, and deployment environments

Migration files live under `src/main/resources/db/migration/` and follow the standard Flyway naming convention (`V<version>__<description>.sql`). The initial migration creates the baseline schema for the application.

### Spring Security

Required dependency:

```xml
org.springframework.boot:spring-boot-starter-security
```

Purpose:

- Authentication and authorization for UI pages
- Role-based access control
- OAuth2 / form-login integration

### Frontend Libraries (bundled via Vaadin)

- **Cytoscape.js** — interactive ontology graph visualization (zoom, pan, expand/collapse, highlight paths)
- **ApexCharts** — real-time dashboard charts (CPU, memory, OPC UA sessions, semantic queries)

These are included via npm dependencies managed by Vaadin's frontend build.

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
│   │   │       ├── error/
│   │   │       │   ├── ApiErrorResponse.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       └── ui/
│   │   │           ├── MainLayout.java
│   │   │           ├── DashboardView.java
│   │   │           ├── ExplorerView.java
│   │   │           ├── SearchView.java
│   │   │           ├── UploadView.java
│   │   │           ├── RegistrationWizardView.java
│   │   │           ├── GraphView.java
│   │   │           ├── MonitoringView.java
│   │   │           ├── SettingsView.java
│   │   │           ├── components/
│   │   │           │   ├── DashboardCard.java
│   │   │           │   ├── SearchBar.java
│   │   │           │   ├── OntologyTree.java
│   │   │           │   ├── UploadCard.java
│   │   │           │   ├── ServiceCard.java
│   │   │           │   ├── StatisticsPanel.java
│   │   │           │   ├── NotificationPanel.java
│   │   │           │   └── StatusBadge.java
│   │   │           ├── services/
│   │   │           │   └── UIService.java
│   │   │           ├── theme/
│   │   │           │   └── ThemeConfig.java
│   │   │           ├── security/
│   │   │           │   └── SecurityConfig.java
│   │   │           └── config/
│   │   │               └── UIConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── db/migration/
│   │       │   └── V1__initial_schema.sql
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
│       │       ├── api/
│       │       │   └── RequirementMatchingControllerIT.java
│       │       └── ui/
│       │           ├── DashboardViewTest.java
│       │           ├── ExplorerViewTest.java
│       │           └── RegistrationWizardViewTest.java
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

- Convert technical errors into clear API errors using Spring's `ProblemDetail` (RFC 9457)
- Handle invalid requirement IDs
- Handle no matching service
- Handle ontology loading failures
- Handle SPARQL query failures

### Exception Design

Define unchecked exception classes per domain concern:

| Exception | Cause |
|---|---|
| `OntologyLoadingException` | Missing/invalid ontology file |
| `SparqlExecutionException` | SPARQL query parse or runtime failure |
| `MatchingException` | Unexpected condition during semantic matching |
| `InputParseException` | Malformed MTP AML or OPC UA Server Datasheet input |

All custom exceptions extend `RuntimeException`. Use `@ControllerAdvice` + `@ExceptionHandler` in `GlobalExceptionHandler` to map each exception to a consistent `ProblemDetail` response. Wrap checked exceptions (e.g., `IOException` from file loading, `SAXException` from XML parsing) at the service boundary into the appropriate custom unchecked exception.

### Observability Module

Package:

```text
com.sda.mapper.observability
```

Responsibilities:

- Expose Micrometer metrics for service layer, parser, inference, and reasoner operations
- Record histograms and counters for match requests, parse invocations, inference runs, manifest generations
- Configure custom health indicators beyond the default Actuator health (ontology model loaded, SPARQL queries loadable, OPC UA simulation reachable)
- Provide structured logging with trace and span IDs across all service boundaries
- No external observability platform is required at MVP time; Prometheus endpoint and console-printed traces suffice

Custom metrics to register:

| Metric Name | Type | Description |
|---|---|---|
| `sda.match.requests.total` | Counter | Total match requests received |
| `sda.match.success` | Counter | Successful matches |
| `sda.match.failure` | Counter | No-match results |
| `sda.parse.invocations` | Counter | Parser invocations by type |
| `sda.inference.duration` | Timer | OWL inference duration |
| `sda.manifest.generated` | Counter | Generated manifests by format |

### UI Module

Package:

```text
com.sda.mapper.ui
```

Built incrementally across phases 1–13. Each phase adds or upgrades the views that depend on the corresponding backend service:

| Phase | UI Work |
|---|---|
| 1 | MainLayout, ThemeConfig, SecurityConfig, responsive shell |
| 2 | ExplorerView (static ontology tree) |
| 3 | ExplorerView (dynamic Jena model loading, detail tabs) |
| 4 | UploadView (drag-drop, progress, validation) |
| 5 | SearchBar (global, autocomplete), SearchView (results grid) |
| 6 | DashboardView (basic cards from inference) |
| 7 | DashboardView (live match statistics) |
| 8 | RegistrationWizardView (multi-step) |
| 9 | DeploymentView (environment cards, manifest viewer) |
| 10 | DashboardView explanation panel, NotificationCenter |
| 11 | MonitoringView (ApexCharts), NotificationCenter full |
| 13 | GraphView (Cytoscape.js), SettingsView, accessibility, mobile QA, performance, integration tests |

Responsibilities:

- Main application layout with AppLayout, header with global search, navigation drawer, and theme toggle
- Dashboard view with animated cards (registered services, capabilities, skills, mappings, digital twins, deployments, simulations, OPC UA sessions)
- Semantic Explorer with split layout (ontology tree + entity details)
- Global AI-style search with autocomplete, fuzzy, exact, and ontology-scoped filters
- Upload Center with drag-and-drop for OPC UA NodeSet, AAS, MTP, RDF, OWL, JSON-LD, YAML
- Service Registration Wizard (multi-step: General → Communication → Capabilities → Skills → Deployment → Mapping → Review → Register)
- Interactive ontology graph visualization using Cytoscape.js (zoom, pan, expand/collapse, highlight paths)
- Real-time monitoring dashboard with ApexCharts (CPU, memory, OPC UA sessions, semantic queries)
- Deployment view grouped by environment (Dev, Test, Prod, Edge, Cloud)
- Notification center with grouped success, info, warning, error, and background task notifications
- User settings (theme, language, time zone, notifications, accessibility, search preferences)
- Light/dark mode via Lumo theme variants with persisted preference
- Responsive layouts (desktop drawer visible, tablet collapsible, mobile overlay)

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
- UI view rendering and component behavior
- UI service layer and security configuration

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
Dashboard view loads and displays animated cards with correct counts.
Search bar autocomplete returns ontology entities matching the query.
Service Registration Wizard navigates through all 8 steps without errors.
Ontology graph renders nodes and supports zoom and pan.
Upload Center validates file types and shows progress.
Monitoring dashboard chart data updates reflect backend state.
```

## Quality Rules

- No semantic matching logic should be hardcoded in Java if it belongs in SPARQL.
- Ontology and query files must be versioned in Git.
- The first version must be small and executable.
- Every roadmap phase must produce a testable result.
- Generated Docker Compose and Kubernetes YAML must be validated in tests.
- API error responses must use Spring's `ProblemDetail` (RFC 9457) with consistent structure.
- API responses must explain why a service was selected or why no service matched.
- Input file parsers must fail with clear, actionable errors on invalid input.
- OWL inference must preserve existing explicit capability triples.
- UI views must be responsive and work on desktop, tablet, and mobile viewports.
- UI must support light and dark mode with persisted user preference.
- Navigation must follow the CSS-SDA hierarchy (Intent → Capability → Skill → Service → CommunicationBinding → DataConnector → Deployment).
- Every service-layer method that processes a request must record Micrometer metrics (counter or timer).
- Structured logging with trace and span IDs must be configured from Phase 1 onward.
- Custom health indicators must be added for ontology readiness, SPARQL query loadability, and OPC UA simulation reachability.
