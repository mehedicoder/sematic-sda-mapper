# Tech Stack: Semantic SDA Mapper

## Runtime Platform

| Area | Technology |
|---|---|
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

## Core Dependencies

### Spring Boot Starters

```xml
spring-boot-starter-web          <!-- REST API -->
spring-boot-starter-validation   <!-- request validation -->
spring-boot-starter-actuator     <!-- health/metrics endpoints -->
spring-boot-starter-test         <!-- unit and integration testing -->
```

### Observability

```xml
io.micrometer:micrometer-registry-prometheus      <!-- /actuator/prometheus metrics -->
io.micrometer:micrometer-tracing-bridge-otel      <!-- OpenTelemetry distributed tracing -->
io.opentelemetry:opentelemetry-exporter-otlp      <!-- export traces to OTLP backends -->
```

Logging uses Logback (bundled via `spring-boot-starter`). Configure structured JSON output for production and plain-text with correlation IDs for development.

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

```xml
org.apache.jena:apache-jena-libs   <!-- includes jena-core -->
```

Load RDF/Turtle ontology, execute SPARQL queries, run OWL reasoner (`OWL_MEM_MICRO_RULES_INF`), and process semantic matching results.

### XML Parsing (MTP AML)

MTP AML files are XML archives (VDI/VDE/NAMUR 2658). Parsing uses Java's built-in XML APIs (SAX/DOM) or a lightweight XML library assured by the Spring Boot BOM.

### JSON Parsing (OPC UA Server Datasheet)

OPC UA Server Datasheet files follow the IDTA 02009-1-0 AAS submodel JSON schema. Jackson (bundled with Spring Boot) handles parsing.

### YAML Support

```xml
org.yaml:snakeyaml   <!-- Kubernetes deployment YAML generation and test validation -->
```

### Vaadin 25

```xml
com.vaadin:vaadin-spring-boot-starter
```

Server-side UI framework with Lumo theme, AppLayout/Grid/FormLayout components, automatic client-server data binding, and responsive/accessibility support. Cytoscape.js and ApexCharts are included via npm dependencies managed by Vaadin's frontend build.

### PostgreSQL

```xml
org.springframework.boot:spring-boot-starter-data-jpa
org.postgresql:postgresql
```

Persistent storage for registered services, users, deployment records, and UI state via JPA/Hibernate ORM.

### Database Migrations

```xml
org.flywaydb:flyway-core
org.flywaydb:flyway-database-postgresql
```

Version-controlled schema evolution. Migration files live under `src/main/resources/db/migration/` following the Flyway naming convention (`V<version>__<description>.sql`).

### Spring Security

```xml
org.springframework.boot:spring-boot-starter-security
```

Authentication, authorization, role-based access control, and OAuth2 / form-login integration.

## Environment Variables

All externalized configuration uses Spring Boot's `${ENV_VAR:default}` binding. No secrets, credentials, or environment-specific URLs are committed to the repository.

### Database

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `DATABASE_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/semantic_sda` | PostgreSQL connection string |
| `DATABASE_USERNAME` | `spring.datasource.username` | `postgres` | |
| `DATABASE_PASSWORD` | `spring.datasource.password` | — | Required, no default |
| `SPRING_FLYWAY_ENABLED` | `spring.flyway.enabled` | `true` | Disable for clean-slate test runs |

### Observability

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `management.tracing.exporter.otlp.endpoint` | — | Jaeger/Tempo URL; omit to disable OTLP export |
| `OTEL_SERVICE_NAME` | `otel.service.name` | `semantic-sda-mapper` | Service identity in trace backends |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `management.tracing.sampling.probability` | `1.0` | Reduce in production (e.g., `0.1`) |
| `MANAGEMENT_ENDPOINTS_EXPOSURE_INCLUDE` | `management.endpoints.web.exposure.include` | `health,prometheus,metrics` | Widen in dev, narrow in prod |
| `MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED` | `management.prometheus.metrics.export.enabled` | `true` | Disable if no Prometheus server |

### Ontology & SPARQL

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `ONTOLOGY_FILE_PATH` | `app.ontology.file-path` | `ontology/css-sda-small.ttl` | Classpath path to ontology TTL |
| `ONTOLOGY_INFERENCE_ENABLED` | `app.ontology.inference-enabled` | `true` | Disable OWL reasoner for unit tests |
| `SPARQL_QUERY_PATH` | `app.sparql.query-path` | `queries/match-service.rq` | Classpath path to SPARQL query |

### OPC UA Simulation

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `OPC_UA_SERVER_HOST` | `app.opcua.server-host` | `nodered` | Hostname of simulated OPC UA server |
| `OPC_UA_SERVER_PORT` | `app.opcua.server-port` | `4840` | OPC UA port |
| `OPC_UA_SERVER_ENDPOINT` | `app.opcua.server-endpoint` | `opc.tcp://nodered:4840` | Full OPC UA endpoint URI |
| `OPC_UA_HEALTH_ENDPOINT` | `app.opcua.health-endpoint` | `http://nodered:1880/` | Node-RED health check URL |

### Security

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `SPRING_SECURITY_USER_NAME` | `spring.security.user.name` | `admin` | Default admin username |
| `SPRING_SECURITY_USER_PASSWORD` | `spring.security.user.password` | — | Required, no default |

### Logging

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `LOGGING_LEVEL_ROOT` | `logging.level.root` | `INFO` | `DEBUG` in dev, `WARN` in prod |
| `LOGGING_LEVEL_APP` | `logging.level.com.sda.mapper` | `INFO` | Per-package log control |
| `LOGGING_PATTERN_CONSOLE` | `logging.pattern.console` | *(see observability block)* | Override for structured JSON in prod |

### Deployment Manifest Generation

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `DEFAULT_CONTAINER_REGISTRY` | `app.deployment.default-registry` | `registry.local` | Container registry for generated YAML |
| `DEFAULT_CONTAINER_TAG` | `app.deployment.default-tag` | `latest` | Pin versions in staging/prod |

### Server & Profiles

| Env Variable | YAML Property | Default | Notes |
|---|---|---|---|
| `SERVER_PORT` | `server.port` | `8080` | Already externalized |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | — | Set to `container` in Docker Compose |
| `SPRING_APPLICATION_NAME` | `spring.application.name` | `semantic-sda-mapper` | Service name override |

### Suggested `application.yml` Binding

```yaml
spring:
  application:
    name: ${SPRING_APPLICATION_NAME:semantic-sda-mapper}
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/semantic_sda}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:}
  flyway:
    enabled: ${SPRING_FLYWAY_ENABLED:true}

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: ${MANAGEMENT_ENDPOINTS_EXPOSURE_INCLUDE:health,prometheus,metrics}
  tracing:
    sampling:
      probability: ${MANAGEMENT_TRACING_SAMPLING_PROBABILITY:1.0}
  prometheus:
    metrics:
      export:
        enabled: ${MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED:true}

app:
  ontology:
    file-path: ${ONTOLOGY_FILE_PATH:ontology/css-sda-small.ttl}
    inference-enabled: ${ONTOLOGY_INFERENCE_ENABLED:true}
  sparql:
    query-path: ${SPARQL_QUERY_PATH:queries/match-service.rq}
  opcua:
    server-host: ${OPC_UA_SERVER_HOST:nodered}
    server-port: ${OPC_UA_SERVER_PORT:4840}
    server-endpoint: ${OPC_UA_SERVER_ENDPOINT:opc.tcp://nodered:4840}
    health-endpoint: ${OPC_UA_HEALTH_ENDPOINT:http://nodered:1880/}
  deployment:
    default-registry: ${DEFAULT_CONTAINER_REGISTRY:registry.local}
    default-tag: ${DEFAULT_CONTAINER_TAG:latest}

logging:
  level:
    root: ${LOGGING_LEVEL_ROOT:INFO}
    com.sda.mapper: ${LOGGING_LEVEL_APP:INFO}
  pattern:
    console: ${LOGGING_PATTERN_CONSOLE:%d{ISO8601} %-5p [%t] %c{1.} [%X{traceId:-},%X{spanId:-}] %m%n}
```

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
├── pom.xml
├── README.md
├── specs/
│   ├── mission.md
│   ├── tech-stack.md
│   ├── roadmap.md
│   └── config-options.md
│
├── src/
│   ├── main/
│   │   ├── java/com/example/semanticsda/
│   │   │   ├── SemanticSdaApplication.java
│   │   │   ├── api/
│   │   │   │   ├── RequirementMatchingController.java
│   │   │   │   └── dto/
│   │   │   │       ├── MatchRequirementRequest.java
│   │   │   │       └── MatchRequirementResponse.java
│   │   │   ├── parser/
│   │   │   │   ├── ParserFactory.java
│   │   │   │   ├── MTPAmlParser.java
│   │   │   │   └── UaServerDatasheetParser.java
│   │   │   ├── inference/
│   │   │   │   └── OntologyInferenceService.java
│   │   │   ├── reasoning/
│   │   │   │   ├── SemanticReasonerService.java
│   │   │   │   ├── SparqlQueryLoader.java
│   │   │   │   └── ServiceMatchResult.java
│   │   │   ├── ontology/
│   │   │   │   ├── OntologyModelLoader.java
│   │   │   │   └── OntologyResourceProperties.java
│   │   │   ├── deployment/
│   │   │   │   ├── DockerComposeManifestService.java
│   │   │   │   ├── KubernetesManifestService.java
│   │   │   │   └── ManifestResult.java
│   │   │   ├── error/
│   │   │   │   ├── ApiErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── observability/
│   │   │   │   └── (metrics, health indicators, structured logging config)
│   │   │   ├── config/
│   │   │   │   ├── OptionsConfigService.java
│   │   │   │   └── Option.java
│   │   │   └── ui/
│   │   │       ├── MainLayout.java
│   │   │       ├── DashboardView.java
│   │   │       ├── ExplorerView.java
│   │   │       ├── SearchView.java
│   │   │       ├── UploadView.java
│   │   │       ├── RegistrationWizardView.java
│   │   │       ├── RequirementFormView.java
│   │   │       ├── GraphView.java
│   │   │       ├── MonitoringView.java
│   │   │       ├── SettingsView.java
│   │   │       ├── components/
│   │   │       │   ├── DashboardCard.java
│   │   │       │   ├── SearchBar.java
│   │   │       │   ├── OntologyTree.java
│   │   │       │   ├── UploadCard.java
│   │   │       │   ├── ServiceCard.java
│   │   │       │   ├── StatisticsPanel.java
│   │   │       │   ├── NotificationPanel.java
│   │   │       │   └── StatusBadge.java
│   │   │       ├── services/UIService.java
│   │   │       ├── theme/ThemeConfig.java
│   │   │       ├── security/SecurityConfig.java
│   │   │       └── config/UIConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── db/migration/V1__initial_schema.sql
│   │       ├── ontology/css-sda-small.ttl
│   │       ├── queries/match-service.rq
│   │       ├── config/options.json
│   │       └── k8s/deployment-template.yaml
│   │
│   └── test/
│       ├── java/com/example/semanticsda/
│       │   ├── parser/
│       │   │   ├── MTPAmlParserTest.java
│       │   │   └── UaServerDatasheetParserTest.java
│       │   ├── inference/OntologyInferenceServiceTest.java
│       │   ├── reasoning/SemanticReasonerServiceTest.java
│       │   ├── deployment/
│       │   │   ├── DockerComposeManifestServiceTest.java
│       │   │   └── KubernetesManifestServiceTest.java
│       │   ├── api/RequirementMatchingControllerIT.java
│       │   ├── config/OptionsConfigServiceTest.java
│       │   └── ui/
│       │       ├── DashboardViewTest.java
│       │       ├── ExplorerViewTest.java
│       │       ├── RegistrationWizardViewTest.java
│       │       └── RequirementFormViewTest.java
│       └── resources/
│           ├── ontology/test-css-sda-small.ttl
│           ├── queries/test-match-service.rq
│           ├── config/test-options.json
│           └── examples/
│               ├── mtp-fermentation-skid.xml
│               └── uaserver-ahu01.json
│
├── docker/
│   ├── Dockerfile
│   └── examples/flowmeter-diagnostics-compose.yaml
├── docker-compose.yml
└── k8s/examples/
    ├── flowmeter-diagnostics-deployment.yaml
    └── fermentation-skid-deployment.yaml
```

## Main Application Modules

| Module | Package | Key Responsibilities |
|---|---|---|
| **API** | `api` | REST endpoints, request validation, `POST /api/v1/requirements/match` (by ID or inline fields), `POST /api/v1/requirements`, `GET /api/v1/options/{field}` |
| **Parser** | `parser` | Parse MTP AML (XML) and OPC UA Datasheet (JSON) into CSS-SDA triples; fail fast on invalid input |
| **Inference** | `inference` | Run OWL reasoner to derive `sda:OfferedCapability` from `css:SkillParameter` semantics |
| **Ontology** | `ontology` | Load RDF/Turtle files, build Jena model, fail fast on missing/invalid ontology |
| **Reasoning** | `reasoning` | Load SPARQL queries, execute matching, convert results to DTOs, handle no-match cases |
| **Deployment** | `deployment` | Generate Docker Compose and Kubernetes YAML, derive `nodeSelector` from `sda:DeploymentContext` |
| **Error** | `error` | `ProblemDetail` (RFC 9457) error responses via `@ControllerAdvice` |
| **Observability** | `observability` | Micrometer metrics, custom health indicators, structured logging with trace/span IDs |
| **Config** | `config` | Load configurable dropdown options from `config/options.json`, serve via `GET /api/v1/options/{field}` |
| **UI** | `ui` | Vaadin views built incrementally across phases 1–13 (see `specs/roadmap.md`) |

### Exception Hierarchy

All custom exceptions extend `RuntimeException`. Map each to `ProblemDetail` via `GlobalExceptionHandler`.

| Exception | Cause |
|---|---|
| `OntologyLoadingException` | Missing/invalid ontology file |
| `SparqlExecutionException` | SPARQL query parse or runtime failure |
| `MatchingException` | Unexpected condition during semantic matching |
| `InputParseException` | Malformed MTP AML or OPC UA Server Datasheet input |

### Custom Metrics

| Metric Name | Type | Description |
|---|---|---|
| `sda.match.requests.total` | Counter | Total match requests received |
| `sda.match.success` | Counter | Successful matches |
| `sda.match.failure` | Counter | No-match results |
| `sda.parse.invocations` | Counter | Parser invocations by type |
| `sda.inference.duration` | Timer | OWL inference duration |
| `sda.manifest.generated` | Counter | Generated manifests by format |

## Data and Semantic Assets

| Asset | Location |
|---|---|
| Ontology | `src/main/resources/ontology/css-sda-small.ttl` |
| SPARQL query | `src/main/resources/queries/match-service.rq` |
| Kubernetes template | `src/main/resources/k8s/deployment-template.yaml` |

## API Contract

### Match Requirement (Mode A — by ID)

```
POST /api/v1/requirements/match
```

**Request:**
```json
{ "requirementId": "REQ_001" }
```

### Match Requirement (Mode B — inline fields)

```
POST /api/v1/requirements/match
```

**Request:**
```json
{
  "functionClass": "AssetConditionMonitoring",
  "protocol": "OPC-UA",
  "accessMode": "read-only",
  "engineeringUnit": "%",
  "nominalRange_min": 0,
  "nominalRange_max": 100,
  "safetyIntegrityLevel": "SIL-0",
  "networkZone": "plant-network",
  "determinismClass": "SoftRealTime",
  "minRecall": 0.90,
  "maxLatencyMs": 60
}
```

### Successful Response (both modes)

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

### No-match Response

```json
{
  "matched": false,
  "requirementId": "REQ_001",
  "message": "No service satisfies the requirement constraints."
}
```

### Save Requirement

```
POST /api/v1/requirements
```

**Request:** same inline fields as Mode B above. Persists a new requirement to the ontology and returns the generated requirement ID.

### Get Dropdown Options

```
GET /api/v1/options/{fieldName}
```

**Response:**
```json
[
  { "value": "OPC-UA", "label": "OPC UA" },
  { "value": "PROFINET", "label": "PROFINET" },
  { "value": "BACnet", "label": "BACnet" }
]
```

Returns `404 Not Found` if `fieldName` does not exist in `config/options.json`. Options are loaded from `src/main/resources/config/options.json` (see `specs/config-options.md`).

## Local Development

### Required Tools

```
JDK 25, Maven 3.9+, Docker, Docker Compose, IntelliJ IDEA or equivalent
```

### Commands

```bash
mvn clean test                         # run tests
mvn spring-boot:run                    # start locally
docker build -t semantic-sda-mapper:local -f docker/Dockerfile .   # build image
docker compose up --build              # run with Docker Compose
```

## Testing Strategy

- **Unit tests**: ontology loading, SPARQL query loading, parsers (valid/malformed/missing), OWL inference, reasoner positive/negative match, manifest generation with YAML validation, UI view rendering
- **Integration tests**: REST API request/response, full requirement-to-service flow, error handling, generated manifest existence
