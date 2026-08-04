# Roadmap: Semantic SDA Mapper

## Roadmap Principle

The project will be implemented as a sequence of small vertical slices. Each phase should produce something executable, testable, and reviewable.

The primary goal is to prove this semantic mapping path:

```text
Input Files (MTP AML, OPC UA Server Datasheet, AAS)
→ Parse
→ Instantiate (CSS-SDA ontology ABox triples)
→ Lift (MTP Service → css:Skill)
→ Infer (OWL reasoner derives OfferedCapability)
→ Match (SPARQL multi-dimensional query)
→ Docker Compose / Kubernetes Manifest
```

The CSS-SDA model: SDA-Service := ⟨ Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext ⟩

**Input sources** (vendor-supplied device description files):
- **MTP AML files** (VDI/VDE/NAMUR 2658) — modular process equipment with Service/Parameter/Communication blocks
- **OPC UA Server Datasheets** (IDTA 02009-1-0) — AAS submodel with EndpointDescriptions, Configuration, Identification
- **AAS submodels** — device metadata including SIL, lifecycle, engineering units (UCUM), semantic IDs (IEC CDD IRDI)

**Five-step mapping procedure** — applied to every input file:
1. **Parse** — read and validate the input file against its schema
2. **Instantiate** — create CommunicationBinding, DataConnector, DeploymentContext ABox triples
3. **Lift** — transform service descriptions into css:Skill + css:SkillParameter instances with UCUM units and IRDI identifiers
4. **Infer** — OWL reasoner derives sda:OfferedCapability from SkillParameter semantics (key novel step)
5. **Match** — SPARQL query compares OfferedCapability against RequiredCapability across all CSS-SDA dimensions simultaneously

Apache Jena and SPARQL are part of the semantic reasoning layer.

```text
Apache Jena + SPARQL = semantic matching core
Docker Compose = generated deployment output
```

---

## Phase 1: Project Foundation

**Goal**

Create a runnable Java 25 and Spring Boot 4.0.x project with basic structure, testing, Vaadin 25 UI foundation, local development setup, and automation pipeline foundation.

**Tasks**

* [x] Create Maven-based Spring Boot 4.0.x project
  * [x] Add Spring Web, Validation, Actuator dependencies
  * [x] Add Micrometer Prometheus registry for metrics export
  * [x] Add Micrometer Tracing with OpenTelemetry bridge for distributed tracing
  * [x] Add Apache Jena dependency
  * [x] Add SnakeYAML dependency
  * [x] Add JUnit 5 and AssertJ test support
  * [x] Add Vaadin 25 Spring Boot starter and Lumo theme dependency
  * [x] Add Spring Security dependency
  * [x] Add Spring Data JPA and PostgreSQL dependencies
  * [x] Add Flyway database migration dependencies
* [x] Create base Java package structure under `com.sda.mapper`
* [x] Add health endpoint through Spring Actuator
* [x] Configure `application.yml` for observability (metrics, tracing, log pattern with trace/span IDs)
* [x] Add custom health indicators for ontology readiness, SPARQL query loadability, OPC UA simulation reachability
* [x] Configure structured logging with Logback for JSON output in production
* [x] Add Vaadin `MainLayout` with AppLayout structure:
  * [x] Header with CSS-SDA logo, title, theme switch, user profile
  * [x] Collapsible navigation drawer following CSS-SDA hierarchy (Intent → Use Cases → Requirements → Capability → Capabilities → Skills → Service Layer → Services → Communication Bindings → Data Connectors → Deployment → Assets → Digital Twins → Monitoring → Administration)
  * [x] Main workspace with responsive routing
* [x] Add `ThemeConfig` with Lumo light/dark mode toggle and persisted preference
* [x] Add `SecurityConfig` with form-login and role-based access skeleton
* [x] Add `UIConfig` with Vaadin routing configuration
* [x] Implement responsive layout:
  * [x] Desktop: Drawer always visible
  * [x] Tablet: Drawer collapses automatically
  * [x] Mobile: Drawer becomes overlay
* [x] Add Dockerfile
* [x] Add docker-compose.yml
* [x] Add README with local run instructions
* [x] Add OPC UA server service to `docker-compose.yml`
 * [x] Configure OPC UA server endpoint and security settings
 * [x] Add OPC UA server health check
 * [x] Add network configuration for service-to-server communication
 * [x] Add integration test that verifies OPC UA server availability
* [x] Add Vaadin UI test for MainLayout rendering and navigation drawer interaction
* [x] Add `docs/banner.png` to MainLayout drawer as project branding
  * [x] Copy `docs/banner.png` to `src/main/resources/META-INF/resources/images/banner.png`
  * [x] Display banner image at top of drawer, above subtitle and navigation links
  * [x] Make banner clickable (navigates to home view)
  * [x] Ensure banner scales responsively and respects dark/light theme
  * [x] Add Vaadin UI test: banner renders in drawer and is clickable
* [x] Add Flyway migration directory `src/main/resources/db/migration/` with `V1__initial_schema.sql`
* [x] Configure `spring.flyway.enabled=true` and baseline settings in `application.yml`
 * [x] Add Node-RED service to `docker-compose.yml` (OPC UA Simulation Approach A — Fermentation + HVAC)
  * [x] Install OpcUa-Server node in Node-RED
  * [x] Mount `docs/nodered-flow-fermentation-hvac.json` as a volume in the Node-RED container so the flow loads automatically on start
  * [x] Use `docs/nodered-flow-fermentation-hvac.json` flow to simulate OPC UA server (Fermentation Skid ns=2, HVAC ns=3, test vars ns=1)
  * [x] Secure Node-RED OPC UA server with SSL certificates
   * [x] Generate self-signed SSL certificates (public + private key) for the OPC UA server
   * [x] Copy certificates into Docker image via `docker/nodered/Dockerfile`
   * [x] Configure Compact Server Security tab: disable anonymous access, enable individual certificate files
   * [x] Set certificate paths in server configuration (`server_selfsigned_cert_2048.pem`, `server_key_2048.pem`)
   * [x] Configure security mode: SignAndEncrypt with Aes128Sha256RsaOaep policy
   * [x] Export client-side DER certificate for Java Milo client trust
   * [x] Add integration test: Java Milo client connects with SignAndEncrypt security to Node-RED server
   * [x] Add integration test: anonymous connection is rejected when SSL is enabled
 * [x] Add standalone Tank1 OPC UA simulation service to `docker-compose.yml` (OPC UA Simulation Approach B — Node.js tank process)
  * [x] Create `docker/tank-simulation/Dockerfile` based on `node:20-alpine`
  * [x] Create `docker/tank-simulation/package.json` with `node-opcua` dependency
  * [x] Port `docs/opcua-tank-simulation-server.js` tank simulation script into `docker/tank-simulation/server.js`
  * [x] Expose OPC UA server on port 4334 (`/UA/FlowMeterServer`)
  * [x] Expose Tank1 object with Level, FlowRateIn, FlowRateOut, Temperature variables
  * [x] Implement physical process model: level dynamics from net flow, temperature drift
  * [x] Expose SetInletFlow method for client actuation
  * [x] Add `tank-simulation` service to `docker-compose.yml` on port 4334 with health check
  * [x] Add network configuration for application-to-Tank1 communication
 * [x] Add Java in-process OPC UA Tank1 simulation using Eclipse Milo (OPC UA Simulation Approach C — Java SDK)
  * [x] Add Eclipse Milo `sdk-server` and `sdk-core` dependencies to `pom.xml`
  * [x] Create `TankProcessState` — level, flowIn, flowOut, temperature, `stepSimulation(dt)` physics
  * [x] Create `TankOpcUaServer` — Milo server with Tank1 address space (Level, FlowRateIn, FlowRateOut, Temperature)
  * [x] Bind read callbacks for Level, Temperature (computed); write callbacks for FlowRateIn, FlowRateOut
  * [x] Implement `SetInletFlow` method via `InvocableNode` / `MethodInvocationHandler`
  * [x] Create `TankSimulationScheduler` — `ScheduledExecutorService` 1s tick calling `stepSimulation`
  * [x] Register as Spring `@Bean`, start on application startup, stop on shutdown
  * [x] Configure `simulation.opcua.enabled`, `simulation.opcua.port` in `application.yml`
  * [x] Add custom health indicator `OpcUaSimulationHealthIndicator` for Java in-process server
  * [x] Add integration test: Java OPC UA server starts, Tank1 node browsable, variables readable
  * [x] Add integration test: SetInletFlow method invocation changes FlowRateIn
  * [x] Add integration test: Level changes after simulation tick (physics model validation)
  * [x] Add unit test: `TankProcessState.stepSimulation` level calculation correctness
  * [x] Add unit test: Level clamps to [0, maxLevel] boundaries
 * [x] Add NAMUR NE 107 pump degradation simulation — device diagnostics and condition monitoring
  * [x] Create `PumpDegradationModel` — physical degradation physics (healthPercent, wearRate, vibration, temperature offset)
  * [x] Implement degradation phases: Healthy → Early wear → Seal leak → Impeller erosion → Failure
  * [x] Model accelerating degradation curve: wearRate increases as healthPercent decreases
  * [x] Compute effective flow capacity: effectiveFlowIn = setpoint × (healthPercent / 100)
  * [x] Model vibration amplitude increasing with degradation level
  * [x] Model temperature offset from bearing heat during degradation
  * [x] Create `Ne107DiagnosticSimulator` — maps degradation state to NE 107 status codes per variable
  * [x] Define NE 107 status enum: Good (Green), Maintenance (Yellow), FunctionCheck (Blue), Failure (Red)
  * [x] Define NE 107 sub-codes for Failure: Sensor (S), Function (F), WireBreak (W), ShortCircuit (K)
  * [x] Expose per-variable diagnostic: value + StatusCode + DiagnosticString
  * [x] Implement fault injection: injectFault(variable, faultType) for manual/automatic scenarios
  * [x] Implement auto-recovery: clearFault after configurable duration
  * [x] Extend `TankOpcUaServer` with pump variables: Pump.FlowRate, Pump.Vibration, Pump.Temperature, Pump.Status
  * [x] Expose NE 107 diagnostic variables: Pump.FlowRate_Status, Pump.FlowRate_Diag, etc.
  * [x] Expose device-level diagnostic: Pump.DeviceStatus (NE 107 aggregate status)
  * [x] Create CSS-SDA ontology individuals for the pump scenario
  * [x] Add `:AssetConditionMonitoringCapability` individual to ontology
  * [x] Add `:PumpDiagnosticsSkill` individual with `requiresConcept :PA-DIM-FlowConcept`
  * [x] Add `:AssetDiagnosticsService` individual with reportedRecall, reportedLatency
  * [x] Add `:PumpBinding` CommunicationBinding (protocol OPC-UA, accessMode read-only)
  * [x] Add `:PumpConnector` DataConnector (informationModel OPC-UA, semanticID IEC-61987-42)
  * [x] Add `:PumpDeployment` DeploymentContext (SIL-1, SoftRealTime, onPlantNetwork true)
  * [x] Add `:REQ-03` Requirement individual mapped to AssetConditionMonitoringCapability
  * [x] Add SPARQL query extension: filter out devices with NE 107 Failure status
  * [x] Add integration test: healthy pump matches REQ-03 via SPARQL query (R1–R3 satisfied)
  * [x] Add integration test: degraded pump (Maintenance) still matches but triggers alert
  * [x] Add integration test: failed pump (Failure) excluded by SPARQL query — no-match result
  * [x] Add integration test: pump recovery restores match after failure cleared
  * [x] Add unit test: PumpDegradationModel degradation curve correctness
  * [x] Add unit test: Ne107DiagnosticSimulator status mapping per fault type
  * [x] Add unit test: NE 107 sub-codes (Sensor, WireBreak, ShortCircuit, Function) mapped correctly
 * [x] Add unified OPC UA Java client to read from all simulation servers (Approach A, B, C) via Apache Milo
  * [x] Add Eclipse Milo `milo-client-sdk` dependency to `pom.xml`
  * [x] Create `OpcUaClientConfig` — configure endpoint URLs, security policy, timeout per server
  * [x] Create `OpcUaClientService` — reusable `readNodeValue(endpointUrl, nodeId)` method
  * [x] Support unencrypted (None) security policy for local simulation servers
  * [x] Support NodeId string identifiers (`ns=2;s=Fermentation.TempSetpoint`) and numeric identifiers
  * [x] Handle connection lifecycle: connect, read, disconnect with proper error handling
  * [x] Configure `opc-ua.client.read-timeout-ms`, `opc-ua.client.connect-timeout-ms` in `application.yml`
  * [x] Add integration test: read Fermentation.TempSetpoint from Node-RED server (Approach A, port 4840)
  * [x] Add integration test: read Tank1.Level from Node.js server (Approach B, port 4334)
  * [x] Add integration test: read Tank1.Level from Java Milo server (Approach C, in-process)
  * [x] Add integration test: read across all three servers in a single client session
  * [x] Add unit test: client handles connection failure gracefully (server unreachable)
  * [x] Add unit test: client handles invalid NodeId gracefully
* [x] Create `src/main/resources/config/options.json` with configurable dropdown values (see `specs/config-options.md`)
* [x] Create `OptionsConfigService` to load options from classpath
* [x] Create `Option` record (`value`, `label`)
* [x] Add `GET /api/v1/options/{fieldName}` endpoint to serve dropdown options
* [x] Add unit test: options load correctly from JSON
* [x] Add unit test: missing `options.json` fails fast on startup
* [x] Add unit test: malformed JSON throws clear error
* [x] Add unit test: `GET /api/v1/options/{fieldName}` returns correct list
* [x] Add unit test: `GET /api/v1/options/nonexistent` returns 404

**Acceptance Criteria**

* [x] `mvn clean test` passes
* [x] Application starts locally
* [x] `/actuator/health` returns `UP`
* [x] Vaadin UI accessible at `http://localhost:8080/` with MainLayout and navigation drawer
* [x] Light/dark mode toggle works
* [x] Navigation drawer follows CSS-SDA hierarchy and is collapsible
* [x] UI is responsive on desktop, tablet, and mobile viewports
* [x] Vaadin UI test for MainLayout rendering and drawer navigation passes
* [x] Docker image can be built
* [x] Application can be started through Docker Compose
* [x] Node-RED OPC UA server (Approach A) starts via `docker compose up` with Fermentation (ns=2) and HVAC (ns=3) variables
* [x] Node-RED OPC UA server is reachable from the application container on port 4840
* [x] Node-RED OPC UA server accepts SignAndEncrypt connections with Aes128Sha256RsaOaep policy
* [x] Node-RED OPC UA server rejects anonymous connections when SSL is enabled
* [x] Java Milo client connects to Node-RED server with SSL certificates and reads variables
* [x] Tank1 OPC UA simulation (Approach B, Node.js) starts via `docker compose up` on port 4334
* [x] Tank1 OPC UA server (Approach B) is reachable from the application container
* [x] Tank1 OPC UA server exposes Level, FlowRateIn, FlowRateOut, Temperature variables
* [x] Tank1 SetInletFlow method is callable from a client
* [x] Java in-process OPC UA Tank1 server (Approach C, Milo) starts with Spring Boot application
* [x] Java OPC UA server exposes Tank1 with Level, FlowRateIn, FlowRateOut, Temperature
* [x] Java OPC UA SetInletFlow method invocation changes FlowRateIn
* [x] Java OPC UA Level variable changes after simulation tick (physics model running)
* [x] Java OPC UA server health indicator appears in `/actuator/health`
* [x] Pump degradation model simulates Healthy → Early wear → Seal leak → Impeller erosion → Failure phases
* [x] NE 107 diagnostic status correctly maps degradation to Good / Maintenance / Failure per variable
* [x] NE 107 sub-codes (Sensor, WireBreak, ShortCircuit, Function) are distinguishable
* [x] Pump OPC UA variables expose value + StatusCode + DiagnosticString
* [x] CSS-SDA ontology individuals for pump scenario are loadable by Apache Jena
* [x] SPARQL query returns AssetDiagnosticsService for REQ-03 when pump is healthy
* [x] SPARQL query excludes pump when NE 107 status is Failure
* [x] Pump recovery after failure clears restores SPARQL match
* [x] All three OPC UA simulation approaches can run simultaneously without port conflicts
* [x] Unified Java Milo client reads Temperature from Node-RED Fermentation server (Approach A)
* [x] Unified Java Milo client reads Level from Node.js Tank1 server (Approach B)
* [x] Unified Java Milo client reads Level from Java Milo Tank1 server (Approach C)
* [x] Single client session can read from all three servers sequentially
* [x] Client handles connection failure gracefully (server unreachable, invalid NodeId)
* [x] `/actuator/prometheus` returns metrics including JVM, request, and custom `sda.*` metrics
* [x] Custom health indicators (ontology, SPARQL, OPC UA) appear in `/actuator/health`
* [x] Log output includes trace and span correlation IDs `[traceId,spanId]`
* [x] Flyway migrations run automatically on application startup
* [x] `flyway_schema_history` table is created in PostgreSQL after first startup
* [x] `options.json` loads successfully on application startup
* [x] `GET /api/v1/options/protocol` returns the list of configured protocols
* [x] `GET /api/v1/options/nonexistent` returns 404

---

## Phase 2: CSS-SDA Ontology Model

**Goal**

Build the CSS-SDA ontology from the paper (Islam, Maurer, Diedrich 2026) and validate it against the paper's primary use case: **Flowmeter Asset-Health Monitoring based on NAMUR NE 107** (Section 4.4). The ontology must support the paper's SPARQL query and SWRL rules. The current stub ontology uses Modbus and a generic pump fill scenario — this phase replaces it with the paper's actual OPC-UA flowmeter diagnostics model.

**Priority: Use Case A (NE 107 Flowmeter) is the MVP. Use Cases B and C are secondary.**

**Tasks**

* [x] **Use Case A (MVP): Flowmeter NE 107 — Ontology Instances**
  * [x] Replace current Modbus pump-fill instances with paper's OPC-UA flowmeter scenario
  * [x] Add `:REQ-001` Requirement (requirementID="REQ-001", title="Flowmeter Asset-Health", requirementType="AssetDiagnostics", lifecycleStatus="Defined")
  * [x] Add `:AssetConditionMonitoringCapability` OfferedCapability (capabilityID="ACM-001")
  * [x] Add `:SK-FlowmeterDiagnostics` Skill (realisedBy → Capability, isImplementedBy → Service)
  * [x] Add `:SVC-FlowmeterDiagnostics` Service (reportedRecall=0.94, reportedLatency=45, image="registry.local/flowmeter-diagnostics:1.0")
  * [x] Add `:CB-Flowmeter-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", securityMode="SignAndEncrypt")
  * [x] Add `:DC-Flowmeter-FT101` DataConnector (informationModel="OPC-UA", semanticID="IEC-61987-42")
  * [x] Add `:DEPLOY-OnPrem` DeploymentContext (onPlantNetwork=true, safetyIntegrityLevel="SIL-0", determinismClass="SoftRealTime")
  * [x] Add negative candidate: `:SVC-WrongProtocol` (HTTP instead of OPC-UA)
  * [x] Add negative candidate: `:SVC-WrongCapability` (FermentationControl, wrong capability chain)
  * [x] Add negative candidate: `:SVC-NE107Failure` (NE 107 status = Failure, excluded by SPARQL)
* [x] **Use Case A (MVP): NE 107 Diagnostic Ontology**
  * [x] Add `sda:NE107Status` class with individuals: `:NE107-Good`, `:NE107-Maintenance`, `:NE107-Failure`
  * [x] Add `sda:NE107SubCode` class with individuals: `:NE107-Sensor`, `:NE107-WireBreak`, `:NE107-ShortCircuit`, `:NE107-Function`
  * [x] Add `sda:hasNE107Status` object property (Device/Variable → NE107Status)
  * [x] Add `sda:hasDiagnosticString` data property
  * [x] Add `sda:hasSubCode` object property (NE107Status → NE107SubCode)
  * [x] Add `sda:DegradedMode` class (subClassOf `css:Skill`)
  * [x] Add `sda:hasDegradationLevel` data property (DegradedMode → xsd:string)
  * [x] Add pump degradation individuals: `:pump1HealthyMode`, `:pump1DegradedMode`, `:pump1FaultedMode`
  * [x] Add `:pump1Asset` FieldDevice with NE 107 status linkage
* [x] **Use Case A (MVP): SPARQL Query (paper Table I)**
  * [x] Rewrite `match-service.rq` to match paper's SPARQL pattern (Section 4.4)
  * [x] R1: capability match chain (REQ → mapsToCapability → Capability → realisedBy → Skill → isImplementedBy → Service)
  * [x] R2b: protocol match (CommunicationBinding.protocol = "OPC-UA", accessMode = "read-only")
  * [x] R3: performance+locality (reportedRecall >= 0.90, reportedLatency <= 60, onPlantNetwork = true)
  * [x] FILTER NOT EXISTS for semantic identifier matching (R2a)
  * [x] ORDER BY DESC(?recall) LIMIT 1
* [x] **Use Case A (MVP): SPARQL Tests**
  * [x] Test: `:REQ-001` returns `:SVC-FlowmeterDiagnostics` (R1-R3 satisfied)
  * [x] Test: `:SVC-WrongProtocol` excluded (protocol mismatch)
  * [x] Test: `:SVC-WrongCapability` excluded (capability mismatch)
  * [x] Test: `:SVC-NE107Failure` excluded (NE 107 status = Failure)
  * [x] Test: pump recovery restores match after failure cleared
* [x] **CSS-SDA Ontology: Classes and Properties**
  * [x] Define Requirement class properties from paper (Figure 5): requirementID, title, description, rationale, acceptanceCondition, requirementType, lifecycleStatus, hasParameter, hasConstraint, hasPriority, specifiedBy, verifiedBy, mapsToCapability
  * [x] Define Capability class properties from paper (Figure 5): capabilityID, capabilityName, description, version, requiresFunction, operatesOn, realisedBy, exposedAs, mapsTo
  * [x] Define physical asset classes: `sda:MTPModule`, `sda:FieldDevice`, `sda:BACnetDevice`, `sda:EnergyMeter`
  * [x] Define object properties: mapsToCapability, realisedBy, isImplementedBy, usesBinding, usesConnector, isDeployedBy
  * [x] Define data properties: protocol, accessMode, reportedRecall, reportedLatency, onPlantNetwork, engineeringUnit, semanticID, nominalRange_min, nominalRange_max
* [x] **Use Case B: Fermentation Skid (Skid-A1) — Critical Process Control**
  * [x] Add `:Skid-A1` MTPModule instance (SIL-2, CPC category, PROFINET, OPC UA gateway)
  * [x] Add `:REQ-002` Requirement (requirementID="REQ-002", title="Fermentation Temperature Control", requirementType="CoreProcessControl")
  * [x] Add `:OC-TempControl` OfferedCapability (capabilityID="TC-001")
  * [x] Add `:OC-DosingControl` OfferedCapability (capabilityID="DC-001")
  * [x] Add `:SK-FermentationControl` Skill (realisedBy → OC-TempControl, isImplementedBy → SVC)
  * [x] Add `:SK-DosingControl` Skill (realisedBy → OC-DosingControl, isImplementedBy → SVC)
  * [x] Add `:SVC-FermentationSkid` Service (SIL-2, HardRealTime)
  * [x] Add `:CB-Skid-A1-PROFINET` CommunicationBinding (protocol="PROFINET", accessMode="read-write")
  * [x] Add `:CB-Skid-A1-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", gateway)
  * [x] Add `:DC-Skid-A1-OPCUA` DataConnector (informationModel="OPC-UA", semanticID from MTP)
  * [x] Add `:DEPLOY-Critical` DeploymentContext (safetyIntegrityLevel="SIL-2", determinismClass="HardRealTime", networkZone="OT-Zone-1", onPlantNetwork=true)
  * [x] Add SPARQL test: REQ-002 matches SVC-FermentationSkid via PROFINET binding
  * [x] Add SPARQL test: wrong SIL (SIL-0) excludes service for SIL-2 requirement
  * [x] Add SPARQL test: wrong determinism class (SoftRealTime) excludes for HardRealTime requirement
* [x] **Use Case C: HVAC Plant (AHU-01 + EM-01) — Non-Critical Monitoring & Optimization**
  * [x] Add `:AHU-01` FieldDevice instance (BACnet, M+O category, SoftRealTime, OT-Zone-3)
  * [x] Add `:EM-01` EnergyMeter instance (native OPC UA, non-critical)
  * [x] Add `:REQ-003` Requirement (requirementID="REQ-003", title="HVAC Temperature Monitoring", requirementType="MonitoringOptimization")
  * [x] Add `:OC-HVACTempMonitoring` OfferedCapability (capabilityID="HVAC-001")
  * [x] Add `:SK-HVACTempAnalysis` Skill (realisedBy → OC-HVACTempMonitoring)
  * [x] Add `:SVC-HVACTempAnalysis` Service (SoftRealTime, on-premise)
  * [x] Add `:CB-AHU01-BACnet` CommunicationBinding (protocol="BACnet", accessMode="read-only", securityMode="Sign")
  * [x] Add `:CB-AHU01-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", gateway)
  * [x] Add `:DC-AHU01-OPCUA` DataConnector (informationModel="OPC-UA", gateway)
  * [x] Add `:CB-EM01-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", securityMode="SignAndEncrypt", native)
  * [x] Add `:DC-EM01-OPCUA` DataConnector (informationModel="OPC-UA", native)
  * [x] Add `:DEPLOY-OTZone3` DeploymentContext (safetyIntegrityLevel="SIL-0", determinismClass="SoftRealTime", networkZone="OT-Zone-3", onPlantNetwork=true)
  * [x] Add SPARQL test: REQ-003 matches SVC-HVACTempAnalysis
  * [x] Add SPARQL test: cross-plant query finds temperature services across PlantA and PlantB
  * [x] Add SPARQL test: wrong networkZone excludes service
* [x] **ExplorerView UI**
  * [x] Create `ExplorerView` with split layout (tree left, details right)
  * [x] Create `OntologyTree` component with expandable/collapsible nodes
  * [x] Add Vaadin UI test for ExplorerView rendering and tree interaction

**Acceptance Criteria**

* [x] **Use Case A (MVP): Ontology supports paper's SPARQL query (Table I) without modification**
* [x] **Use Case A (MVP): SPARQL returns `:SVC-FlowmeterDiagnostics` for `:REQ-001`**
* [x] **Use Case A (MVP): SPARQL excludes `:SVC-WrongProtocol` (protocol mismatch)**
* [x] **Use Case A (MVP): SPARQL excludes `:SVC-WrongCapability` (capability mismatch)**
* [x] **Use Case A (MVP): SPARQL excludes `:SVC-NE107Failure` (NE 107 status = Failure)**
* [x] **Use Case A (MVP): Pump recovery restores SPARQL match after failure cleared**
* [x] **Use Case B: SPARQL returns `:SVC-FermentationSkid` for `:REQ-002` via PROFINET binding**
* [x] **Use Case B: SPARQL excludes service when SIL requirement mismatch (SIL-0 vs SIL-2)**
* [x] **Use Case B: SPARQL excludes service when determinism class mismatch (SoftRealTime vs HardRealTime)**
* [x] **Use Case C: SPARQL returns `:SVC-HVACTempAnalysis` for `:REQ-003`**
* [x] **Use Case C: Cross-plant query finds temperature services across PlantA and PlantB**
* [x] **Use Case C: SPARQL excludes service when networkZone mismatch**
* [x] Ontology file is valid Turtle (Jena loads without error)
* [x] Ontology contains all 8 CSS-SDA classes (UseCase, Requirement, Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext)
* [x] Ontology contains NE 107 status individuals (Good, Maintenance, Failure) and sub-codes
* [x] Ontology contains DegradedMode class with hasDegradationLevel property
* [x] Ontology contains Requirement class with paper's properties (Figure 5)
* [x] Ontology contains Capability class with paper's properties (Figure 5)
* [x] Ontology contains all three use cases (A: Flowmeter, B: Fermentation, C: HVAC)
* [x] ExplorerView shows CSS-SDA class hierarchy in tree sidebar
* [x] Tree nodes are expandable and collapsible
* [x] Selecting a tree node displays entity details in the right panel
* [x] ExplorerView UI test validates tree rendering and node selection

---

## Phase 3: Ontology Loading with Apache Jena

**Goal**

Implement Java code that loads the ontology model into Apache Jena.

**Tasks**

* [x] Create `OntologyModelLoader`
* [x] Load ontology file from classpath
* [x] Return Apache Jena `Model`
* [x] Add clear error when ontology file is missing
* [x] Add clear error when ontology file is invalid
* [x] Add unit test for successful ontology loading
* [x] Add unit test for missing ontology file
* [x] Add unit test for invalid ontology content
* [x] Wire `ExplorerView` to `OntologyModelLoader` — tree loads dynamically from Jena model
* [x] Add entity detail tabs: Overview, Properties, Relationships
* [x] Add Vaadin UI test for ExplorerView dynamic tree loading and tab switching

**Acceptance Criteria**

* [x] Ontology loads successfully during application startup or first use
* [x] Invalid ontology input fails with a clear exception
* [x] Tests cover success and failure cases
* [x] ExplorerView ontology tree populates dynamically from loaded model
* [x] Selecting an entity shows Properties and Relationships tabs
* [x] Missing ontology file shows user-friendly error in ExplorerView
* [x] ExplorerView UI test validates dynamic loading and tab interaction

---

## Phase 4: Input File Parsers — MTP AML and OPC UA Server Datasheet

**Goal**

Implement parsers that read vendor-supplied device description files (MTP AML, OPC UA Server Datasheet) and generate CSS-SDA ontology instances (ABox triples) as specified in the five-step procedure (Steps 1–3: Parse, Instantiate, Lift).

**Tasks**

* [x] Create `MTPAmlParser` for VDI/VDE/NAMUR 2658 AML archives:
  * [x] Parse `<Service>` blocks into `css:Skill` + `css:SkillParameter` instances with UCUM units and IEC CDD IRDI identifiers
  * [x] Parse `<Communication>/<OpcUaServer>` into `sda:CommunicationBinding` triples (endpoint, security mode, policy, NodeSet)
  * [x] Parse `<DataAssembly>/<OpcUaNode>` into `sda:DataConnector` triples
  * [x] Handle missing or invalid AML structure with clear errors
* [x] Create `UaServerDatasheetParser` for IDTA 02009-1-0 AAS submodels:
  * [x] Parse `Configuration/NodeSets` into `sda:DataConnector`
  * [x] Parse `EndpointDescriptions` into `sda:CommunicationBinding` (endpointUri, securityMode, securityPolicy, securityLevel, transportProfile, discoveryUrl)
  * [x] Parse `Identification` into device metadata (productUri, softwareVersion)
  * [x] Handle missing or invalid datasheet structure with clear errors
* [x] Create `ParserFactory` to select parser based on input file type
* [x] Add test MTP AML example file under `src/test/resources/examples/`
* [x] Add test OPC UA Server Datasheet example file under `src/test/resources/examples/`
* [x] Add unit tests for MTP AML parsing (valid, missing elements, invalid XML)
* [x] Add unit tests for OPC UA Server Datasheet parsing (valid, missing elements, invalid JSON)
* [x] Add integration test that loads parsed triples into Apache Jena Model
* [x] Create `UploadView` with drag-and-drop upload panel:
  * [x] Support OPC UA NodeSet, AAS, MTP, AutomationML, RDF, OWL, JSON-LD, YAML
  * [x] Upload progress bar and validation results display
  * [x] Preview parsed triples after successful upload
* [x] Add Vaadin UI test for UploadView file selection, validation, and progress display

**Acceptance Criteria**

* [x] MTP parser generates valid CSS-SDA triples from a real AML source
* [x] OPC UA Datasheet parser generates valid CSS-SDA triples from a real datasheet source
* [x] Parsers produce `CommunicationBinding` triples with endpoint, security mode, and security policy
* [x] MTP parser produces `css:Skill` + `css:SkillParameter` with UCUM units and IRDI
* [x] Datasheet parser produces `CommunicationBinding` with securityLevel, transportProfile, discoveryUrl
* [x] Both parsers fail with clear errors on invalid input
* [x] Parsed triples can be loaded into the same Apache Jena Model as the ontology schema
* [x] UploadView accepts valid file types and rejects invalid types with clear messages
* [x] UploadView shows progress bar during upload
* [x] UploadView previews parsed triples on success
* [x] UploadView UI test validates file type acceptance, rejection, and progress display

---

## Phase 5: SPARQL Matching Query

**Goal**

Create SPARQL queries that select a service satisfying a requirement by matching across all CSS-SDA dimensions simultaneously (Capability, CommunicationBinding, DeploymentContext). Follow the PDF's multi-dimensional matching pattern.

**Tasks**

* [x] Create `src/main/resources/queries/match-service.rq`
* [x] Match requirement to capability via `sda:functionClass`, `sda:engineeringUnit`, `sda:nominalRange`, `sda:safetyIntegrityLevel`
* [x] Match capability to skill (css:Skill via css:offersUseOf)
* [x] Match skill to service
* [x] Match required protocol via `sda:CommunicationBinding/sda:protocol`
* [x] Match required access mode
* [x] Match data connector (informationModel, nodeSetFile)
* [x] Match deployment context (networkZone, safetyIntegrityLevel, determinismClass)
* [x] Filter by minimum recall
* [x] Filter by maximum latency
* [x] Order candidates by highest recall
* [x] Limit result to one best service
* [x] Add query test with Apache Jena
* [x] Create global `SearchBar` component:
  * [x] AI-style semantic search with autocomplete, fuzzy search, exact search, recent searches
  * [x] Filters: Requirement, Capability, Skill, Service, Deployment, Asset
  * [x] Add `SearchBar` to `MainLayout` header
* [x] Create `SearchView` with results Grid:
  * [x] Columns: Name, Type, Description, Ontology Path, Similarity Score, Linked Services, Actions
  * [x] Sorting, filtering, pagination, column chooser, CSV/Excel export, context menu
* [x] Add Vaadin UI test for SearchBar autocomplete and SearchView result grid interaction

**Acceptance Criteria**

* [x] Query returns `FlowmeterDiagnosticsService` for `REQ_001`
* [x] Query excludes service with wrong capability (functionClass mismatch)
* [x] Query excludes service with wrong protocol
* [x] Query excludes service with excessive latency
* [x] Query excludes service with incompatible deployment context
* [x] Query evaluates CommunicationBinding, DataConnector, and DeploymentContext simultaneously
* [x] SearchBar shows autocomplete suggestions as user types
* [x] SearchView displays results Grid with correct columns
* [x] Filters scope search to selected entity type
* [x] Results support sorting and pagination
* [x] SearchBar and SearchView UI tests validate autocomplete and grid interaction

---

## Phase 6: Ontology Inference — OWL Reasoner

**Goal**

Implement the OWL reasoner step (Step 4: Infer from the five-step procedure) that derives `sda:OfferedCapability` instances from `css:SkillParameter` semantics. This is the key architectural novelty — the capability is inferred, not parsed.

**Tasks**

* [x] Create `OntologyInferenceService`
* [x] Configure OWL reasoner (e.g., Apache Jena OWL_MEM_MICRO_RULES_INF or EL reasoner)
* [x] Define OWL axiom A4 for capability derivation: SkillParameter (engineeringUnit, nominalRange, functionClass) → OfferedCapability
* [x] Register `OfferedCapability` and `RequiredCapability` in the ontology
* [x] Run inference after parser-generated triples are loaded
* [x] Ensure inference does not overwrite existing explicit `OfferedCapability` triples
* [x] Add unit test: parser-generated triples produce expected `OfferedCapability` after inference
* [x] Add unit test: changing SkillParameter semantics changes inferred capability
* [x] Create `DashboardView` with animated cards:
  * [x] Registered Services, Capabilities, Skills, Semantic Mappings
  * [x] Each card with icon, title, value, trend, status color; clickable to navigate
* [x] Create `DashboardCard` component
* [x] Wire DashboardView to `OntologyInferenceService` for live counts
* [x] Add Vaadin UI test for DashboardView card rendering and navigation

**Acceptance Criteria**

* [x] Inferred `OfferedCapability` instance has correct `functionClass`, `engineeringUnit`, and `nominalRange`
* [x] Inference preserves existing explicit capability triples
* [x] Changing SkillParameter input changes inferred capability output
* [x] Inference completes within acceptable time for the MVP dataset
* [x] Reasoner can be enabled/disabled for testing without inference
* [x] DashboardView loads and displays animated cards with correct backend data
* [x] Cards show live counts from inference results
* [x] Clicking a card navigates to the corresponding view
* [x] DashboardView UI test validates card rendering and click navigation

---

## Phase 7: Semantic Reasoner Service

**Goal**

Implement the application service that runs the SPARQL matching query and returns a Java result object.

**Tasks**

* [x] Create `SemanticReasonerService`
* [x] Create `SparqlQueryLoader`
* [x] Create `ServiceMatchResult`
* [x] Execute SPARQL query against ontology model
* [x] Support matching by `requirementId`
* [x] Extract selected service name
* [x] Extract container image
* [x] Extract capability
* [x] Extract protocol
* [x] Extract access mode
* [x] Extract connector
* [x] Extract deployment context
* [x] Extract recall and latency values
* [x] Return no-match result when query has no result
* [x] Add unit tests for match and no-match scenarios
* [x] Wire DashboardView cards to `SemanticReasonerService` — live match counts and statistics
* [x] Create `StatisticsPanel` component for match/success/failure rates
* [x] Add Vaadin UI test for DashboardView statistics display

**Acceptance Criteria**

* [x] Reasoner returns a complete result for `REQ_001`
* [x] Reasoner returns no-match for unsatisfied requirements
* [x] Reasoner does not hardcode service selection in Java
* [x] Matching behavior is controlled by ontology data and SPARQL rules
* [x] Tests prove that changing ontology data changes the selected service without Java code changes
* [x] DashboardView shows live match statistics from reasoner
* [x] DashboardView statistics UI test validates correct count display

---

## Phase 8: REST API

**Goal**

Expose requirement matching through a REST endpoint.

**Tasks**

* [x] Create `RequirementMatchingController`
* [x] Create `MatchRequirementRequest` supporting two modes:
  * [x] Mode A: `{ "requirementId": "REQ_001" }` — lookup existing requirement from ontology
  * [x] Mode B: inline fields (`functionClass`, `protocol`, `accessMode`, `engineeringUnit`, `safetyIntegrityLevel`, `networkZone`, `determinismClass`, `minRecall`, `maxLatencyMs`, `nominalRange_min`, `nominalRange_max`) — match on-the-fly without persisting a requirement
* [x] Create `MatchRequirementResponse`
* [x] Add request validation (requirementId OR inline fields required, not both)
* [x] Add endpoint `POST /api/v1/requirements/match`
* [x] Add endpoint `POST /api/v1/requirements` to persist a new requirement to the ontology
* [x] Return matched service details
* [x] Return no-match response when no service satisfies the requirement
* [x] Add global exception handling
* [x] Add integration tests with MockMvc or WebTestClient (both modes)
* [x] Create `RegistrationWizardView` (multi-step):
  * [x] Step 1: General Information
  * [x] Step 2: Communication
  * [x] Step 3: Capabilities (dropdowns populated from `GET /api/v1/options/{field}`)
  * [x] Step 4: Skills
  * [x] Step 5: Deployment
  * [x] Step 6: Semantic Mapping
  * [x] Step 7: Review
  * [x] Step 8: Register
  * [x] Navigation: Previous, Next, Save Draft, Cancel
  * [x] On Register, call REST API to persist new service in ontology model
* [x] Add Vaadin UI test for RegistrationWizardView step navigation and form interaction
* [x] Create `RequirementFormView` for creating and matching requirements:
  * [x] Dropdown: functionClass (from `GET /api/v1/options/functionClass`)
  * [x] Dropdown: protocol (from `GET /api/v1/options/protocol`)
  * [x] Dropdown: accessMode (from `GET /api/v1/options/accessMode`)
  * [x] Dropdown: engineeringUnit (from `GET /api/v1/options/engineeringUnit`)
  * [x] Dropdown: safetyIntegrityLevel (from `GET /api/v1/options/safetyIntegrityLevel`)
  * [x] Dropdown: networkZone (from `GET /api/v1/options/networkZone`)
  * [x] Dropdown: determinismClass (from `GET /api/v1/options/determinismClass`)
  * [x] Numeric input: nominalRange_min
  * [x] Numeric input: nominalRange_max
  * [x] Numeric input: minRecall (0.0–1.0)
  * [x] Numeric input: maxLatencyMs
  * [x] Button: "Match" — calls `POST /api/v1/requirements/match` with inline fields
  * [x] Button: "Save Requirement" — calls `POST /api/v1/requirements` to persist, then match by ID
  * [x] Results panel: shows matched service details or no-match message
  * [x] All dropdown options loaded from configurable `options.json` via `GET /api/v1/options/{field}`
* [x] Add Vaadin UI test for RequirementFormView dropdown loading, form fill, and match result display

**Acceptance Criteria**

* [x] API accepts a requirement ID (Mode A)
* [x] API accepts inline requirement fields (Mode B)
* [x] API returns selected service for valid matching requirement
* [x] API returns no-match response for unsatisfied requirement
* [x] API returns validation error when neither requirementId nor inline fields are provided
* [x] API returns validation error when both requirementId and inline fields are provided
* [x] `POST /api/v1/requirements` persists a new requirement to the ontology
* [x] Integration tests pass for both modes
* [x] RegistrationWizardView navigates through all 8 steps without errors
* [x] RegistrationWizardView calls REST API on final Register step
* [x] Draft saving preserves form state across sessions
* [x] RegistrationWizardView UI test validates all steps and final registration
* [x] RequirementFormView loads all dropdown options from `GET /api/v1/options/{field}`
* [x] RequirementFormView dropdowns are populated from configurable `options.json`
* [x] RequirementFormView "Match" button returns correct service or no-match
* [x] RequirementFormView "Save Requirement" persists and then matches
* [x] RequirementFormView UI test validates dropdown loading, form interaction, and match results

---

## Phase 9: Docker Compose and Kubernetes Manifest Generation

**Goal**

Generate deployment manifests for the selected service. Support both Docker Compose (for edge/fieldbus scenarios) and Kubernetes (with nodeSelector constraints derived from DeploymentContext triples as described in the PDF).

**Tasks**

* [x] Create `DockerComposeManifestService`
* [x] Create `KubernetesManifestService`
* [x] Create `ManifestResult`
* [x] Generate container service name from selected service
* [x] Add container image configuration from selected service
* [x] Docker Compose: Add `restart: unless-stopped` baseline edge policy configuration
* [x] Docker Compose: Add environment parameters block for asset ID, process variable, protocol, and deployment context
* [x] Docker Compose: Configure `network_mode: host` only if justified for the edge/fieldbus scenario
* [x] Kubernetes: Generate Pod or Deployment spec from selected service
* [x] Kubernetes: Derive `nodeSelector` constraints from `sda:DeploymentContext` triples (safetyIntegrityLevel, determinismClass, networkZone)
* [x] Kubernetes: Add container environment variables from CommunicationBinding and DataConnector
* [x] Add unit tests using SnakeYAML to parse and verify generated Docker Compose YAML
* [x] Add unit tests using SnakeYAML or official K8s client to verify generated Kubernetes YAML
* [x] Add example file output under `docker/examples/` and `k8s/examples/`
* [x] Create `DeploymentView`:
  * [x] Cards per environment: Development, Testing, Production, Edge, Cloud
  * [x] Each card with status, services, assets, health, region, version
  * [x] Click to view generated Docker Compose and Kubernetes manifests
* [x] Create `ManifestDisplay` component with syntax-highlighted YAML viewer
* [x] Add Vaadin UI test for DeploymentView card rendering and manifest display

**Acceptance Criteria**

* [x] Matching API response includes valid Docker Compose YAML
* [x] Matching API response includes valid Kubernetes YAML
* [x] YAML contains selected service name and specific target image
* [x] YAML includes injected operational runtime metadata from CommunicationBinding and DeploymentContext
* [x] Kubernetes YAML contains `nodeSelector` with `sda.ontology/sil`, `sda.ontology/deterministic`, `sda.ontology/zone`
* [x] YAML can be parsed successfully using SnakeYAML
* [x] Tests verify runtime property mappings
* [x] DeploymentView shows environment cards with status
* [x] Clicking a card displays generated manifests
* [x] DeploymentView UI test validates environment cards and manifest viewer

---

## Phase 10: Matching Explanation

**Goal**

Make the result explainable so a user can understand why a service was selected.

**Tasks**

* [x] Add `matchingExplanation` field to API response
* [x] Include capability match reason
* [x] Include protocol match reason
* [x] Include access mode match reason
* [x] Include latency constraint reason
* [x] Include recall constraint reason
* [x] Include deployment context reason
* [x] Add explanation for no-match cases where possible
* [x] Add tests for explanation content
* [x] Add explanation panel to `DashboardView` showing match/no-match reasons per dimension
* [x] Create `NotificationCenter` with grouped notifications:
  * [x] Success, Information, Warning, Error, Background Tasks
* [x] Create `NotificationPanel` component for header notification bell
* [x] Add Vaadin UI test for DashboardView explanation panel and NotificationCenter interaction

**Acceptance Criteria**

* [x] Successful match response explains why the service was selected
* [x] No-match response explains that constraints were not satisfied
* [x] Explanation is understandable without reading the SPARQL query
* [x] Tests verify explanation content for successful and failed matches
* [x] DashboardView shows explanation panel after match
* [x] NotificationCenter groups notifications by type
* [x] Explanation panel and NotificationCenter UI tests validate interaction

---

## Phase 11: Evaluation Dataset — Fermentation Skid and HVAC Plant

**Goal**

Create evaluation datasets based on the two real-world PDF examples: the fermentation skid (critical CPC, SIL-2, MTP input) and the HVAC plant (non-critical M+O, OPC UA Server Datasheet input). Include cross-plant semantic queries.

**Tasks**

* [x] Add Example A: Fermentation Skid (Skid-A1)
  * [x] Add MTP AML example file in `src/test/resources/examples/mtp-fermentation-skid.xml`
  * [x] Add expected CSS-SDA triples for CommunicationBinding, DataConnector, DeploymentContext
  * [x] Add FermentationControl and DosingControl css:Skill + css:SkillParameter instances
  * [x] Add inferred OfferedCapability_TempControl and OfferedCapability_Dosing
  * [x] Add RequiredCapability for batch step 3 (temperature-controlled fermentation)
  * [x] Add positive match test (query returns FermentationControl)
  * [x] Add negative match tests (wrong SIL, wrong zone, wrong protocol)
* [x] Add Example B: HVAC Plant (AHU-01 + EM-01)
  * [x] Add OPC UA Server Datasheet example file in `src/test/resources/examples/uaserver-ahu01.json`
  * [x] Add expected CSS-SDA triples for AHU-01 CommunicationBinding (Sign, Basic128Rsa15, discoveryUrl)
  * [x] Add expected CSS-SDA triples for EM-01 CommunicationBinding (SignAndEncrypt, Basic256Sha256)
  * [x] Add deployment context with SoftRealTime, SIL-0, OT Zone 3
  * [x] Add positive match test for HVAC energy optimisation query
  * [x] Add negative match test (wrong determinism class, wrong zone)
* [x] Add cross-plant semantic query (PDF Listing B.7): find all temperature services across both plants
* [x] Add flowmeter diagnostics requirement `REQ_001` for backward compatibility
* [x] Document expected matching results for all evaluation scenarios

**Acceptance Criteria**

* [x] Evaluation dataset contains both fermentation skid and HVAC plant
* [x] Fermentation skid query returns expected service with endpoint, SIL, and zone
* [x] HVAC plant query returns expected device with CommunicationBinding properties
* [x] Cross-plant query returns temperature services from both plants in one query
* [x] No-match tests pass for wrong SIL, wrong zone, wrong protocol
* [x] Flowmeter `REQ_001` still matches correctly for backward compatibility
* [x] README documents evaluation scenarios with expected results
* [x] Create `MonitoringView` with real-time widgets:
  * [x] CPU, Memory, OPC UA Sessions, Connected Assets, Semantic Queries, Notifications
  * [x] ApexCharts with live updates
  * [x] Wire charts to `/actuator/prometheus` metrics or Micrometer MeterRegistry directly
  * [x] Add metric counters for match requests, parser invocations, inference runs, manifest generations
* [x] Wire NotificationCenter to system events (match, upload, inference, manifest generation)
* [x] Add DashboardView cards for Digital Twins, Deployments, Running Simulations, Live OPC UA Sessions
* [x] Add summary-card navigation: OPC UA Sessions, Connected Assets
* [x] Add Vaadin UI test for MonitoringView chart rendering and NotificationCenter event display

**Acceptance Criteria**

* [x] MonitoringView charts reflect current backend state
* [x] NotificationCenter receives events from all backend services
* [x] DashboardView visualizes all 8 metrics with live data
* [x] All three use cases (flowmeter, fermentation, HVAC) render correctly in evaluation
* [x] MonitoringView and NotificationCenter UI tests validate chart display and event grouping

---

## Phase 11b: OPC UA Data Visualization Client

**Goal**

Create a real-time Vaadin UI view that connects to all three OPC UA simulation servers (Approach A: Node-RED, Approach B: Node.js Tank, Approach C: Java Milo) and visualizes their data in a unified dashboard. This serves as an OPC UA visual client for monitoring tank process, pump diagnostics, and fermentation/HVAC variables live.

**Tasks**

* [x] Create `OpcUaVisualizationView` with Vaadin route `/opcua-visualization`
  * [x] Split layout: server selector sidebar + main dashboard area
  * [x] Server selector: tab or dropdown to switch between Approach A (Node-RED), B (Node.js Tank), C (Java Milo)
  * [x] Connection status indicator per server (green/red badge)
  * [x] Auto-reconnect on server disconnect
* [x] **Approach C (Java Milo) — Tank + Pump Dashboard** (primary, always available)
  * [x] Tank level gauge (0–100 cm) with animated fill
  * [x] Tank level trend chart (ApexCharts, rolling 60s window)
  * [x] Flow rate in/out gauges (L/min)
  * [x] Temperature gauge with color coding (normal/warning/critical)
  * [x] Pump vibration bar chart with NE 107 status color overlay
  * [x] Pump temperature gauge with NE 107 diagnostic overlay
  * [x] Pump health status card (Healthy / Early Wear / Seal Leak / Impeller Erosion / Failure)
  * [x] NE 107 device status badge (Good = green, Maintenance = yellow, FunctionCheck = blue, Failure = red)
  * [x] Set inlet flow control slider (writes via OPC UA SetInletFlow method)
* [x] **Approach B (Node.js Tank) — External Tank Dashboard**
  * [x] Tank level gauge + trend chart (same variables: Level, FlowRateIn, FlowRateOut, Temperature)
  * [x] Connection status indicator (server may not be running)
  * [x] Graceful "server unavailable" placeholder when not connected
* [x] **Approach A (Node-RED) — Fermentation + HVAC Dashboard**
  * [x] Fermentation temperature display (ns=2 Fermentation.TempSetpoint)
  * [x] HVAC temperature display (ns=3 HVAC.SupplyAirTemp)
  * [x] Connection status indicator
  * [x] Graceful "server unavailable" placeholder when not connected
* [x] **Shared components**
  * [x] `OpcUaGauge` component — configurable min/max/unit/color thresholds
  * [x] `OpcUaTrendChart` — ApexCharts line chart with auto-scrolling time axis
  * [x] `OpcUaStatusBadge` — NE 107 color-coded status indicator
  * [x] `OpcUaConnectionPanel` — connection status, endpoint URL, latency display
* [x] **Backend service**
  * [x] Create `OpcUaVisualizationService` — periodic polling (1s interval) of all three servers
  * [x] Use existing `OpcUaClientWrapper` / `OpcUaClientFactory` for connections
  * [x] Publish reactive stream of variable snapshots to UI via `@Push` or `SseEmitter`
  * [x] Handle connection failures gracefully per server (don't block other servers)
* [x] **Navigation**
  * [x] Add "OPC UA Monitor" entry to `MainLayout` navigation drawer under Monitoring section
  * [x] Add icon and badge showing number of connected servers
* [x] **Tests**
  * [x] Add Vaadin UI test: `OpcUaVisualizationView` renders with all three server tabs
  * [x] Add Vaadin UI test: gauge components display correct values
  * [x] Add Vaadin UI test: trend chart renders and scrolls
  * [x] Add Vaadin UI test: connection status badge updates
  * [x] Add Vaadin UI test: SetInletFlow slider sends write command
  * [x] Add Vaadin UI test: graceful handling when server is unavailable

**Acceptance Criteria**

* [x] `OpcUaVisualizationView` loads at `/opc-ua-visualization` after login
* [x] Java Milo server (Approach C) data displays in real-time without page refresh
* [x] Tank level gauge animates and trend chart scrolls with live data
* [x] Pump vibration and temperature show NE 107 diagnostic color overlays
* [x] NE 107 status badge reflects current degradation phase
* [x] SetInletFlow slider writes to OPC UA and level changes within 2 seconds
* [x] Node.js Tank (Approach B) shows data when server is running, graceful placeholder when not
* [x] Node-RED (Approach A) shows data when server is running, graceful placeholder when not
* [x] Connection status indicators correctly show green/red per server
* [x] All three server views accessible via tab/selector without page reload
* [x] Vaadin UI tests pass for rendering, gauge values, chart scroll, and connection status
* [x] Polling interval is configurable via `application.yml`

---

## Phase 11c: Semantic Mapping Hub

**Goal**

Add a universal mapping hub at `/mapping-hub` that runs the full PARSE → LIFT → INFER → MATCH → MANIFEST pipeline over vendor device descriptions (AAS JSON/XML, MTP AML, OPC UA Server Datasheet). Uploads enrich a session-scoped canonical model incrementally; gap analysis, supplementary-format recommendations and manual entry close coverage gaps; a complete JSON manifest is downloadable at any time.

**Tasks**

* [x] **Canonical model**
  * [x] Define `CanonicalEntity` contract (`className`, `id`, `name`, `sourceFileName`, `inferred`, `attributes`)
  * [x] Add 25 canonical entity records mapped to the 24-class TBox (Asset, Skill, SkillParameter, CommunicationBinding, SemanticConcept, InformationModel, ProcessVariable, Sensor, Observation, UseCase, Requirement, Stakeholder, Capability, Service, ServiceInstance, DeploymentContext, ClusterNode, DeploymentPolicy, NetworkZone, SafetyClass, DataConnector, VerificationCriterion, RequirementConstraint, RequirementParameter, + interface)
  * [x] Add `CanonicalAssetModel` with typed lists, dedup merge, per-class queries and `allEntities()`
  * [x] Add `AssetFileFormat` (AAS_XML, AAS_JSON, MTP_AML, OPCUA_JSON), `EntityOrigin`, `CoverageStatus`, `CoverageEntry`, `GapReport`, `BlockingGap`, `Recommendation`, `UploadedFileInfo`, `Provenance`, `Attributes`
* [x] **Parsers**
  * [x] `HubFileTypeDetector` — sniffs content (XML root, AAS JSON, OPC UA datasheet keys) with extension fallback
  * [x] `CanonicalParserRegistry` — finds parsers by format, throws clear errors for unregistered formats
  * [x] `AasModelParser`, `MtpModelParser`, `OpcUaModelParser` — parse fixtures into canonical entities with provenance
  * [x] `ParsingException` with file name and clear message
* [x] **Pipeline services**
  * [x] `MappingHubService` orchestrating PARSE → LIFT → INFER → MATCH → MANIFEST with per-stage console logging
  * [x] `LiftService` — semantic concepts per data node, information models per asset
  * [x] `InferenceService` — sensor per process variable, capability per skill, ServiceInstance/ClusterNode/NetworkZone/DeploymentContext per binding, all marked inferred
  * [x] `MatchingService` — coverage over all 24 TBox classes with weighted percentage (1 decimal), PRESENT/PARTIAL/INFERRED/MISSING statuses, missing-field detail, blocking gaps for core relations, source tracking
  * [x] `RecommendationService` — recommends missing formats that can fill gaps, excludes already-uploaded formats, sorted by gap fill count
  * [x] `ManifestExportService` — JSON manifest with metadata, coverage, entity groups, files, blocking gaps and recommendations
  * [x] `MappingHubSession` (`@VaadinSessionScope`) — incremental model accumulation, manual entities, inferred entities, file history
  * [x] `PipelineResult` — model, inferred entities, gap report, recommendations, console lines
* [x] **Hub UI (`/mapping-hub`, `MainLayout` drawer)**
  * [x] Header "Semantic Mapping Hub" + "Parse → Lift → Infer → Match → Manifest" subtitle
  * [x] Pipeline stepper, monospace console, coverage dashboard (4 status cards + coverage % + segment progress bar)
  * [x] Layer tabs with missing-count badges, class list with status dots, class detail panel
  * [x] Class detail: status header, provenance lines, raw entity cards, gap actions
  * [x] Gap actions: format-restricted supplementary upload dropzone + manual entry toggle with per-class field lists
  * [x] Recommendations bar with inline upload buttons per recommended format
  * [x] Action bar: Run Pipeline, Reset, Export Complete Manifest (JSON download via data URI)
  * [x] Dark-palette stylesheet `src/main/frontend/hub-styles.css`
* [x] **Session-scope integration fix**
  * [x] Inject `MappingHubSession` into the singleton `MappingHubService` via `@Lazy` scoped proxy so the application context loads in `@SpringBootTest` (Vaadin session scope is only active during a UI request)
* [x] **Tests (12 classes, 71 tests)**
  * [x] Parser tests: `HubFileTypeDetectorTest`, `CanonicalParserRegistryTest`, `AasModelParserTest`, `MtpModelParserTest`, `OpcUaModelParserTest`
  * [x] Service tests: `LiftServiceTest`, `InferenceServiceTest`, `MatchingServiceTest`, `RecommendationServiceTest`, `ManifestExportServiceTest`, `MappingHubServiceTest`
  * [x] UI tests: `MappingHubViewTest` (route, title, stylesheet, dashboard, class list, layer tabs, console, detail refresh)

**Acceptance Criteria**

* [x] Uploading `pump-aas.json` runs the full pipeline: 62.5% coverage, 12 PRESENT, 6 INFERRED, 6 MISSING, 5 `[STAGE]` console lines
* [x] Every layer shows its missing-class count; all 24 TBox classes appear in the class list
* [x] Selecting a class shows status, provenance, raw entity data and gap actions
* [x] Manual entry forms expose the exact per-class field lists (Stakeholder, UseCase, Requirement, DeploymentContext, ClusterNode, NetworkZone, Sensor, Capability, ServiceInstance)
* [x] Recommendations exclude formats already uploaded and sort by gap fill count
* [x] Supplementary OPC UA upload enriches the session model (2 files, 2 formats) without duplicating re-run state
* [x] Export Complete Manifest produces a JSON manifest containing coverage and entity groups
* [x] Reset clears all session state
* [x] Inferred entities are marked `inferred` (detail panel "Inferred from" pills)
* [x] Unsupported uploads fail with a clear `ParsingException`
* [x] `mvn clean test` — BUILD SUCCESS, 414 tests, 0 failures (was 343; +71 hub tests)

---

## Phase 11c-2: Semantic Infer and Match Alignment (paper Steps 4–5)

**Goal**

Align the hub's INFER and MATCH stages with the paper: the INFER stage derives `sda:OfferedCapability` from `SkillParameter` semantics (Axiom A4 — functionClass, engineeringUnit, nominalRange, SIL), and the MATCH stage runs a SPARQL query that compares each inferred `OfferedCapability` against each `RequiredCapability` across the capability, communication and deployment CSS-SDA dimensions simultaneously, projecting the matched skill, endpoint, protocol, network zone and SIL.

**Tasks**

* [x] Ontology `css-sda-unified.ttl`: add `OfferedCapability` and `RequiredCapability` classes (sub-classes of `Capability`) plus datatype properties `functionClass`, `engineeringUnit`, `nominalRange_min`, `nominalRange_max`, `accuracy`, `safetyIntegrityLevel`, `networkZone`, `protocol`, `accessMode`, `endpointUrl`
* [x] `SkillParameter` gains `functionClass`; MTP and AAS parsers read it and the `pump-module-mtp.xml` / `pump-aas.json` fixtures carry `FunctionClass`
* [x] Canonical records `OfferedCapability`, `RequiredCapability`, `CapabilityMatch`; `CanonicalAssetModel` supports all three; `DeploymentContext` gains `networkZone`
* [x] `InferenceService` derives an `OfferedCapability` per skill from its primary parameter (functionClass-bearing first) while keeping the generic `Capability` for the coverage ledger
* [x] SPARQL asset `queries/hub-match-offered-required.rq` matching offered vs required across all three dimensions
* [x] `SemanticMatchService` instantiates the canonical model + inferred entities into an in-memory Jena ABox and executes the match query
* [x] `MappingHubService` wires the semantic match stage into `runStages`; `PipelineResult` carries `matches` and logs each match
* [x] Tests: `SemanticMatchServiceTest` (positive + negative scenarios: functionClass, range containment, unit, SIL, protocol, zone), `InferenceServiceTest` offered-capability derivation, `MtpModelParserTest` functionClass, `OntologyAndQueryLoadingTest` vocabulary + query parse

**Acceptance Criteria**

* [x] A `RequiredCapability` matches an `OfferedCapability` with equal functionClass, compatible unit, contained nominal range, sufficient SIL, matching protocol and matching network zone
* [x] Negative scenarios (different functionClass / non-contained range / unit / insufficient SIL / protocol / zone) produce no match
* [x] Required capabilities omitting optional dimensions still match on the capability dimension alone
* [x] `mvn clean test` — BUILD SUCCESS, 430 tests, 0 failures (was 414)

---

## Phase 12: Documentation and Thesis Support

**Goal**

Document the prototype clearly for academic and engineering evaluation.

**Tasks**

* [ ] Write project overview
* [ ] Document architecture (five-step mapping procedure: Parse, Instantiate, Lift, Infer, Match)
* [ ] Document input file sources (MTP AML, OPC UA Server Datasheet, AAS)
* [ ] Document ontology classes (CSS-SDA dimensions, MTPModule, FieldDevice)
* [ ] Document SPARQL matching logic (multi-dimensional, cross-plant queries)
* [ ] Document MTP vs OPC UA Server Datasheet contribution to each CSS-SDA dimension
* [ ] Document REST API
* [ ] Document Docker Compose and Kubernetes manifest generation strategy
* [ ] Document evaluation cases (fermentation skid, HVAC plant, cross-plant)
* [ ] Add limitations section
* [ ] Add future work section

**Acceptance Criteria**

* [ ] README explains how to run the project
* [ ] Documentation explains the semantic mapping chain
* [ ] Documentation includes example request and response
* [ ] Documentation includes example Docker Compose manifest output
* [ ] Documentation clearly states limitations

---

## Phase 13: EKA UI Polish — Graph, Settings, and Cross-Cutting Views

**Goal**

Add the remaining three EKA UI views (ontology graph visualization, settings, settings menu) that depend on all backend phases being complete. Apply cross-cutting accessibility, mobile QA, and performance optimization across all views. Add integration tests that exercise the full UI stack.

**Tasks**

* [ ] Create `GraphView` with Cytoscape.js integration:
  * [ ] Interactive ontology graph with zoom, pan, expand/collapse nodes, highlight paths, relationship labels, search nodes, center node, fullscreen
* [ ] Create `SettingsView`:
  * [ ] User preferences: Theme, Language, Time Zone, Notifications, Accessibility, Search Preferences
* [ ] Add settings menu to `MainLayout` header
* [ ] Add user profile menu to `MainLayout` header
* [ ] Implement keyboard navigation across all views:
  * [ ] Tab order, focus indicators, shortcut keys for main actions
* [ ] Implement ARIA labels on all interactive components
* [ ] Implement high contrast mode
* [ ] Implement screen reader support across all views
* [ ] Implement focus management for modals, wizards, and dialogs
* [ ] Implement virtual scrolling for large grids (SearchView, ExplorerView entity list)
* [ ] Implement lazy view loading for all views
* [ ] Minimize DOM updates and avoid unnecessary server round trips
* [ ] Add Vaadin view rendering tests for all UI views
* [ ] Add Vaadin component interaction tests for key workflows:
  * [ ] Dashboard navigation
  * [ ] Search with autocomplete and result selection
  * [ ] Service Registration Wizard complete flow
  * [ ] Upload flow with file selection and validation
  * [ ] Explorer tree expand/collapse
  * [ ] Graph zoom and pan
  * [ ] Settings save and reload
  * [ ] Light/dark mode toggle
* [ ] Audit all views for responsive layout on mobile viewport
* [ ] Audit all views for accessibility compliance

**Acceptance Criteria**

* [ ] Vaadin view rendering tests pass for all UI views across all phases
* [ ] Vaadin component interaction tests pass for all key workflows
* [ ] Ontology graph renders nodes and supports zoom, pan, highlight, and search
* [ ] SettingsView saves preferences and restores them on reload
* [ ] User profile menu shows user name and settings link
* [ ] All views have keyboard-navigable controls
* [ ] All interactive components have ARIA labels
* [ ] High contrast mode renders all views legibly
* [ ] Screen readers can navigate all views
* [ ] Large grids use virtual scrolling for performance
* [ ] Views lazy-load to minimize initial bundle size
* [ ] Vaadin tests pass for all view rendering scenarios
* [ ] Vaadin integration tests pass for all key workflows
* [ ] Responsive layout works on mobile, tablet, and desktop viewports

---

## Phase 14: Margo Sandbox Deployment Context

**Goal**

Use Margo Sandbox as an industrial edge deployment/interoperability context. Validate that the selected service or generated deployment artifact can fit into an edge deployment workflow.

**Tasks**

* [ ] Study the Margo Sandbox application deployment workflow
* [ ] Define how the selected service maps to a Margo-style application package
* [ ] Add a simple Margo deployment descriptor or package metadata if required
* [ ] Document limitations of the Margo Sandbox integration

**Acceptance Criteria**

* [ ] Margo integration does not replace Apache Jena or SPARQL
* [ ] Margo integration is documented as deployment-context validation only

---

## MVP Definition

The minimum viable prototype is complete when the following phases are done:

```text
Phase 1:  Project Foundation
Phase 2:  CSS-SDA Ontology Model
Phase 3:  Ontology Loading with Apache Jena
Phase 4:  Input File Parsers — MTP AML and OPC UA Server Datasheet
Phase 5:  SPARQL Matching Query
Phase 6:  Ontology Inference — OWL Reasoner
Phase 7:  Semantic Reasoner Service
Phase 8:  REST API
Phase 9:  Docker Compose and Kubernetes Manifest Generation
Phase 10: Matching Explanation
Phase 11:  Evaluation Dataset — Fermentation Skid and HVAC Plant
Phase 11b: OPC UA Data Visualization Client
Phase 12: Documentation and Thesis Support
Phase 13: EKA UI Polish — Graph, Settings, and Cross-Cutting Views
Phase 14: Margo Sandbox Deployment Context
```

All three use cases (A: Flowmeter NE 107, B: Fermentation Skid, C: HVAC Plant) are delivered when Phases 1-11 are complete. Phases 12-14 add documentation, UI polish, and deployment validation.

---

## MVP Demo Script

1. Start the application.

2. Send request:

```http
POST /api/v1/requirements/match
```

Body:

```json
{
  "requirementId": "REQ_001"
}
```

3. Show selected service:

```text
FlowmeterDiagnosticsService
```

4. Show explanation:

```text
The service matches the required asset condition monitoring capability, uses OPC UA in read-only mode, satisfies recall and latency constraints, and can run in the selected deployment context.
```

5. Show generated Docker Compose manifest.

---

## Final Deliverables

The final project should deliver:

* Working Java 25 and Spring Boot 4.0.x application
* CSS-SDA ontology with full 8-tuple (UseCase, Requirement, Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext)
* RDF/Turtle ontology file
* MTP AML parser — generates CSS-SDA triples from VDI/VDE/NAMUR 2658 files
* OPC UA Server Datasheet parser — generates CSS-SDA triples from IDTA 02009-1-0 submodels
* SPARQL matching query — multi-dimensional matching across all CSS-SDA dimensions
* OWL reasoner inference — derives OfferedCapability from SkillParameter semantics
* Apache Jena-based semantic reasoner
* Requirement matching REST API
* Docker Compose and Kubernetes manifest generation
* Unit, integration, and Vaadin UI tests
* Evaluation dataset with all 3 use cases: Flowmeter NE 107, Fermentation Skid, HVAC Plant + cross-plant queries
* Vaadin 25 UI with ExplorerView for ontology browsing
