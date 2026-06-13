# Phase 1: Project Foundation Plan

## Implementation Plan

Task: Project Foundation | Phase: Phase 1

### Files to Modify or Create

- `pom.xml`
- `src/main/java/com/sda/mapper/SemanticSdaMapperApplication.java`
- `src/main/java/com/sda/mapper/api/SystemStatusController.java`
- `src/main/java/com/sda/mapper/api/dto/SystemStatusResponse.java`
- `src/main/java/com/sda/mapper/api/package-info.java`
- `src/main/java/com/sda/mapper/api/dto/package-info.java`
- `src/main/java/com/sda/mapper/ontology/package-info.java`
- `src/main/java/com/sda/mapper/reasoning/package-info.java`
- `src/main/java/com/sda/mapper/deployment/package-info.java`
- `src/main/java/com/sda/mapper/error/package-info.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-container.yml`
- `src/test/java/com/sda/mapper/SemanticSdaMapperApplicationTest.java`
- `src/test/java/com/sda/mapper/api/SystemStatusControllerTest.java`
- `docker/Dockerfile`
- `.dockerignore`
- `docker-compose.yml`
- `README.md`

### Semantic Model and Query Considerations

- No ontology classes, properties, individuals, RDF/Turtle assets, or SPARQL queries are introduced in Phase 1.
- Apache Jena is available as a dependency for later phases only.
- No service-selection rule is implemented in Java.

### API and Deployment Considerations

- Add only `GET /api/v1/system/status` with a deterministic response.
- Retain Actuator's `GET /actuator/health` as the operational health contract.
- Do not introduce the Phase 6 requirement-matching endpoint.
- Docker and Compose must run the same application artifact and activate the container profile.

### Runtime and Performance Considerations

- No Jena model is loaded, cached, or shared in this phase.
- Spring beans should remain stateless.
- Container startup and health checks should use bounded intervals and a startup grace period appropriate for a Spring Boot application.
- Avoid additional runtime services and unnecessary image contents.

### Test Coverage Plan

- [ ] Spring application context test
- [ ] Smoke endpoint status and JSON contract test
- [ ] Actuator health endpoint status and `UP` assertion
- [ ] Container-profile liveness and readiness probe tests
- [ ] Maven clean test execution
- [ ] Local startup verification
- [ ] Docker image build verification
- [ ] Docker Compose startup and health verification

## Task Groups

### 1. Maven and Application Bootstrap

1.1 Create `pom.xml` with the Spring Boot 4.0.7 parent and Java 25 compiler settings.

1.2 Add Spring Web, Validation, Actuator, Apache Jena `apache-jena-libs` 6.1.0 with type `pom`, SnakeYAML, and Spring Boot Test dependencies.

1.3 Use Spring Boot dependency management for SnakeYAML 2.5 and test-library versions; do not specify a separate SnakeYAML version.

1.4 Configure the Spring Boot Maven plugin and ensure Maven uses UTF-8 source encoding.

1.5 Create `SemanticSdaMapperApplication` under `com.sda.mapper`.

1.6 Confirm the empty application context starts without requiring ontology or external services.

### 2. Package and Configuration Foundation

2.1 Establish Git-tracked package ownership boundaries for `api`, `api.dto`, `ontology`, `reasoning`, `deployment`, and `error` using concise `package-info.java` documentation where no implementation class exists.

2.2 Create `application.yml` with the application name, default port, and health-only Actuator exposure.

2.3 Create `application-container.yml` with `management.endpoint.health.probes.enabled=true` without duplicating shared defaults.

2.4 Keep all configurable values environment-overridable and free of secrets.

### 3. Smoke API and Health Surface

3.1 Create an immutable `SystemStatusResponse` contract.

3.2 Create `SystemStatusController` with `GET /api/v1/system/status`.

3.3 Return only `status: UP` and `application: semantic-sda-mapper`.

3.4 Verify that `GET /actuator/health` is available while other Actuator endpoints are not exposed over HTTP unless required by Spring Boot defaults.

3.5 Verify that the `container` profile enables `GET /actuator/health/liveness` and `GET /actuator/health/readiness` health groups.

### 4. Automated Tests

4.1 Add a context-load test using Spring Boot Test.

4.2 Add a web test asserting the smoke endpoint's HTTP status, content type, exact fields, and values.

4.3 Add a web test asserting `/actuator/health` returns HTTP 200 and status `UP`.

4.4 Add a profile-specific web test asserting the liveness and readiness health groups are available and return `UP` when the `container` profile is active.

4.5 Run `mvn clean test` and address failures without weakening assertions.

4.6 Record the effective Spring Boot, Apache Jena, and SnakeYAML versions using Maven evaluation or dependency-tree output.

### 5. Containerization

5.1 Add `.dockerignore` to exclude build output, IDE files, Git metadata, and unrelated local files from the build context.

5.2 Create `docker/Dockerfile` with Java 25-compatible build and runtime stages.

5.3 Configure a non-root runtime user, port `8080`, and direct Java process startup.

5.4 Create `docker-compose.yml` to build the image, activate the `container` profile, publish port `8080`, and check `/actuator/health/readiness`.

5.5 Build the image and verify its configured runtime user is non-root.

5.6 Start with Docker Compose and wait for the service to become healthy.

### 6. Developer Documentation

6.1 Add README prerequisites for JDK 25, Maven 3.9+, Docker, and Docker Compose.

6.2 Document Maven test and local startup commands.

6.3 Document smoke, health, liveness, and readiness endpoint requests and expected responses.

6.4 Document Docker image build and run commands.

6.5 Document Docker Compose startup, health inspection, logs, and shutdown commands.

6.6 Document common Java-version, port-conflict, and unhealthy-container troubleshooting steps.

### 7. Phase Completion Evidence

7.1 Record automated and manual results in `validation-report.md` through the Validation Agent workflow.

7.2 Update Phase 1 roadmap markers only after all acceptance criteria have validation evidence or the human explicitly directs the update.

7.3 Update `tasks.md` only if it exists or is introduced as part of the approved implementation workflow and completed work is supported by evidence.
