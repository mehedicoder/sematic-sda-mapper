# SDA Pump Health Use Case — Example Upload Package

This package contains a complete, runnable example of the **CSS-SDA ontology pipeline** described in the paper *"A Semantic Approach towards Software-Defined Automation"* (EKA 2026).

## Files

| File | Role | Ontology Layer |
|---|---|---|
| `req_pump_health.ttl` | **Demand side** — Use case, requirement, required capability, constraints, parameters | Layer 1 (Application) |
| `catalogue_services.ttl` | **Supply side** — Skills, offered capabilities, services, bindings, connectors | Layer 2 (Service/Capability/Skill) |
| `catalogue_assets.ttl` | **Asset side** — Physical device, sensors, process variables, semantic concepts | Layer 4 (Physical Plant) |
| `edge_nodes.ttl` | **Infrastructure** — Deployment contexts, cluster nodes, network zones, policies | Layer 2b (Platform Infrastructure) |

## The Use Case

**Business need:** Prevent unplanned interruptions by monitoring degradation of a tank-fill pump using **NAMUR NE107** diagnostics.

**Requirement (REQ-03):**
- Capability: `AssetConditionMonitoring`
- Performance: recall ≥ 0.90, latency ≤ 60 ms
- Deployment: must run on-plant network (`onPlantNetwork = true`)
- Determinism: `SoftRealTime`

## Pipeline Execution

### 1. Load the TBox (ontology schema)
```python
import rdflib
g = rdflib.Graph()
g.parse("sda_ontology_v2_structured.ttl", format="turtle")
```

### 2. Load the ABox (this package)
```python
g.parse("req_pump_health.ttl", format="turtle")
g.parse("catalogue_services.ttl", format="turtle")
g.parse("catalogue_assets.ttl", format="turtle")
g.parse("edge_nodes.ttl", format="turtle")
```

### 3. Run R1 — Capability Match (type-based)
Finds services whose skill offers a capability matching `RequiredCapability.capabilityType`.

**Expected result:** `Service_Diagnostics_v1` matches (type = "AssetConditionMonitoring").  
`Service_TempControl_v1` is excluded (type = "TemperatureControl").

### 4. Run R2a — Semantic Concept Coverage
Checks that every `SemanticConcept` required by the skill exists on the asset.

**Expected result:** PASS — `CDD_FlowRate` and `CDD_SignalQuality` are present.

### 5. Run R2b — Protocol Match
Checks that the skill's `CommunicationBinding.protocol` matches the asset's host capability.

**Expected result:** `Service_Diagnostics_v1` uses OPC-UA (matches).  
`Service_Vibration_Modbus` uses Modbus-RTU (excluded).

### 6. Run R3 — Requirement Satisfaction
Filters by deployment context and performance parameters.

**Expected result:** `Service_Diagnostics_v1` passes (recall=0.95 ≥ 0.90, latency=45 ≤ 60, onPrem=true).

### 7. Run R-Deploy — Commit the Match (SWRL)
Writes back `boundService`, `boundBinding`, `boundConnector`, `boundContext`, and sets `lifecycleStatus` to "Matched".

### 8. Run R-Verify — Runtime Verification
After deployment, verification criteria are checked. When all pass, `lifecycleStatus` becomes "Verified".

## Candidates in the Catalogue

| Service | Capability Type | Protocol | Recall | Latency | Expected Match |
|---|---|---|---|---|---|
| `Service_Diagnostics_v1` | AssetConditionMonitoring | OPC-UA | 0.95 | 45 ms | ✅ YES |
| `Service_TempControl_v1` | TemperatureControl | — | 0.99 | 20 ms | ❌ Wrong capability |
| `Service_Vibration_Modbus` | AssetConditionMonitoring | Modbus-RTU | 0.92 | 55 ms | ❌ Wrong protocol |

## Key Design Decisions

1. **RequiredCapability / OfferedCapability split**  
   The demand-side (`RCap_AssetConditionMonitoring`) and supply-side (`OCap_AssetConditionMonitoring`) are **independent nodes**. Matching uses `capabilityType` string equality, not shared node identity. This enables discovery of unknown providers.

2. **MTP + PA-DIM + AAS integration**  
   The `Skill_NE107_Diag` is typed as `MTPSkill` and references an MTP AML descriptor. It consumes PA-DIM semantic concepts (IEC CDD IRDIs) via the `DataConnector`.

3. **Margo-aligned deployment context**  
   `ClusterNode` properties (`ociRuntimeCompliant`, `hasOTELCollector`, `marginDeviceRole`) are lifted directly from the Margo device specification.

## Namespaces

```turtle
@prefix :    <http://sda-platform.org/ontology#> .
@prefix ex:  <http://sda-platform.org/examples/pump-health#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
```

## Validation

Run the test case to verify:
```bash
python tc_c3_required_offered_capability.py
```

This confirms that:
- The **type-based R1 query** returns exactly `Service_Diagnostics_v1`
- The **old identity-based query** returns nothing (proving the split is load-bearing)
- The **wrong-type** and **wrong-protocol** candidates are correctly excluded

## Next Steps

1. Extend the asset catalogue with additional field devices (level sensors, temperature transmitters)
2. Add more service candidates to test constraint ranking
3. Connect the `DeploymentManager` to generate Kubernetes manifests from `boundContext` + `DeploymentPolicy`
4. Implement the `R-Verify` runtime check against live OPC-UA telemetry
