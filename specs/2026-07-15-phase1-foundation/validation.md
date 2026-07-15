# Phase 1: Project Foundation — Validation Steps

## Build Validation
- [ ] `mvn clean install -DskipTests` completes with BUILD SUCCESS
- [ ] `mvn clean verify` passes all tests

## Application Startup
- [ ] `docker compose up -d postgresql` starts PostgreSQL
- [ ] `mvn spring-boot:run` starts application on port 8080
- [ ] `curl http://localhost:8080/api/system/status` returns JSON with version, commit, uptime

## Health Indicators
- [ ] `curl http://localhost:8080/actuator/health` includes custom indicators
- [ ] `curl http://localhost:8080/actuator/prometheus` returns Micrometer metrics

## Docker Compose
- [ ] `docker compose up -d` starts all services
- [ ] `docker compose ps` shows nodered, tank-simulation, semantic-sda-mapper running
- [ ] Node-RED accessible at `https://localhost:4840` (SSL)
- [ ] Tank simulation accessible at `https://localhost:4334` (SSL)
- [ ] Mapper accessible at `http://localhost:8080`

## OPC UA Server Validation
- [ ] Connect to Node-RED (port 4840) with SSL — read variables
- [ ] Connect to Tank simulation (port 4334) with SSL — read variables
- [ ] Connect to Java Milo server — read variables
- [ ] All three respond simultaneously without port conflicts

## NAMUR NE 107 Validation
- [ ] Tank-fill pump variables show StatusCode + DiagnosticString
- [ ] Pump transitions: healthy → degraded → faulted
- [ ] NE107 severity values correct (0=Good, 1=Low, 2=Medium, 3=High)

## Vaadin UI Validation
- [ ] Navigate to `http://localhost:8080/vaadin/`
- [ ] SystemStatusView displays version and uptime
- [ ] ServiceInstancesView shows placeholder list
- [ ] Light/dark mode toggle works
- [ ] Responsive on mobile viewport (375px)

## Options Generator Validation
- [ ] `POST /api/options` returns valid JSON
- [ ] All five phases present: Intent, Capability, Skill, Service, CommunicationBinding, DataConnector
- [ ] Output matches `docs/eval-dataset-fermentation-hvac/options.json`

## Test Validation
- [ ] Unit tests for `OpcUaClientWrapper`, `OpcUaVariableMonitor`, `Ne107DiagnosticSimulator`
- [ ] Unit tests for `TankPhysicsModel`, `Ne107DiagnosticSimulator` (JavaScript)
- [ ] Integration test: start Java Milo server, read variables, verify NE107 diagnostics
- [ ] Vaadin UI tests pass

## Rollback Plan
- If build fails: check `pom.xml` dependencies, Maven cache
- If Docker fails: check Dockerfile paths, port conflicts
- If OPC UA fails: check SSL certificate, port assignments
- If UI fails: check Vaadin compatibility, Lumo theme configuration
