# Phase 1: Project Foundation Validation

## Validation Preconditions

- JDK 25 is installed and `java -version` reports Java 25.
- Maven 3.9 or newer is installed.
- Docker Engine and Docker Compose are available for container checks.
- Port `8080` is available, or an explicitly documented override is used where applicable.
- Validation starts from the repository root.

## Automated Validation

### Maven build and tests

Run:

```bash
mvn clean test
```

Required assertions:

- Maven compiles main and test sources for Java 25.
- The Spring application context loads without external services or semantic assets.
- `GET /api/v1/system/status` returns HTTP 200 and JSON content.
- The smoke response contains exactly the intended stable contract fields:

```json
{
  "status": "UP",
  "application": "semantic-sda-mapper"
}
```

- `GET /actuator/health` returns HTTP 200 with JSON path `$.status` equal to `UP`.
- With the `container` profile active, `GET /actuator/health/liveness` and `GET /actuator/health/readiness` return HTTP 200 with JSON path `$.status` equal to `UP`.
- Tests do not require Docker, network services, ontology files, or machine-specific paths.

### Package and dependency inspection

Verify:

- The root application package is `com.sda.mapper`.
- The project uses Spring Boot 4.0.7 and Java release 25.
- Apache Jena resolves to 6.1.0.
- SnakeYAML resolves to 2.5 through Spring Boot dependency management and has no explicit dependency version in the project POM.
- Required Spring Web, Validation, Actuator, Apache Jena, SnakeYAML, and test dependencies are present.
- No version range, snapshot, milestone, or dynamic latest-version selector is used.
- No unapproved database, queue, cache, cloud, or security dependency is introduced.
- No ontology, SPARQL, matching, or Kubernetes manifest implementation is included.

Record effective dependency versions with concrete Maven output. Acceptable commands include:

```bash
mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout
mvn dependency:tree -Dincludes=org.apache.jena,org.yaml:snakeyaml
```

Required output:

- Parent version is `4.0.7`.
- Apache Jena artifacts resolve at `6.1.0`.
- `org.yaml:snakeyaml` resolves at `2.5`.

### Package structure inspection

Verify that Git-tracked Java sources define these packages:

- `com.sda.mapper`
- `com.sda.mapper.api`
- `com.sda.mapper.api.dto`
- `com.sda.mapper.ontology`
- `com.sda.mapper.reasoning`
- `com.sda.mapper.deployment`
- `com.sda.mapper.error`

Packages without implementation classes must use `package-info.java`; empty directories and placeholder domain classes do not satisfy this requirement.

### Docker image

Run:

```bash
docker build -t semantic-sda-mapper:phase-1 -f docker/Dockerfile .
```

Required assertions:

- The build completes successfully from a clean build context.
- The image contains and starts the packaged Spring Boot application.
- The configured runtime user is not root.
- The container exposes or listens on application port `8080`.
- No source-control metadata, IDE metadata, or local build output is copied unnecessarily into the image.

### Docker Compose

Run:

```bash
docker compose up --build -d
docker compose ps
```

Wait for the configured health check, then require:

- The service is running and reported healthy.
- The `container` Spring profile is active.
- `http://localhost:8080/actuator/health` returns HTTP 200 and `UP`.
- `http://localhost:8080/actuator/health/liveness` returns HTTP 200 and `UP`.
- `http://localhost:8080/actuator/health/readiness` returns HTTP 200 and `UP`; this is the Compose health-check target.
- `http://localhost:8080/api/v1/system/status` returns the specified smoke response.
- The service does not depend on any additional container.

After validation, run:

```bash
docker compose down
```

The shutdown must complete without requiring volume deletion or manual cleanup.

## Manual Validation

### Local Maven walkthrough

1. Run `mvn spring-boot:run`.
2. Confirm startup completes without ontology files or external infrastructure.
3. Request `/actuator/health` and verify `UP`.
4. Request `/api/v1/system/status` and verify the deterministic JSON contract.
5. Stop the process and confirm it exits normally.

### Configuration walkthrough

1. Review `application.yml` for shared defaults and health-only Actuator exposure.
2. Review `application-container.yml` and confirm it sets `management.endpoint.health.probes.enabled=true`.
3. Confirm Docker Compose activates the `container` profile through environment configuration.
4. Confirm the liveness and readiness health groups are unavailable without the profile unless Spring Boot explicitly enables them for the detected runtime environment, and available with the profile.
5. Confirm no secrets, local absolute paths, or environment-specific credentials are committed.

### Container walkthrough

1. Inspect the Dockerfile for Java 25 compatibility and multi-stage construction.
2. Confirm the final runtime process uses a non-root user.
3. Confirm Compose publishes the expected port and has a bounded readiness health check with a startup grace period.
4. Restart the Compose service and verify it returns to healthy status.

### README walkthrough

Follow the README from a repository checkout and verify that it accurately documents:

- Required tool versions.
- Maven test and startup commands.
- Smoke and health endpoint usage.
- Docker image build and run commands.
- Docker Compose startup, health inspection, logs, and shutdown.
- Troubleshooting for Java version mismatch, occupied port `8080`, and unhealthy containers.

## Edge Cases

- Starting without ontology and query resources must succeed in Phase 1.
- Requesting an unknown API path must not return the smoke response.
- The smoke response must not expose profiles, hostnames, environment variables, build paths, or secrets.
- Container startup failures must remain visible through exit status, logs, or unhealthy state.
- A port conflict must fail clearly rather than silently selecting a different port.

## Tone Check

- README language is concise, technical, and appropriate for a research prototype.
- Documentation does not claim semantic matching, ontology loading, Kubernetes generation, or production readiness exists in Phase 1.
- Error and troubleshooting text gives actionable commands without hiding failures.

## Definition of Done

- All Phase 1 roadmap tasks are implemented.
- `mvn clean test` passes on Java 25.
- The application starts locally, the smoke and base health endpoints behave as specified, and the container profile exposes working liveness and readiness groups.
- The Docker image builds and runs as a non-root user.
- Docker Compose starts the application with the container profile and reaches healthy status.
- README instructions have been manually followed successfully.
- No out-of-scope semantic or infrastructure behavior is introduced.
- Validation evidence is recorded in `specs/2026-06-13-project-foundation/validation-report.md`.
- Phase 1 roadmap markers remain incomplete until validation evidence exists or the human explicitly requests an update.
