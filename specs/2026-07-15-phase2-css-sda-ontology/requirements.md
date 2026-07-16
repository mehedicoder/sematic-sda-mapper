# Phase 2: CSS-SDA Ontology Model — Requirements

## Scope

Build the CSS-SDA ontology and validate against all three use cases from the paper. **Use Case A (NE 107 Flowmeter) is the MVP.** Use Cases B and C follow immediately after.

## In Scope

### Use Case A (MVP) — Flowmeter NE 107
- Replace current Modbus instances with paper's OPC-UA flowmeter scenario
- REQ-001 → AssetConditionMonitoringCapability → FlowmeterDiagnosticsService chain
- CommunicationBinding: OPC-UA, read-only, SignAndEncrypt
- DeploymentContext: onPremise=true, plant-network
- NE 107 diagnostic ontology: status individuals, sub-codes, DegradedMode, hasDegradationLevel
- SPARQL query matching paper's Table I (R1 capability match, R2b protocol match, R3 performance+locality)
- Negative candidates: wrong protocol, wrong capability, NE 107 Failure
- SPARQL tests: healthy match, failed exclusion, recovery

### Use Case B — Fermentation Skid (Skid-A1)
- MTPModule, SIL-2, CPC, PROFINET, OPC UA gateway
- REQ-002 → TempControl + DosingControl capabilities
- Skills: FermentationControl, DosingControl
- CommunicationBinding: PROFINET (read-write) + OPC-UA (read-only gateway)
- DeploymentContext: SIL-2, HardRealTime, OT-Zone-1
- SPARQL tests: SIL mismatch, determinism class mismatch

### Use Case C — HVAC Plant (AHU-01 + EM-01)
- AHU-01: FieldDevice, BACnet, OPC UA gateway, SoftRealTime, OT-Zone-3
- EM-01: EnergyMeter, native OPC UA, non-critical
- REQ-003 → HVACTempMonitoring capability
- CommunicationBinding: BACnet (Sign) + OPC-UA (SignAndEncrypt, native)
- DeploymentContext: SIL-0, SoftRealTime, OT-Zone-3
- Cross-plant query across PlantA (Skid-A1) and PlantB (AHU-01, EM-01)
- SPARQL tests: networkZone mismatch, cross-plant query

### ExplorerView
- Ontology tree sidebar with CSS-SDA class hierarchy

## Out of Scope

- Phase 3+ (Jena loader, OWL reasoner, SWRL rules)
- Dynamic tree loading from Jena
- Input file parsers (Phase 4)

## Existing Assets (from Phase 1)

- `css-sda-small.ttl` — stub with Modbus pump fill (to be replaced)
- `match-service.rq` — stub filtering Modbus (to be rewritten)
- `Ne107DiagnosticSimulator.java` — NE 107 status codes, fault injection, auto-recovery
- `PumpDegradationModel.java` — pump degradation physics
- `TankOpcUaServer.java` — exposes NE 107 diagnostic variables on OPC UA

## Acceptance Criteria

### Use Case A (MVP)
1. Ontology supports paper's SPARQL query (Table I) without modification
2. SPARQL returns `:SVC-FlowmeterDiagnostics` for `:REQ-001`
3. SPARQL excludes `:SVC-WrongProtocol` (protocol mismatch)
4. SPARQL excludes `:SVC-WrongCapability` (capability mismatch)
5. SPARQL excludes `:SVC-NE107Failure` (NE 107 status = Failure)
6. Pump recovery restores SPARQL match after failure cleared
7. Ontology contains NE 107 status individuals (Good, Maintenance, Failure) and sub-codes
8. Ontology contains DegradedMode class with hasDegradationLevel property
9. Requirement class has paper's properties (Figure 5)
10. Capability class has paper's properties (Figure 5)

### Use Case B
11. SPARQL returns `:SVC-FermentationSkid` for `:REQ-002` via PROFINET binding
12. SPARQL excludes service when SIL mismatch (SIL-0 vs SIL-2)
13. SPARQL excludes service when determinism class mismatch

### Use Case C
14. SPARQL returns `:SVC-HVACTempAnalysis` for `:REQ-003`
15. Cross-plant query finds temperature services across PlantA and PlantB
16. SPARQL excludes service when networkZone mismatch

### General
17. Ontology is valid Turtle (Jena loads without error)
18. Ontology contains all 8 CSS-SDA classes
19. ExplorerView shows CSS-SDA class hierarchy in tree sidebar
20. `mvn clean test` passes
