# Semantic SDA Mapper

Semantic SDA Mapper is a Java and Spring Boot prototype for mapping industrial automation requirements to deployable services. Phase 1 provides the runnable application foundation, operational health endpoints, automated tests, and local container workflow. Ontology loading, SPARQL matching, and Kubernetes manifest generation are planned for later phases.

## Prerequisites

- JDK 21
- Maven 3.9 or newer
- Docker Engine
- Docker Compose

Confirm that Maven uses Java 21:

```bash
java -version
mvn -version
```

Both commands must report Java 21 before running the Maven build.

## Test

```bash
mvn clean test
```

## Run Locally

```bash
mvn spring-boot:run
```

The application listens on port `8080` by default. Override it with `SERVER_PORT` when needed.

### Vaadin UI

Open `http://localhost:8080/` in your browser to access the Vaadin-based UI with:

- **System Status** — application health and diagnostic overview
- **Service Instances** — registered CSS-SDA service instances

The UI uses the Lumo theme and supports light/dark mode.

### Smoke endpoint

```bash
curl http://localhost:8080/api/v1/system/status
```

Expected response:

```json
{"status":"UP","application":"semantic-sda-mapper"}
```

### Health endpoint

```bash
curl http://localhost:8080/actuator/health
```

Expected status:

```json
{"status":"UP"}
```

The liveness and readiness groups are enabled by the `container` Spring profile:

```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

## Docker Image

Build the Java 21 image:

```bash
docker build -t semantic-sda-mapper:local -f docker/Dockerfile .
```

Run it with the container profile:

```bash
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=container semantic-sda-mapper:local
```

The runtime image executes the application as a non-root user.

## Docker Compose

Build and start the service:

```bash
docker compose up --build -d
```

Inspect health and logs:

```bash
docker compose ps
docker compose logs semantic-sda-mapper
```

Compose checks `/actuator/health/readiness` and reports the service as healthy after startup completes.

Stop the service:

```bash
docker compose down
```

## Troubleshooting

### Maven uses an older Java version

Set `JAVA_HOME` to a JDK 25 installation and ensure `%JAVA_HOME%\bin` on Windows or `$JAVA_HOME/bin` on Linux/macOS appears before older Java installations on `PATH`. Run `mvn -version` again to confirm Java 25 is active.

### Port 8080 is already in use

Stop the process using port `8080`, or run the local application on another port:

```bash
SERVER_PORT=8081 mvn spring-boot:run
```

For Docker Compose, change the host side of the port mapping, for example `8081:8080`.

### Compose service is unhealthy

Inspect status and application logs:

```bash
docker compose ps
docker compose logs semantic-sda-mapper
```

Verify that the `container` profile is active and that `/actuator/health/readiness` is reachable inside the container. Container startup and health failures are intentionally left visible for diagnosis.
