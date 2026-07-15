# Phase 1: Project Foundation — Implementation Plan

## 1. Maven & Build
- [ ] Add Milo dependencies (`sdk-core`, `sdk-client`) to `pom.xml`
- [ ] Add Vaadin 25, Spring Data JPA, PostgreSQL driver, Flyway
- [ ] Verify `mvn clean install -DskipTests` passes

## 2. Package Structure
- [ ] Create `com.sda.mapper.config` package (AppConfig, CorsConfig)
- [ ] Create `com.sda.mapper.model` package stubs (entities)
- [ ] Create `com.sda.mapper.repository` package stubs
- [ ] Create `com.sda.mapper.service` package stubs
- [ ] Create `com.sda.mapper.controller` package (SystemStatusController already exists)
- [ ] Create `com.sda.mapper.opcua` package (unified client, model, publisher, config)
- [ ] Create `com.sda.mapper.ui` package (Vaadin views)
- [ ] Verify build passes

## 3. Health & Observability
- [ ] Expand `SystemStatusController` to expose `/api/system/status` with version, commit, uptime
- [ ] Add custom health indicator: OntologyReadinessHealthIndicator
- [ ] Add custom health indicator: SparqlQueryHealthIndicator
- [ ] Add Micrometer metrics for `/api/system/status`
- [ ] Verify health endpoint returns correct response

## 4. Application Configuration
- [ ] Update `application.yml` with PostgreSQL, JPA, Flyway, Vaadin, Micrometer settings
- [ ] Create Flyway migration V1__init_schema.sql (minimal: system_status table)
- [ ] Verify application starts with PostgreSQL

## 5. Docker Compose
- [ ] Add `tank-simulation` service (port 4334) to `docker-compose.yml`
- [ ] Create `docker/tank-simulation/Dockerfile`
- [ ] Verify `docker compose up` starts all three services (nodered:4840, tank:4334, mapper:8080)

## 6. Vaadin 25 UI Foundation
- [ ] Create `MainLayout` with Lumo theme (light/dark toggle)
- [ ] Create `SystemStatusView` (displays `/api/system/status`)
- [ ] Create `ServiceInstancesView` (placeholder list of services)
- [ ] Verify UI renders at `http://localhost:8080/vaadin/`

## 7. Options Generator
- [ ] Implement `OptionsGenerator` — reads OWL, maps classes → JSON select options
- [ ] Implement `OwlParser` — loads OWL, extracts CSS-SDA hierarchy
- [ ] Implement `ClassMapper` — maps OWL classes to option items
- [ ] Create `POST /api/options` endpoint returning JSON select options
- [ ] Verify output against `docs/eval-dataset-fermentation-hvac/options.json`
- [ ] Verify all five phases covered: Intent, Capability, Skill, Service, CommunicationBinding, DataConnector

## 8. OPC UA Unified Java Client (Milo `milo-client-sdk`)
- [ ] Implement `OpcUaEndpointConfig` (host, port, SecurityPolicy, auth, ssl)
- [ ] Implement `OpcUaNodeConfig` (namespaceIndex, identifier, BrowsePath)
- [ ] Implement `OpcUaClientWrapper` (connect, disconnect, subscribe, read)
- [ ] Implement `OpcUaSubscriptionCallback` (dataChange, subscriptionLost)
- [ ] Implement `OpcUaClientFactory` (creates client from endpoint config)
- [ ] Implement `OpcUaVariableMonitor` (watches variables, publishes to MQTT/Kafka/REST)
- [ ] Implement `Ne107DiagnosticSimulator` (StatusCode, DiagnosticString, severity)
- [ ] Implement `OpcUaPumpServer` (Approach C: Java Milo in-process server)
- [ ] Implement `TankSimulationClient` (connects to Approach A or B)
- [ ] Implement `TankSimulationHealthIndicator`
- [ ] Write unit tests for all OPC UA classes
- [ ] Write integration test: start Java Milo server, read variables, verify NE107 diagnostics

## 9. OPC UA Simulation Servers (Node.js tank-fill model)
- [ ] Reference `docs/opcua-tank-simulation-server.js` copied to `docker/tank-simulation/server.js`
- [ ] Create `docker/tank-simulation/Dockerfile` (node:20-alpine, node-opcua, express, ws)
- [ ] Implement `TankPhysicsModel` — fills, leaks, spillover
- [ ] Implement `Ne107DiagnosticSimulator` (JavaScript) — StatusCode, DiagnosticString per variable
- [ ] Implement `OpcUaServer` — 12 variables with NAMUR NE107 attributes
- [ ] Implement `OpcUaSubscriptionHandler` — handles Create/Modify/Delete subscriptions
- [ ] Implement `OpcUaMonitoredItem` — polls at 500ms intervals
- [ ] Implement `ExpressApi` — `/api/tank/health`, `/api/tank/physics`
- [ ] Implement `WebSocketBridge` — `/ws` for real-time values
- [ ] Add HTTPS/SSL support with self-signed certificate
- [ ] Verify server starts on port 4334, connects to OPC UA simulator
- [ ] Verify all 12 variables have NAMUR NE107 attributes

## 10. Node-RED Flow (Approach A)
- [ ] Reference `docs/nodered-flow-fermentation-hvac.json` loaded in Node-RED
- [ ] Verify OPC UA server accessible on port 4840 with SignAndEncrypt security
- [ ] Verify fermentation and HVAC dataset variables are browsable
- [ ] Verify tank-fill pump variables with NAMUR NE107 diagnostics

## 11. NE 107 Pump Degradation (CSS-SDA Ontology)
- [ ] Add `DegradedMode` class to `css-sda.ttl` (subclass of Skill)
- [ ] Add `hasDegradationLevel` property to ontology
- [ ] Add pump degradation individuals: `pump1HealthyMode`, `pump1DegradedMode`, `pump1FaultedMode`
- [ ] Add SPARQL queries for NE 107 diagnostic mapping in `semantic/ne107-mapping.ru`
- [ ] Add test data with degradation scenarios in `test-data/ne107-pump/`

## 12. Acceptance Criteria
- [ ] All three OPC UA servers respond simultaneously (4840, 4334, Java in-process)
- [ ] Node-RED OPC UA server uses SSL (SignAndEncrypt, Aes128Sha256RsaOaep)
- [ ] NAMUR NE107 diagnostics visible for all three approaches
- [ ] Tank-fill pump shows healthy → degraded → faulted transition
- [ ] Java client connects to all three servers and reads variables
- [ ] CSS-SDA ontology includes pump degradation individuals and SPARQL queries
- [ ] `mvn clean verify` passes with all tests
- [ ] `docker compose up` starts all containers, health endpoints respond
- [ ] Vaadin UI renders on desktop, tablet, mobile; light/dark mode works
