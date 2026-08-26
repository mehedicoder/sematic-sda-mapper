# Device Descriptions

Example device description source files for testing the semantic pipeline.

## File Structure

Upload sample files are grouped by **use case / upload session** so that every
file you would drag into the Mapping Hub dropzone together lives in one folder.
Standalone format samples with no specific use case live under `reference-formats/`.

```
device-descriptions/
├── use-cases/
│   ├── REQ-001-flowmeter-diagnostics/   # Use case A — Flowmeter (NE 107)
│   │   ├── pump-aas.json                #   AAS for Pump Module FT101
│   │   ├── pump-module-mtp.xml          #   MTP AML for Pump Module FT101
│   │   ├── pump-opcua-datasheet.json    #   OPC UA datasheet for Pump FT101
│   │   ├── requirement-flowdiagnostics.ttl  # supplementary requirement upload
│   │   └── req001-uaserver-nodeset-complete.xml
│   ├── REQ-002-fermentation-skid/       # Use case B — Fermentation Skid
│   │   ├── mixer-module-mtp.xml         #   MTP AML for Mixer Module MX101
│   │   └── req002-mtp-fermentation-skid*.xml
│   └── REQ-003-hvac-plant/              # Use case C — HVAC Plant
│       ├── temperature-sensor-opcua-datasheet.json
│       └── req003-uaserver-ahu01*.json
├── reference-formats/
│   ├── motor-aas.json                   # AAS for Motor Module EM101 (no use case)
│   └── sample-caex.aml                  # AutomationML / CAEX smoke-test sample
└── README.md
```

## Device Cross-Reference

The same physical device is described in multiple formats to enable
multi-source parsing and semantic lifting. Under `use-cases/REQ-001-flowmeter-diagnostics/`:

| Physical Device | AAS File | MTP File | OPC UA Datasheet |
|----------------|----------|----------|------------------|
| Pump Module FT101 | `use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json` | `use-cases/REQ-001-flowmeter-diagnostics/pump-module-mtp.xml` | `use-cases/REQ-001-flowmeter-diagnostics/pump-opcua-datasheet.json` |
| Mixer Module MX101 | -- | `use-cases/REQ-002-fermentation-skid/mixer-module-mtp.xml` | -- |
| Temperature Sensor | -- | -- | `use-cases/REQ-003-hvac-plant/temperature-sensor-opcua-datasheet.json` |

## Semantic Mapping

Source field → Ontology concept (css-sda-unified.ttl)

### AAS → Ontology

| AAS Element | Ontology Class | Ontology Property |
|------------|---------------|-------------------|
| `submodels[].idShort = "Skills"` → `submodelElements[].idShort` | `css:Skill` | `rdfs:label` |
| `submodels[].idShort = "Skills"` → `submodelElements[].properties[SkillId]` | `css:Skill` | `sda:capabilityID` |
| `submodels[].idShort = "Skills"` → `submodelElements[].properties[EngineeringUnit]` | `css:Skill` | `sda:engineeringUnit` |
| `submodels[].idShort = "Skills"` → `submodelElements[].properties[NominalRangeMin]` | `css:Skill` | `sda:nominalRange_min` |
| `submodels[].idShort = "Skills"` → `submodelElements[].properties[NominalRangeMax]` | `css:Skill` | `sda:nominalRange_max` |
| `submodels[].idShort = "Communication"` → `properties[Protocol]` | `sda:CommunicationBinding` | `sda:protocol` |
| `submodels[].idShort = "Communication"` → `properties[EndpointUrl]` | `sda:DataConnector` | endpoint URI |
| `submodels[].idShort = "Communication"` → `properties[SecurityMode]` | `sda:CommunicationBinding` | `sda:securityMode` |
| `submodels[].idShort = "Communication"` → `properties[AccessMode]` | `sda:CommunicationBinding` | `sda:accessMode` |
| `submodels[].idShort = "Operations"` → `submodelElements[].idShort` | `sda:Skill` (operation) | `rdfs:label` |
| `assetInformation.globalAssetId` | `sda:FieldDevice` | instance URI |
| `submodels[].idShort = "TechnicalData"` → properties | `sda:FieldDevice` | data properties |
| Deployment metadata | `sda:DeploymentContext` | `sda:onPlantNetwork`, `sda:safetyIntegrityLevel`, `sda:determinismClass`, `sda:networkZone` |

### MTP AML → Ontology

| MTP Element | Ontology Class | Ontology Property |
|------------|---------------|-------------------|
| `<Service Name="...">` | `css:Skill` | `rdfs:label` |
| `<Service>` → `<Parameter Name="...">` | `css:Skill` | `sda:engineeringUnit`, `sda:nominalRange_min`, `sda:nominalRange_max` |
| `<Communication>` → `<OpcUaServer Endpoint="...">` | `sda:CommunicationBinding` | `sda:protocol = "OPC-UA"` |
| `<OpcUaServer>` → `Endpoint` attribute | `sda:DataConnector` | endpoint URI |
| `<OpcUaServer>` → `SecurityMode` attribute | `sda:CommunicationBinding` | `sda:securityMode` |
| `<DataAssembly>` → `<OpcUaNode>` | `sda:DataConnector` | `sda:informationModel = "OPC-UA"` |
| `<OpcUaNode>` → `NodeId` attribute | `sda:DataConnector` | `sda:semanticID` |
| `<Instance Name="...">` | `sda:MTPModule` | `rdfs:label` |
| Wiring: Service → Communication | `sda:isImplementedBy` | Skill → Service |
| Wiring: Communication → DataAssembly | `sda:usesConnector` | Binding → Connector |

### OPC UA Server Datasheet (IDTA 02009-1-0) → Ontology

| JSON Field | Ontology Class | Ontology Property |
|-----------|---------------|-------------------|
| `EndpointDescriptions[].uri` | `sda:CommunicationBinding` | endpoint URI |
| `EndpointDescriptions[].securityMode` | `sda:CommunicationBinding` | `sda:securityMode` |
| `EndpointDescriptions[].transportProfile` | `sda:CommunicationBinding` | `sda:protocol = "OPC-UA"` |
| `Configuration.NodeSets[].id` | `sda:DataConnector` | `sda:semanticID` |
| `Configuration.NodeSets[].namespace` | `sda:DataConnector` | namespace qualifier |
| `Identification.productUri` | `sda:Service` | `rdfs:label` |
| `Identification.softwareVersion` | `sda:Service` | `sda:version` |
| `Identification.manufacturerName` | `sda:Service` | manufacturer metadata |
| `Identification.serialNumber` | `sda:Service` | `sda:image` (as container image reference) |

### Pipeline: Parse → Instantiate → Lift → Infer → Match

```
Source Files         Parser            RDF Triples        Lifter           Ontology Instances
─────────────        ──────            ───────────        ──────           ──────────────────
AAS JSON      ──→   (future)    ──→   raw triples  ──→   CSS-SDA lift  ──→  Skill, DataConnector
MTP XML       ──→   MTPAmlParser ──→   raw triples  ──→   CSS-SDA lift  ──→  Skill, CommunicationBinding
OPC UA JSON   ──→   UaServerDatasheetParser ──→       raw triples  ──→   CSS-SDA lift  ──→  DataConnector, Service
                                                              │
                                                              v
                                                    OfferedCapability
                                                    DeploymentContext
                                                              │
                                                              v
                                                    SPARQL Match (R1, R2a, R2b, R3)
                                                              │
                                                              v
                                                    R-Deploy → lifecycleStatus = "Matched"
                                                    R-Verify  → lifecycleStatus = "Verified"
```

## Semantic Consistency

All three formats for Pump Module FT101 must yield consistent ontology instances:

| Concept | AAS Source | MTP Source | OPC UA Source |
|---------|-----------|-----------|--------------|
| Skill "Pumping" | `Skills.Pumping` | `Service[@Name='Pumping']` | (derived from methods) |
| Skill "FlowMonitoring" | `Skills.FlowMonitoring` | `Service[@Name='FlowMonitoring']` | (derived from methods) |
| CommunicationBinding | `Communication.Protocol=OPC-UA` | `OpcUaServer[@Endpoint]` | `EndpointDescriptions[uri]` |
| DataConnector | `Communication.EndpointUrl` | `OpcUaNode[@NodeId]` | `Configuration.NodeSets[id]` |
| DeploymentContext | (inferred from site) | (inferred from instance) | (inferred from identification) |

## Realistic Industrial Values

All device descriptions use realistic values from the process automation domain:

- **Protocols**: OPC-UA (IEC 62541), BACnet (ISO 16484-5), PROFINET (IEC 61158/61784)
- **Security Modes**: None, Sign, SignAndEncrypt
- **Security Policies**: Basic128Rsa15, Basic256, Basic256Sha256
- **NE 107 Diagnostics**: Good, Maintenance, Failure, Sensor, WireBreak, ShortCircuit, Function
- **Engineering Units**: l/min, bar, °C, rpm, mm/s, mL/min, kW
- **Safety Integrity Levels**: SIL-0, SIL-1, SIL-2, SIL-3, SIL-4
- **Determinism Classes**: SoftRealTime, HardRealTime
- **Network Zones**: OT-Zone-1, OT-Zone-2, OT-Zone-3, IT-Zone
- **IEC Semantic IDs**: IEC-61987-42 (flow), IEC-61987-22 (temperature), VDI/VDE/NAMUR 2658

## Use-Case Bundles

The `use-cases/` subfolder groups every upload file that belongs to a single
paper use case together in one folder:

| Folder | Paper use case | Input format | Expected matched service |
|--------|---------------|--------------|--------------------------|
| `use-cases/REQ-001-flowmeter-diagnostics/` | A — Flowmeter (NE 107) diagnostics | OPC UA NodeSet (XML) | `SVC-FlowmeterDiagnostics` |
| `use-cases/REQ-002-fermentation-skid/` | B — Fermentation Skid | MTP AML (XML) | `SVC-FermentationSkid` |
| `use-cases/REQ-003-hvac-plant/` | C — HVAC Plant | OPC UA datasheet (JSON) | `SVC-HVACTempAnalysis` |

`use-cases/REQ-001-flowmeter-diagnostics/requirement-flowdiagnostics.ttl` is a
supplementary **requirement file** that can be uploaded together with the Pump
FT101 device files in that same folder to match the seeded Flowmeter Diagnostics
Service via its `FlowDiagnostics` capability.

These files are also the canonical source for the manual UAT instructions in
`docs/use-case-inputs/README.md`. The previously duplicated copies under
`docs/use-case-inputs/formats/` have been removed.
