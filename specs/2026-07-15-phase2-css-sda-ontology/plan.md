# Phase 2: CSS-SDA Ontology Model — Implementation Plan

## 1. Use Case A (MVP): Replace Ontology Instances
- [ ] Replace Modbus pump-fill instances with paper's OPC-UA flowmeter scenario
- [ ] Add `:REQ-001` Requirement (requirementID="REQ-001", title="Flowmeter Asset-Health", requirementType="AssetDiagnostics")
- [ ] Add `:AssetConditionMonitoringCapability` OfferedCapability (capabilityID="ACM-001")
- [ ] Add `:SK-FlowmeterDiagnostics` Skill (realisedBy → Capability, isImplementedBy → Service)
- [ ] Add `:SVC-FlowmeterDiagnostics` Service (reportedRecall=0.94, reportedLatency=45)
- [ ] Add `:CB-Flowmeter-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", securityMode="SignAndEncrypt")
- [ ] Add `:DC-Flowmeter-FT101` DataConnector (informationModel="OPC-UA", semanticID="IEC-61987-42")
- [ ] Add `:DEPLOY-OnPrem` DeploymentContext (onPlantNetwork=true, safetyIntegrityLevel="SIL-0")
- [ ] Add negative: `:SVC-WrongProtocol` (HTTP), `:SVC-WrongCapability` (FermentationControl), `:SVC-NE107Failure` (Failure status)

## 2. Use Case A (MVP): NE 107 Diagnostic Ontology
- [ ] Add `sda:NE107Status` class with individuals: `:NE107-Good`, `:NE107-Maintenance`, `:NE107-Failure`
- [ ] Add `sda:NE107SubCode` class with individuals: `:NE107-Sensor`, `:NE107-WireBreak`, `:NE107-ShortCircuit`, `:NE107-Function`
- [ ] Add `sda:hasNE107Status` object property, `sda:hasDiagnosticString` data property, `sda:hasSubCode` object property
- [ ] Add `sda:DegradedMode` class (subClassOf Skill), `sda:hasDegradationLevel` data property
- [ ] Add pump degradation individuals: `:pump1HealthyMode`, `:pump1DegradedMode`, `:pump1FaultedMode`
- [ ] Add `:pump1Asset` FieldDevice with NE 107 status linkage

## 3. Use Case A (MVP): SPARQL Query (paper Table I)
- [ ] Rewrite `match-service.rq` to match paper's SPARQL pattern
- [ ] R1: capability match (REQ → mapsToCapability → Capability → realisedBy → Skill → isImplementedBy → Service)
- [ ] R2b: protocol match (CommunicationBinding.protocol = "OPC-UA", accessMode = "read-only")
- [ ] R3: performance+locality (reportedRecall >= 0.90, reportedLatency <= 60, onPlantNetwork = true)
- [ ] ORDER BY DESC(?recall) LIMIT 1

## 4. Use Case A (MVP): SPARQL Tests
- [ ] Test: REQ-001 returns SVC-FlowmeterDiagnostics
- [ ] Test: SVC-WrongProtocol excluded
- [ ] Test: SVC-WrongCapability excluded
- [ ] Test: SVC-NE107Failure excluded
- [ ] Test: pump recovery restores match

## 5. Use Case B: Fermentation Skid (Skid-A1)
- [ ] Add `:Skid-A1` MTPModule instance (SIL-2, CPC, PROFINET, OPC UA gateway)
- [ ] Add `:REQ-002` Requirement (requirementID="REQ-002", title="Fermentation Temperature Control", requirementType="CoreProcessControl")
- [ ] Add `:OC-TempControl` OfferedCapability (capabilityID="TC-001")
- [ ] Add `:OC-DosingControl` OfferedCapability (capabilityID="DC-001")
- [ ] Add `:SK-FermentationControl` Skill (realisedBy → OC-TempControl)
- [ ] Add `:SK-DosingControl` Skill (realisedBy → OC-DosingControl)
- [ ] Add `:SVC-FermentationSkid` Service (SIL-2, HardRealTime)
- [ ] Add `:CB-Skid-A1-PROFINET` CommunicationBinding (protocol="PROFINET", accessMode="read-write")
- [ ] Add `:CB-Skid-A1-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", gateway)
- [ ] Add `:DC-Skid-A1-OPCUA` DataConnector (informationModel="OPC-UA")
- [ ] Add `:DEPLOY-Critical` DeploymentContext (SIL-2, HardRealTime, OT-Zone-1)
- [ ] SPARQL test: REQ-002 matches SVC-FermentationSkid via PROFINET
- [ ] SPARQL test: SIL mismatch excludes service
- [ ] SPARQL test: determinism class mismatch excludes service

## 6. Use Case C: HVAC Plant (AHU-01 + EM-01)
- [ ] Add `:AHU-01` FieldDevice instance (BACnet, M+O, SoftRealTime, OT-Zone-3)
- [ ] Add `:EM-01` EnergyMeter instance (native OPC UA, non-critical)
- [ ] Add `:REQ-003` Requirement (requirementID="REQ-003", title="HVAC Temperature Monitoring", requirementType="MonitoringOptimization")
- [ ] Add `:OC-HVACTempMonitoring` OfferedCapability (capabilityID="HVAC-001")
- [ ] Add `:SK-HVACTempAnalysis` Skill (realisedBy → OC-HVACTempMonitoring)
- [ ] Add `:SVC-HVACTempAnalysis` Service (SoftRealTime, on-premise)
- [ ] Add `:CB-AHU01-BACnet` CommunicationBinding (protocol="BACnet", accessMode="read-only", securityMode="Sign")
- [ ] Add `:CB-AHU01-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", gateway)
- [ ] Add `:DC-AHU01-OPCUA` DataConnector (informationModel="OPC-UA", gateway)
- [ ] Add `:CB-EM01-OPCUA` CommunicationBinding (protocol="OPC-UA", accessMode="read-only", securityMode="SignAndEncrypt", native)
- [ ] Add `:DC-EM01-OPCUA` DataConnector (informationModel="OPC-UA", native)
- [ ] Add `:DEPLOY-OTZone3` DeploymentContext (SIL-0, SoftRealTime, OT-Zone-3)
- [ ] Add belongsToSite: Skid-A1→PlantA, AHU-01→PlantB, EM-01→PlantB
- [ ] SPARQL test: REQ-003 matches SVC-HVACTempAnalysis
- [ ] SPARQL test: cross-plant query finds temperature services across PlantA and PlantB
- [ ] SPARQL test: networkZone mismatch excludes service

## 7. CSS-SDA Classes and Properties
- [ ] Requirement class properties (paper Figure 5): requirementID, title, description, rationale, acceptanceCondition, requirementType, lifecycleStatus, mapsToCapability
- [ ] Capability class properties (paper Figure 5): capabilityID, capabilityName, description, version, realisedBy, exposedAs, mapsTo
- [ ] Physical asset classes: MTPModule, FieldDevice, BACnetDevice, EnergyMeter
- [ ] Object properties: mapsToCapability, realisedBy, isImplementedBy, usesBinding, usesConnector, isDeployedBy
- [ ] Data properties: protocol, accessMode, reportedRecall, reportedLatency, onPlantNetwork, engineeringUnit, semanticID

## 8. ExplorerView UI
- [ ] Create ExplorerView with SplitLayout (tree left, details right)
- [ ] Create OntologyTree component (expandable/collapsible)
- [ ] Add Vaadin UI test

## 9. Tests
- [ ] Jena loads ontology, contains all 8 CSS-SDA classes
- [ ] Jena model contains REQ-001, REQ-002, REQ-003 chains
- [ ] Vaadin UI test: ExplorerView renders
- [ ] `mvn clean test` passes
