# Task History

Track completed work with validation evidence. Only the human engineer marks tasks complete.

## Format

```markdown
- [ ] Not started
- [x] Completed
- [ ] [IN_PROGRESS]
- [ ] [BLOCKED]
```

---

## Phase 1: Project Foundation

**Status:** [x] Completed
**Branch:** `feature/2026-07-15-phase1-foundation`
**Completed:** 2026-07-15
**Tests:** 84 passing (0 failures)

### Completion Evidence

- `mvn clean test` — BUILD SUCCESS, 84 tests, 0 failures
- `mvn compile` — no errors
- All Phase 1 acceptance criteria marked `[x]` in `specs/roadmap.md`

### What was built

| Component | Files |
|---|---|
| Maven project (Spring Boot 4.0.7, Java 25) | `pom.xml` |
| Base package structure | `com.sda.mapper.{api,opcua,ontology,security,ui,deployment,error,reasoning}` |
| Application entry point | `SemanticSdaMapperApplication.java` |
| Vaadin UI foundation | `MainLayout.java`, `SystemStatusView.java`, `ServiceInstancesView.java`, `ThemeConfig.java`, `UIConfig.java` |
| 8 stub view classes | `OntologyExplorerView`, `RequirementFormView`, `RegistrationWizardView`, `SearchView`, `DashboardView`, `DeploymentView`, `MonitoringView`, `SettingsView` |
| Security | `SecurityConfig.java` (form-login, BCrypt, @Profile("!test")) |
| REST API | `SystemStatusController`, `OptionsController`, `OptionsConfigService`, `Option` record |
| OPC UA Server (Milo) | `TankOpcUaServer`, `TankNamespace`, `TankProcessState`, `SetInletFlow` method |
| OPC UA Client | `OpcUaClientWrapper`, `OpcUaClientFactory`, `OpcUaEndpointConfig`, `OpcUaNodeConfig` |
| NAMUR NE 107 | `PumpDegradationModel`, `Ne107DiagnosticSimulator` (with injectFault/clearFault/auto-recovery) |
| Health indicators | `OntologyReadinessHealthIndicator`, `SparqlQueryHealthIndicator`, `OpcUaSimulationHealthIndicator`, `TankSimulationHealthIndicator` |
| Ontology stub | `src/main/resources/ontology/css-sda-small.ttl` |
| SPARQL stub | `src/main/resources/queries/match-service.rq` |
| Options config | `src/main/resources/config/options.json` (7 categories) |
| Structured logging | `logback-spring.xml` (JSON prod, console dev), `logback-test.xml` |
| Observability | Micrometer Tracing + OTel bridge, OTLP exporter, Prometheus registry |
| Flyway | `V1__initial_schema.sql`, `spring.flyway.enabled=true` |
| Responsive CSS | `META-INF/resources/themes/sda-mapper/styles.css` |
| Docker | `docker/Dockerfile`, `docker-compose.yml` (nodered, tank-simulation, semantic-sda-mapper) |
| Test infrastructure | `src/test/resources/application.yml` (H2, test profile, Vaadin/security exclusions) |

### Test classes (16 total, 84 @Test methods)

| Test Class | Tests |
|---|---|
| `SemanticSdaMapperApplicationTest` | 1 |
| `ContainerHealthEndpointTest` | 1 |
| `OptionsConfigServiceTest` | 4 |
| `OptionsConfigServiceErrorTest` | 3 |
| `OptionsControllerTest` | 3 |
| `SystemStatusControllerTest` | 3 |
| `OntologyAndQueryLoadingTest` | 2 |
| `TankProcessStateTest` | 4 |
| `PumpDegradationModelTest` | 6 |
| `Ne107DiagnosticSimulatorTest` | 6 |
| `Ne107DiagnosticSimulatorExtendedTest` | 9 |
| `OpcUaClientFactoryTest` | 9 |
| `OpcUaClientWrapperTest` | 10 |
| `OpcUaEndpointConfigTest` | 10 |
| `OpcUaNodeConfigTest` | 10 |
| `MainLayoutTest` | 3 |

### Key decisions

- Spring Boot 4 uses `tools.jackson` (Jackson 3), not `com.fasterxml.jackson`
- Spring Boot 4 health classes in `org.springframework.boot.health.contributor`
- Spring Boot 4 security auto-config in `org.springframework.boot.security.autoconfigure`
- Vaadin 25.0.12 via `com.vaadin:vaadin-spring-boot-starter` (not Spring Boot managed)
- SecurityConfig uses `@Profile("!test")` to disable in test profile
- `RouterLink` requires VaadinService context — UI tests verify structure without instantiation
- Milo `UaMethodNode` requires 9-param constructor; `MethodInvocationHandler` in `sdk.server.api.methods`
