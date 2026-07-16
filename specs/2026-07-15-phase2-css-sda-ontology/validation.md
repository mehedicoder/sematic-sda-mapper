# Phase 2: CSS-SDA Ontology Model — Validation Steps

## Automated

### Build
- [ ] `mvn clean test` passes (0 failures)

### Ontology Validity
- [ ] Jena loads `css-sda-small.ttl` without error
- [ ] Jena model contains all 8 CSS-SDA classes (UseCase, Requirement, Capability, Skill, Service, CommunicationBinding, DataConnector, DeploymentContext)

### Use Case A: Flowmeter NE 107 (paper Section 4.4)
- [ ] Jena model contains `:REQ-001` with requirementID="REQ-001"
- [ ] Jena model contains `:AssetConditionMonitoringCapability`
- [ ] Chain exists: REQ-001 → mapsToCapability → Capability → realisedBy → Skill → isImplementedBy → Service
- [ ] Service has reportedRecall=0.94, reportedLatency=45
- [ ] CommunicationBinding has protocol="OPC-UA", accessMode="read-only"
- [ ] DeploymentContext has onPlantNetwork=true
- [ ] Negative candidate `:SVC-WrongProtocol` has protocol="HTTP"
- [ ] Negative candidate `:SVC-WrongCapability` is not reachable from REQ-001
- [ ] Negative candidate `:SVC-NE107Failure` exists with NE 107 status = Failure

### SPARQL Query (paper Table I)
- [ ] Paper's SPARQL query returns `:SVC-FlowmeterDiagnostics` for `:REQ-001`
- [ ] Query excludes `:SVC-WrongProtocol` (protocol mismatch)
- [ ] Query excludes `:SVC-WrongCapability` (capability mismatch)
- [ ] Query excludes `:SVC-NE107Failure` (NE 107 status = Failure)
- [ ] Query orders by recall descending

### ExplorerView UI
- [ ] ExplorerView renders with split layout
- [ ] OntologyTree shows CSS-SDA class hierarchy
- [ ] Selecting a node shows entity details

## Manual

### Ontology Browsing (Protégé or VS Code RDF extension)
- [ ] Open `css-sda-small.ttl`, verify class hierarchy
- [ ] Verify Requirement has all properties from paper Figure 5
- [ ] Verify Capability has all properties from paper Figure 5
- [ ] Verify full chain: UseCase → Requirement → Capability → Skill → Service → CommunicationBinding → DataConnector → DeploymentContext
- [ ] Verify Use Case A: REQ-001 → FlowmeterDiagnosticsService is complete
- [ ] Verify negative candidates are distinguishable

## Definition of Done

- [ ] All automated tests pass
- [ ] Ontology supports the paper's SPARQL query (Table I) without modification
- [ ] ExplorerView renders and navigable from MainLayout
