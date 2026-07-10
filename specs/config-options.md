# Configurable Options Registry

## Purpose

Define a JSON-based configuration source that feeds all dropdown selections in the requirement form and service registration wizard. Dropdown values are not hardcoded in Java — they are loaded from a configuration file that can be extended without code changes.

## Location

```text
src/main/resources/config/options.json
```

## Schema

```json
{
  "<fieldName>": [
    { "value": "<ontology-value>", "label": "<display-label>" }
  ]
}
```

Each key is a field name matching an ontology data property. Each value is an ordered list of `{ value, label }` pairs where `value` is the RDF literal stored in the ontology and `label` is the human-readable text shown in the UI.

## Defined Fields

### Capability Dimension

| Field Key | Ontology Property | Example Values |
|---|---|---|
| `functionClass` | `sda:functionClass` | AssetConditionMonitoring, TempControl, Dosing, FlowMonitoring, PressureControl, VibrationAnalysis |
| `engineeringUnit` | `sda:engineeringUnit` | °C, bar, L/min, %, RPM, kPa, m³/h, µS/cm |
| `safetyIntegrityLevel` | `sda:safetyIntegrityLevel` | SIL-0, SIL-1, SIL-2, SIL-3, SIL-4 |

### CommunicationBinding Dimension

| Field Key | Ontology Property | Example Values |
|---|---|---|
| `protocol` | `sda:protocol` | OPC-UA, PROFINET, BACnet, MQTT, EtherNet/IP, Modbus TCP, IO-Link |
| `accessMode` | `sda:accessMode` | read-only, read-write, write-only |

### DeploymentContext Dimension

| Field Key | Ontology Property | Example Values |
|---|---|---|
| `networkZone` | `sda:networkZone` | plant-network, OT-Zone-1, OT-Zone-2, OT-Zone-3, DMZ, cloud |
| `determinismClass` | `sda:determinismClass` | HardRealTime, SoftRealTime, NonRealTime |

## Default Options File

```json
{
  "functionClass": [
    { "value": "AssetConditionMonitoring", "label": "Asset Condition Monitoring" },
    { "value": "TempControl", "label": "Temperature Control" },
    { "value": "Dosing", "label": "Dosing Control" },
    { "value": "FlowMonitoring", "label": "Flow Monitoring" },
    { "value": "PressureControl", "label": "Pressure Control" },
    { "value": "VibrationAnalysis", "label": "Vibration Analysis" }
  ],
  "protocol": [
    { "value": "OPC-UA", "label": "OPC UA" },
    { "value": "PROFINET", "label": "PROFINET" },
    { "value": "BACnet", "label": "BACnet" },
    { "value": "MQTT", "label": "MQTT" },
    { "value": "EtherNet/IP", "label": "EtherNet/IP" },
    { "value": "Modbus TCP", "label": "Modbus TCP" },
    { "value": "IO-Link", "label": "IO-Link" }
  ],
  "engineeringUnit": [
    { "value": "°C", "label": "Degrees Celsius (°C)" },
    { "value": "bar", "label": "Bar (bar)" },
    { "value": "L/min", "label": "Liters per Minute (L/min)" },
    { "value": "%", "label": "Percent (%)" },
    { "value": "RPM", "label": "Revolutions per Minute (RPM)" },
    { "value": "kPa", "label": "Kilopascal (kPa)" },
    { "value": "m³/h", "label": "Cubic Meters per Hour (m³/h)" },
    { "value": "µS/cm", "label": "Microsiemens per Centimeter (µS/cm)" }
  ],
  "safetyIntegrityLevel": [
    { "value": "SIL-0", "label": "SIL-0 (No requirement)" },
    { "value": "SIL-1", "label": "SIL-1" },
    { "value": "SIL-2", "label": "SIL-2" },
    { "value": "SIL-3", "label": "SIL-3" },
    { "value": "SIL-4", "label": "SIL-4" }
  ],
  "accessMode": [
    { "value": "read-only", "label": "Read Only" },
    { "value": "read-write", "label": "Read/Write" },
    { "value": "write-only", "label": "Write Only" }
  ],
  "networkZone": [
    { "value": "plant-network", "label": "Plant Network" },
    { "value": "OT-Zone-1", "label": "OT Zone 1" },
    { "value": "OT-Zone-2", "label": "OT Zone 2" },
    { "value": "OT-Zone-3", "label": "OT Zone 3" },
    { "value": "DMZ", "label": "DMZ" },
    { "value": "cloud", "label": "Cloud" }
  ],
  "determinismClass": [
    { "value": "HardRealTime", "label": "Hard Real-Time" },
    { "value": "SoftRealTime", "label": "Soft Real-Time" },
    { "value": "NonRealTime", "label": "Non Real-Time" }
  ]
}
```

## Java Implementation

### OptionsConfigService

Package: `com.example.semanticsda.config`

Responsibilities:
- Load `config/options.json` from classpath on application startup
- Parse into a `Map<String, List<Option>>` where `Option` is a record `{ value, label }`
- Expose `List<Option> getOptions(String fieldName)` for the REST API
- Expose `List<String> getAllFieldNames()` for UI initialization
- Fail fast with clear error if `options.json` is missing or malformed
- Validate that all option values are non-empty strings

### Option Record

```java
public record Option(String value, String label) {}
```

### REST Endpoint

```
GET /api/v1/options/{fieldName}
```

Response:
```json
[
  { "value": "OPC-UA", "label": "OPC UA" },
  { "value": "PROFINET", "label": "PROFINET" },
  ...
]
```

If `fieldName` does not exist in the options file, return `404 Not Found` with a `ProblemDetail` error.

## Extensibility Rules

- Adding a new protocol: edit `options.json`, no Java code changes
- Adding a new functionClass: edit `options.json`, no Java code changes
- Adding a new field entirely: add the key to `options.json` + add the corresponding dropdown to the UI form
- The ontology data property must match the field key in `options.json` for SPARQL queries to work
- Option values must match the RDF literal values used in the ontology — mismatch causes silent no-match results

## Validation

- `options.json` must be valid JSON on application startup
- Each field must have at least one option
- Each option must have non-empty `value` and `label`
- No duplicate `value` entries within the same field
- On load failure, the application must not start (fail fast)

## Tests

- Unit test: `OptionsConfigService` loads all fields correctly
- Unit test: missing `options.json` throws `OntologyLoadingException`
- Unit test: malformed JSON throws clear parse error
- Unit test: empty field list throws validation error
- Unit test: duplicate values are rejected
- Unit test: `GET /api/v1/options/{fieldName}` returns correct list
- Unit test: `GET /api/v1/options/nonexistent` returns 404
