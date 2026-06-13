# Phase 1: Project Foundation Requirements

## Feature Summary

Establish a runnable Java 25 and Spring Boot 4.0.7 foundation for Semantic SDA Mapper. The foundation must support local Maven execution and container-based execution, expose operational health information, and provide a minimal smoke API proving that the web application is reachable.

This phase prepares the codebase for later ontology, SPARQL, matching, and Kubernetes manifest features without implementing those domain capabilities.

## Scope

### Included

- Create a Maven-based Spring Boot 4.0.7 application using Java 25.
- Use the root Java package `com.sda.mapper`.
- Add Spring Web, Validation, Actuator, Apache Jena 6.1.0, SnakeYAML 2.5, Spring Boot Test, JUnit 5, and AssertJ support.
- Create a small package skeleton under `com.sda.mapper` for `api`, `ontology`, `reasoning`, `deployment`, and `error` ownership boundaries. Use `package-info.java` for packages that do not yet contain implementation classes.
- Add the main Spring Boot application class.
- Add a minimal read-only smoke endpoint at `GET /api/v1/system/status`.
- Return a stable JSON response from the smoke endpoint:

  | Field | Type | Required value | Purpose |
  |---|---|---|---|
  | `status` | string | `UP` | Confirms that the application web layer is reachable. |
  | `application` | string | `semantic-sda-mapper` | Identifies the running application. |

- Expose Spring Actuator health at `GET /actuator/health`.
- Add base and container-specific configuration using `application.yml` and `application-container.yml`. The container profile must enable Actuator liveness and readiness health probes.
- Use port `8080` by default and allow environment-variable overrides where Spring Boot supports them.
- Add a container-oriented Dockerfile that runs the application as a non-root user.
- Add `docker-compose.yml` with the container profile enabled, port mapping, and a health check against `/actuator/health/readiness`.
- Add a basic Spring context test and web tests for the smoke and health endpoints.
- Add a README with prerequisites, Maven commands, Docker build commands, Docker Compose commands, endpoint examples, and troubleshooting notes relevant to Java 25 and container health.

### Excluded

- RDF/Turtle ontology files or ontology modeling.
- Apache Jena model loading or lifecycle management.
- SPARQL query files or semantic matching behavior.
- Requirement matching API contracts.
- Kubernetes manifest generation.
- Real OPC UA communication or industrial device connectivity.
- Authentication, authorization, persistence, messaging, cloud services, or external infrastructure.
- Production deployment configuration beyond a locally buildable image and Docker Compose workflow.

## Decisions

### Package and application identity

- The root package is `com.sda.mapper` rather than the package suggested in `specs/tech-stack.md`, based on the human engineer's direction.
- The Maven artifact and Spring application name should be `semantic-sda-mapper`.
- Package boundaries without implementation classes are represented by concise `package-info.java` documentation. Phase 1 must not add placeholder domain services that imply unapproved behavior.

### Dependency versions

- Use Spring Boot `4.0.7` as the parent and plugin-managed platform version.
- Use Apache Jena `apache-jena-libs` version `6.1.0` with dependency type `pom`.
- Declare SnakeYAML without an explicit dependency version so Spring Boot 4.0.7 dependency management supplies version `2.5`.
- Do not use version ranges, snapshots, milestone builds, or dynamically resolved latest-version selectors.

### Smoke API

- `GET /api/v1/system/status` is included in Phase 1 to verify web routing independently from Actuator.
- The endpoint has no input, persistence, domain logic, timestamps, host details, build details, or environment details. This keeps its output deterministic and avoids leaking runtime metadata.
- The smoke API is not the requirement-matching contract planned for Phase 6.

### Configuration

- `application.yml` contains shared defaults and exposes only the Actuator health endpoint.
- `application-container.yml` sets `management.endpoint.health.probes.enabled=true` and is activated in Docker Compose through `SPRING_PROFILES_ACTIVE=container`.
- With the container profile active, `/actuator/health/liveness` and `/actuator/health/readiness` must be available as health endpoint groups. Local execution without the container profile is not required to expose these probe groups.
- Configuration must not contain secrets or machine-specific absolute paths.

### Container behavior

- The Docker image uses a multi-stage build unless implementation evidence shows a simpler approach is necessary for Java 25 compatibility.
- The runtime process runs as a non-root user.
- The application listens on port `8080` by default.
- Docker Compose publishes `8080:8080`, activates the container profile, and declares a health check for `/actuator/health/readiness`.
- The selected runtime image or health-check mechanism must make the health check executable without adding an unnecessary application dependency.

### Dependency boundaries

- Apache Jena and SnakeYAML are declared because the approved roadmap requires them, but they are not exercised by semantic or YAML-generation code in this phase.
- Spring Boot dependency management controls SnakeYAML and Spring test-library versions. Apache Jena remains explicitly pinned because it is not supplied by Spring Boot dependency management.
- No dependency outside `specs/tech-stack.md` may be added without human approval, except build plugins normally managed by Spring Boot's Maven parent/plugin management.

## Context

- Container readiness is the primary implementation emphasis.
- The result remains a small executable research prototype, not a production platform.
- Follow Java 25, Spring Boot 4.0.7, Maven, JUnit 5, AssertJ, Spring Boot Test, Docker, and Docker Compose constraints from `specs/tech-stack.md`.
- Prefer immutable response types such as Java records.
- Add concise JavaDoc to public classes and public contract methods.
- Keep endpoint output and container configuration deterministic.
- Use clear startup and health-check failures; do not hide failed checks.
- The implementation must remain compatible with later packages for ontology, reasoning, deployment, API, and error handling.

## Artifact Placement

All feature-specific specification and report artifacts belong under `specs/2026-06-13-project-foundation/`:

- `requirements.md`
- `plan.md`
- `validation.md`
- `spec-review-report.md` when produced by the Reviewer Agent
- `spec-validation-report.md` when produced by the Validation Agent
- `review-report.md` when produced by the Reviewer Agent
- `validation-report.md` when produced by the Validation Agent

Implementation files remain in their standard repository locations such as `pom.xml`, `src/`, `docker/`, `docker-compose.yml`, and `README.md`.
