# Semantic SDA Mapper — User Test Manual

## Overview

This manual guides you through testing all features of the Semantic SDA Mapper prototype. The application maps industrial automation requirements to deployable software services using a CSS-SDA ontology, Apache Jena, and SPARQL matching.

**Prerequisites:**
- Java 25+
- Maven 3.9+
- PostgreSQL (or use H2 in-memory for testing)
- Browser (Chrome, Firefox, Edge)

---

## 1. Starting the Application

```bash
# From project root
mvn spring-boot:run

# Or run the main class
# com.sda.mapper.SemanticSdaMapperApplication
```

The application starts at **http://localhost:8080**. The OPC UA simulation server starts automatically on port 4840.

**Login credentials:**
- Username: `user`
- Password: `password`

---

## 2. System Status (Landing Page)

**Route:** `/`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 2.1 | Open http://localhost:8080 | Login page appears |
| 2.2 | Enter credentials and log in | System Status page loads with welcome message |
| 2.3 | Verify navigation drawer on the left | All 12 navigation links are visible |
| 2.4 | Click the theme toggle (sun/moon icon) | Page switches between light and dark mode |
| 2.5 | Refresh the page | Theme preference is preserved |

---

## 3. Dashboard

**Route:** `/dashboard`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 3.1 | Click "Dashboard" in navigation | 5 animated cards appear: Services, Capabilities, Skills, Semantic Mappings, Inferred Capabilities |
| 3.2 | Verify card values | Cards show numeric counts from the ontology (e.g., Services: 6+, Capabilities: 5+) |
| 3.3 | Click on "Services" card | Navigates to Service Instances view |
| 3.4 | Click on "Capabilities" card | Navigates to relevant view |

---

## 4. Ontology Explorer

**Route:** `/ontology`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 4.1 | Click "Ontology Explorer" in navigation | Split layout appears: tree on left, details on right |
| 4.2 | Expand "sda:Requirement" node in tree | REQ-001, REQ-002, REQ-003 appear as children |
| 4.3 | Click on "REQ-001" | Right panel shows Overview tab with label "REQ-001 Flowmeter Asset-Health" |
| 4.4 | Click "Properties" tab | Shows requirementID=REQ-001, title, description, etc. |
| 4.5 | Click "Relationships" tab | Shows mapsToCapability -> OC-ACM-Flowmeter |
| 4.6 | Expand "sda:Service" node | Shows all service instances |
| 4.7 | Click on "SVC-FlowmeterDiagnostics" | Shows service details with image, recall, latency |
| 4.8 | Expand "css:Capability" node | Shows all capability instances |

---

## 5. Requirement Form — Mode A (By ID)

**Route:** `/requirements`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 5.1 | Click "Requirement Form" in navigation | Form appears with dropdown fields and text inputs |
| 5.2 | Type "REQ-001" in the Requirement ID field | Field accepts input |
| 5.3 | Click "Match" button | Results panel shows matched service: name, protocol, recall, latency |
| 5.4 | Verify explanation text | Explanation mentions capability match, OPC-UA protocol, recall >= 0.90 |
| 5.5 | Clear the Requirement ID field | Field is empty |
| 5.6 | Type "REQ-002" and click "Match" | Shows SVC-FermentationSkid matched with PROFINET protocol |
| 5.7 | Type "REQ-003" and click "Match" | Shows SVC-HVACTempAnalysis matched with BACnet protocol |

---

## 6. Requirement Form — Mode B (Inline Fields)

**Route:** `/requirements`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 6.1 | Leave Requirement ID empty | No ID entered |
| 6.2 | Select "OPC-UA" from Protocol dropdown | Dropdown populated from options.json |
| 6.3 | Select "read-only" from Access Mode dropdown | Dropdown shows read-only selected |
| 6.4 | Set Minimum Recall to 0.90 | Numeric field accepts value |
| 6.5 | Set Maximum Latency to 60 | Numeric field accepts value |
| 6.6 | Click "Match" button | Results show a matched service with OPC-UA protocol |
| 6.7 | Change Protocol to "HTTP" and click "Match" | No match returned (no service uses HTTP) |

---

## 7. Requirement Form — Save Requirement

**Route:** `/requirements`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 7.1 | Select "FlowRate" from Function Class | Dropdown selection |
| 7.2 | Select "OPC-UA" from Protocol | Dropdown selection |
| 7.3 | Select "read-only" from Access Mode | Dropdown selection |
| 7.4 | Click "Save Requirement" button | Success message with auto-generated requirement ID |
| 7.5 | The saved requirement appears in the ontology | Can be matched by ID |

---

## 8. Registration Wizard

**Route:** `/register`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.1 | Click "Registration Wizard" in navigation | Step 1 (General Info) appears with form fields |
| 8.2 | Fill in service name, description | Fields accept input |
| 8.3 | Click "Next" | Step 2 (Communication) appears with protocol dropdown |
| 8.4 | Select protocol and access mode | Dropdowns populated from API |
| 8.5 | Click "Next" through Steps 3-6 | Each step shows relevant form fields |
| 8.6 | Step 7 (Review) shows all entered data | Summary of all fields displayed |
| 8.7 | Click "Register" on Step 8 | POST to /api/v1/requirements, success message |
| 8.8 | Click "Previous" on any step | Returns to previous step with data preserved |
| 8.9 | Click "Cancel" | Returns to Step 1, form cleared |

---

## 9. Search

**Route:** `/search`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 9.1 | Click "Search" in navigation | Search bar with type filter appears |
| 9.2 | Type "Flowmeter" in search field | Results grid shows Flowmeter-related entities |
| 9.3 | Verify grid columns | Name, Type, Description, Score columns visible |
| 9.4 | Select "Requirement" from type filter | Only requirement entities shown |
| 9.5 | Clear filter and type "OPC-UA" | All OPC-UA related entities appear |
| 9.6 | Type "NONEXISTENT" | Empty results with "No results found" message |
| 9.7 | Type "REQ-001" | Shows REQ-001 requirement with high similarity score |

---

## 10. Upload

**Route:** `/upload`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 10.1 | Click "Upload" in navigation | Upload panel with drag-and-drop area appears |
| 10.2 | Drag an XML file onto the upload area | File is accepted, progress bar shows |
| 10.3 | After upload completes | Results grid shows triple count, file type, summary |
| 10.4 | Upload a JSON file (OPC UA Datasheet) | Parsed successfully, triples counted |
| 10.5 | Upload an invalid file | Error message displayed with clear description |

---

## 11. Deployment

**Route:** `/deployment`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 11.1 | Click "Deployment" in navigation | Environment cards appear (Development, Testing, Production) |
| 11.2 | Click "Development" card | Card is selected with highlight |
| 11.3 | Click "Generate Manifests" button | Docker Compose YAML and Kubernetes YAML appear in text areas |
| 11.4 | Verify Docker Compose YAML | Contains service name, image, restart policy, environment variables |
| 11.5 | Verify Kubernetes YAML | Contains apiVersion, kind: Deployment, nodeSelector with SIL labels |
| 11.6 | Read the explanation panel | Human-readable explanation of why the service was selected |

---

## 12. Monitoring

**Route:** `/monitoring`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 12.1 | Click "Monitoring" in navigation | System metrics cards appear |
| 12.2 | Verify CPU usage | Progress bar shows current CPU usage percentage |
| 12.3 | Verify Memory usage | Progress bar shows used/total memory |
| 12.4 | Verify Ontology Statistics | Shows triple count (538+) and class count (17) |
| 12.5 | Verify Uptime | Shows application uptime in human-readable format |
| 12.6 | Refresh the page | Metrics update with current values |

---

## 13. OPC UA Monitor

**Route:** `/opcua-visualization`

| Step | Action | Expected Result |
|------|--------|-----------------|
| 13.1 | Click "OPC UA Monitor" in navigation | Server grid appears showing all known OPC UA servers (250px tall) |
| 13.2 | Verify server grid columns | Name, Type, Host:Port, Path, Status columns visible |
| 13.3 | Verify built-in server row | "Built-in Tank Server" shows Connected status |
| 13.4 | Verify server cards below grid | Each server has a card with connection status and node values grid |
| 13.5 | Click on "Built-in Tank Server" row | Detail panel opens with server name, endpoint, and browsed nodes grid |
| 13.6 | Verify browsed nodes grid | Shows Node ID, Browse Name, Display Name, Value, Type columns |
| 13.7 | Verify node values | Tank1.Level, Tank1.FlowRateIn/Out, Temperature, Pump nodes with live values |
| 13.8 | Wait 5 seconds | Node values refresh automatically in detail view |
| 13.9 | Click "Back to Servers" button | Returns to server grid view |
| 13.10 | Click on external server row | If server is unreachable, "Could not connect" message in log; grid shows browsed nodes or empty |

---

## 14. REST API Testing

### 14.1 Match by Requirement ID (Mode A)

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId": "REQ-001"}'
```

**Expected Response:**
```json
{
  "matched": true,
  "serviceName": "Flowmeter Diagnostics Service",
  "containerImage": "registry.local/flowmeter-diagnostics:1.0",
  "capabilityName": "Asset Condition Monitoring for Flowmeter",
  "protocol": "OPC-UA",
  "accessMode": "read-only",
  "recall": 0.94,
  "latencyMs": 45,
  "explanation": "..."
}
```

### 14.2 Match by Inline Fields (Mode B)

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{
    "protocol": "OPC-UA",
    "accessMode": "read-only",
    "minRecall": 0.90,
    "maxLatencyMs": 60
  }'
```

**Expected:** Returns a matching service or no-match response.

### 14.3 Persist a New Requirement

```bash
curl -X POST http://localhost:8080/api/v1/requirements \
  -H "Content-Type: application/json" \
  -d '{
    "functionClass": "FlowRate",
    "protocol": "OPC-UA",
    "accessMode": "read-only",
    "engineeringUnit": "l/min",
    "safetyIntegrityLevel": "SIL-0",
    "networkZone": "Plant",
    "determinismClass": "SoftRealTime"
  }'
```

**Expected Response:**
```json
{
  "requirementId": "REQ-a1b2c3d4"
}
```

### 14.4 Get Dropdown Options

```bash
curl http://localhost:8080/api/v1/options/protocol
```

**Expected:** `[{"value":"OPC-UA","label":"OPC-UA"},{"value":"PROFINET","label":"PROFINET"},...]`

### 14.5 Get All Option Field Names

```bash
curl http://localhost:8080/api/v1/options
```

**Expected:** `["protocol","accessMode","engineeringUnit","safetyIntegrityLevel","networkZone","determinismClass","informationModel"]`

### 14.6 Validation Error — Neither Mode

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Expected:** 400 Bad Request with validation error.

### 14.7 Validation Error — Both Modes

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId": "REQ-001", "protocol": "OPC-UA"}'
```

**Expected:** 400 Bad Request with validation error.

---

## 15. Health Endpoints

```bash
# Application health
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# System status
curl http://localhost:8080/api/v1/system/status
```

---

## 16. Theme and Responsive Design

| Step | Action | Expected Result |
|------|--------|-----------------|
| 16.1 | Click sun/moon icon in header | Theme toggles between light and dark mode |
| 16.2 | Resize browser to tablet width (~768px) | Navigation drawer collapses to hamburger menu |
| 16.3 | Resize browser to mobile width (~375px) | Navigation becomes overlay, content fills width |
| 16.4 | Click hamburger menu on mobile | Navigation drawer slides in as overlay |
| 16.5 | Refresh page | Last selected theme is remembered |

---

## 17. Evaluation Test Scenarios

These scenarios validate the three use cases from the paper.

### Use Case A: Flowmeter NE 107

```bash
# Match flowmeter diagnostics requirement
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId": "REQ-001"}'
```

**Expected:** SVC-FlowmeterDiagnostics matched, OPC-UA protocol, recall=0.94, latency=45ms.

### Use Case B: Fermentation Skid

```bash
# Match fermentation temperature control
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId": "REQ-002"}'
```

**Expected:** SVC-FermentationSkid matched, PROFINET protocol, SIL-2, HardRealTime.

### Use Case C: HVAC Plant

```bash
# Match HVAC temperature monitoring
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId": "REQ-003"}'
```

**Expected:** SVC-HVACTempAnalysis matched, BACnet protocol, SoftRealTime, OT-Zone-3.

### Cross-Plant Query

The SPARQL query `cross-plant-temperature.rq` finds all temperature-related services across both plants. Verify it via the Ontology Explorer or by loading the query manually.

---

## 18. Negative Test Scenarios

| Scenario | Input | Expected |
|----------|-------|----------|
| Wrong protocol | Protocol=HTTP in Mode B | No match returned |
| Nonexistent requirement | requirementId="REQ-999" | No match returned |
| Invalid XML upload | Upload a .txt file | Parser error message |
| Empty search | Search with no input | Empty results |
| Both match modes | requirementId + inline fields | 400 Bad Request |
| No match mode | Empty request body | 400 Bad Request |

---

## 19. Automated Test Suite

Run all 253 automated tests:

```bash
mvn clean test
```

**Expected:** BUILD SUCCESS with 253 tests, 0 failures, 0 errors.

Test categories:
- Unit tests: Ontology loading, SPARQL matching, parser parsing, manifest generation
- Integration tests: REST API endpoints, option loading, system status
- UI tests: View rendering, component instantiation, route annotations
- Evaluation tests: All 3 use cases, cross-plant queries, file parsing

---

## 20. Troubleshooting

| Issue | Solution |
|-------|----------|
| Application won't start | Check PostgreSQL is running, or use `test` profile for H2 |
| OPC UA connection refused | Java Milo server starts automatically; check port 4840 |
| Login fails | Use `user`/`password` credentials |
| Blank dashboard | Ontology may not have loaded; check `/actuator/health` |
| Search returns nothing | Try different search terms; check ontology has entities |
| Upload fails | Ensure file is valid XML (MTP AML) or JSON (OPC UA Datasheet) |
