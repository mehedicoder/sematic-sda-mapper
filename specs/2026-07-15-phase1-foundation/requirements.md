# Phase 1: Project Foundation — Requirements

## Scope

Build the complete project foundation for the Semantic SDA Mapper, including Maven build configuration, package structure, health/observability endpoints, Docker Compose orchestration, Vaadin 25 UI foundation, three OPC UA simulation approaches (A: Node-RED, B: Node.js tank, C: Java Milo), unified Java OPC UA client, NAMUR NE 107 pump degradation simulation, and options.json generation.

## In Scope
- Maven dependencies (Milo, Vaadin 25, JPA, PostgreSQL, Flyway)
- Java package structure and stubs
- Health endpoints and Micrometer metrics
- Docker Compose with three OPC UA services
- Vaadin 25 UI (MainLayout, SystemStatusView, ServiceInstancesView)
- Options generator (OWL → JSON select options)
- OPC UA unified Java client (Milo `milo-client-sdk`)
- OPC UA simulation server (Node.js tank-fill, SSL)
- NAMUR NE 107 diagnostic simulation (JavaScript + CSS-SDA ontology)
- Tank-fill pump degradation model
- Approach A: Node-RED with SSL (port 4840)
- Approach B: Node.js standalone (port 4334)
- Approach C: Java Milo in-process

## Out of Scope
- Phase 2+ features (OPC UA client integration, Jena reasoning, matching engine, real OPC UA devices)
- Redis, Kafka, queues, cloud services
- Production-grade OPC UA device communication
- Safety-critical control logic

## Acceptance Criteria

### Build & Package
1. `mvn clean install -DskipTests` completes successfully
2. All packages exist under `com.sda.mapper`
3. Application starts on port 8080 with PostgreSQL

### Health & Observability
4. `GET /api/system/status` returns version, commit, uptime
5. Custom health indicators for ontology readiness, SPARQL query loadability
6. Micrometer metrics exposed at `/actuator/prometheus`

### Docker Compose
7. `docker compose up` starts all three OPC UA services
8. Node-RED on port 4840 (SSL), tank simulation on port 4334, Java in-process
9. All three respond simultaneously without port conflicts

### Vaadin UI
10. UI renders at `http://localhost:8080/vaadin/`
11. Light/dark mode toggle persists
12. Responsive on desktop, tablet, mobile viewports

### Options Generator
13. `POST /api/options` returns valid JSON select options
14. All five CSS-SDA phases covered: Intent, Capability, Skill, Service, CommunicationBinding, DataConnector
15. Output matches `docs/eval-dataset-fermentation-hvac/options.json`

### OPC UA Unified Client
16. `OpcUaClientWrapper` connects to any of the three servers
17. `OpcUaVariableMonitor` watches variables and publishes to MQTT/Kafka/REST
18. `Ne107DiagnosticSimulator` returns StatusCode + DiagnosticString per variable
19. Unit tests pass for all OPC UA classes

### OPC UA Tank Simulation (Approach B)
20. `docker/tank-simulation/server.js` starts on port 4334
21. 12 OPC UA variables with NAMUR NE107 attributes
22. HTTPS/SSL with self-signed certificate
23. Express API at `/api/tank/health` and `/api/tank/physics`
24. WebSocket bridge at `/ws` for real-time values

### Node-RED (Approach A)
25. OPC UA server on port 4840 with SignAndEncrypt security
26. Fermentation and HVAC dataset variables browsable
27. Tank-fill pump variables with NAMUR NE107 diagnostics

### NE 107 Pump Degradation
28. CSS-SDA ontology includes `DegradedMode` class and `hasDegradationLevel` property
29. Pump degradation individuals: healthy, degraded, faulted
30. SPARQL queries for NE 107 diagnostic mapping
31. Tank-fill pump shows healthy → degraded → faulted transition

### Testing
32. `mvn clean verify` passes with all tests
33. Unit tests for Java Milo server, OPC UA client, physics models
34. Integration test: start server, read variables, verify NE107 diagnostics
35. Vaadin UI tests for view rendering and component interaction

## Dependencies
- Java 25 + Spring Boot 4.0.x
- Maven 3.9.x
- Docker + Docker Compose
- PostgreSQL (for JPA/Flyway)
- Eclipse Milo 0.6.14 (`sdk-core`, `sdk-client`)
- Vaadin 25 (Lumo)
- Apache Jena 6.1.0

## References
- `specs/roadmap.md` — Phase 1 task list (lines 43–230)
- `specs/tech-stack.md` — Technology choices
- `docs/opcua-tank-simulation-server.js` — Reference tank simulation
- `docker/tank-simulation/server.js` — Runtime tank simulation
- `docs/nodered-flow-fermentation-hvac.json` — Node-RED flow
- `docs/opcua-node-red-server-guide.md` — Server reference
- `docs/opcua-secure-server-guide.md` — SSL security reference
- `docs/opcua-node-red-client-guide.md` — Client reference
