# Semantic SDA Mapper — User Acceptance Test (UAT) Manual

**Scope:** Full user acceptance testing of the Semantic SDA Mapper, including the
Mapping Hub, Ontology Visualizer, Session overview, Service/Discovery Catalogs,
Settings (incl. LLM translation), OPC UA Monitor, the REST API, and the paper-derived
use cases. Covers the completed roadmap phases 1–21, specifically the recently added
**Phase 15** (capability typing / PlantUnit), **Phase 16** (Process Variable Analysis),
**Phase 17** (Deployment Manager orchestration), **Phase 19** (AutomationML / CAEX
ingest), the configurable **LLM-based semantic translation** (Phase 18) and
**Phase 21** (end-to-end pipeline observability).

**Prerequisites**

- Java 25+
- Maven 3.9+
- PostgreSQL (or run with the `test` profile / H2 for a quick smoke test)
- A modern browser (Chrome, Firefox, Edge)
- For LLM translation only: an API key from OpenRouter (free tier) or any
  OpenAI-compatible provider

**Application URL:** http://localhost:8080

---

## 1. Starting the Application

```bash
mvn spring-boot:run
# or run com.sda.mapper.SemanticSdaMapperApplication
```

- The app starts at **http://localhost:8080**.
- The OPC UA simulation server starts automatically on port **4840**.
- On first load you are presented with the login page.

**Login (default):** `user` / `user` or `admin` / `admin` (form login; security is
active outside the `test` profile).

---

## 2. Navigation Overview

After login the left navigation drawer (MainLayout) shows:

| Link | Route | Purpose |
|------|-------|---------|
| Ontology Browser | `/` | Interactive CSS-SDA ontology visualizer |
| Mapping Hub | `/mapping-hub` | Universal ingest → parse → lift → infer → match → manifest |
| Sessions | `/sessions` | Per-user mapping-session overview (complete / incomplete) |
| Service Catalog | `/catalog` | Browsable SDA service catalog |
| Discovery Catalog | `/catalog/discover` | Discoverable servers / capabilities |
| OPC UA Monitor | `/opcua-visualization` | Live OPC UA server + node values |
| Settings | `/settings` | Appearance + LLM translation configuration |

(Legacy standalone Dashboard / Upload / Deployment / Requirement Form / Search views
were consolidated into the Mapping Hub and the API.)

---

## 3. Ontology Visualizer (`/`)

| Step | Action | Expected |
|------|--------|----------|
| 3.1 | Open the app | Ontology Visualizer loads at `/` with a dark-themed layout |
| 3.2 | Inspect the summary pills | Shows layer / class / relation counts (24 classes, 85+ relations, 5 layers) |
| 3.3 | Click a layer tab | Class list filters to that layer |
| 3.4 | Click a class (e.g. `Service`) | Detail panel shows attributes + core/supporting relations |
| 3.5 | Click a related class from a relation | Neighborhood graph navigates to it |
| 3.6 | Toggle light/dark (header icon) | Theme switches and is preserved on refresh |

**Phase 15 check:** the ontology now carries the capability-typing relation
`sda:requiresFunction` (inverse `sda:requiredByCapability`) linking a `Capability` to an
`AnalyticalFunction` / `ControlFunction`, and the `PlantUnit` reference concept. These are
reference (non-numbered) concepts; verify their presence via the Visualizer relation
view or a SPARQL probe (`match-service.rq` exercises them). The `sda:operatesOn` range
was widened to `Asset` **and** `PlantUnit`.

---

## 4. Mapping Hub (`/mapping-hub`)

The Hub is the single place to ingest device descriptions and run the semantic pipeline.

### 4.1 Upload / Ingest

| Step | Action | Expected |
|------|--------|----------|
| 4.1.1 | Open Mapping Hub | Upload dropzone + console + layer tabs + coverage dashboard render |
| 4.1.2 | Drag an **AAS .xml** file | Detected as AAS, parsed, triples/canonical entities listed in the console |
| 4.1.3 | Drag an **MTP .aml / .xml** file | Detected as MTP AML, parsed (Service/Communication/DataAssembly) |
| 4.1.4 | Drag an **OPC UA .json** datasheet | Detected as OPC UA, parsed |
| 4.1.5 | Drag a generic **AutomationML / CAEX .aml / .xml** file | Detected as `AutomationML` (not MTP) and parsed into `AutomationML` canonical entities — **Phase 19** |
| 4.1.6 | Upload an invalid / non-CAEX file | Clear `ParsingException` error message, no crash |

> Detection order matters: MTP markers (`RoleClassLibrary` / `Service`) are checked
> **first**; a plain CAEX/AutomationML file then falls through to the `AutomationML`
> format, so AML is never misclassified as MTP.

### 4.2 Pipeline, Coverage & Matching

| Step | Action | Expected |
|------|--------|----------|
| 4.2.1 | On load (or click **Run Pipeline**) | Pipeline runs Parse → Lift → Infer → Match → Manifest; stages logged in the console |
| 4.2.2 | Inspect the **Coverage Dashboard** | Per-layer coverage % and gap counts for the 24 numbered classes |
| 4.2.3 | Open a class with a gap (red) | Detail panel shows gap actions (e.g. manual entry / supplementary upload) |
| 4.2.4 | Click a matched service in results | Shows the matched `Service`, `Skill`, `CommunicationBinding`, `DataConnector`, deployment context |
| 4.2.5 | Click **Export** | Manifest (Docker Compose + Kubernetes YAML) is produced/previewed |

### 4.3 Pipeline Observability Console · **Phase 21**

Embedded in the Mapping Hub below the dashboard: a **Stage Timeline**, a **Trace
Panel** and a streaming **Pipeline Console**. Server push is enabled, so events
appear without manual refresh.

| Step | Action | Expected |
|------|--------|----------|
| 4.3.1 | Click **Run Pipeline** (or upload a file) | Timeline chips switch waiting → running → succeeded with per-stage durations; no manual refresh needed |
| 4.3.2 | Watch the Trace Panel during the run | Current run id plus one row per stage (PARSE / INSTANTIATE / LIFT / INFER / MATCH) with status and duration |
| 4.3.3 | Watch the Pipeline Console | One `[HH:mm:ss] [STAGE] STARTED/SUCCEEDED (Xms)` line per transition, newest at bottom |
| 4.3.4 | Force a **no-match** run (e.g. match an unknown requirement id via §8.4) | MATCH chip/trace shows failure; audit detail names the failed dimensions (capability / protocol / deployment / R2a concept), consistent with the user-facing explanation |
| 4.3.5 | Query history after the run | `GET /api/v1/pipeline-audits?sessionId=…` (or `?requirementId=…`, `?runId=…`) returns the chronological timeline — see §10.6 |

Every stage transition is persisted to `pipeline_audit_log` (Flyway V6) and every
stage emits a child span under one `sda.pipeline.run` trace per pipeline run; logs
carry `runId` / `sessionId` / `requirementId` MDC entries plus trace/span ids.

### 4.4 Manual Entry

| Step | Action | Expected |
|------|--------|----------|
| 4.3.1 | Select a class in the browser | `ManualEntryFormFactory` renders ontology-driven fields for that class |
| 4.3.2 | Fill required attributes and Save | Entity is added to the canonical model; coverage updates |
| 4.3.3 | Submit an entity missing a required attribute | Validation error, entity not added |

### 4.5 UAT Input Files & Combined Matching Sessions

The sample upload files for the paper's use cases are collected in a single
canonical location: `docs/usecases/`. They can be dragged into the Mapping Hub
dropzone for manual UAT, or used to drive the REST matcher. (The previous
duplicate copies under `docs/use-case-inputs/formats/` have been removed, and the
per-use-case bundles were moved out of `src/main/resources/device-descriptions/`
into `docs/usecases/` so all use-case packages live with their documentation.)

They are organized **by use case** under `docs/usecases/REQ-*`. Each such folder
holds every file you upload together for that use case — the device
description(s) **and** the requirement file(s) — so a multi-file session is
stored in one place. The folders contain only upload/test inputs (no
documentation files); testing steps live in this manual.

#### Per-use-case upload (one folder per session)

| File (under `docs/usecases/`) | Use case | Expected matched service |
|-------------------------------------------------------------|----------|--------------------------|
| `REQ-001-flowmeter-diagnostics/req001-uaserver-nodeset-complete.xml` | A — Flowmeter (NE 107) | `SVC-FlowmeterDiagnostics` |
| `REQ-002-fermentation-skid/req002-mtp-fermentation-skid-complete.xml` | B — Fermentation Skid | `SVC-FermentationSkid` |
| `REQ-003-hvac-plant/req003-uaserver-ahu01-complete.json` | C — HVAC Plant | `SVC-HVACTempAnalysis` |
| `REQ-004-process-variable-analysis/` (no upload file — ontology-native) | D — Process Variable Analysis | `SVC-ProcessVariableAnalytics` (via `requirementId`) |
| `REQ-001-flowmeter-diagnostics/pump-aas.json`, `REQ-001-flowmeter-diagnostics/pump-module-mtp.xml`, `REQ-001-flowmeter-diagnostics/pump-opcua-datasheet.json`, `docs/usecases/reference-formats/sample-caex.aml` | format smoke tests | respective service / category |

#### Supplementary requirement upload

`docs/usecases/REQ-001-flowmeter-diagnostics/requirement-flowdiagnostics.ttl` can be uploaded through the Mapping
Hub dropzone to match the seeded Flowmeter Diagnostics Service via its
`FlowDiagnostics` capability (see `docs/usecases/REQ-001-flowmeter-diagnostics/requirement-flowdiagnostics.ttl`).

#### Pump Asset Health Monitoring use-case package (ABox TTL)

The `docs/usecases/Pump Asset Health Monitoring/` package is a complete SDA ABox
described with four Turtle files. Upload them together into the Mapping Hub
dropzone (multi-select or one at a time — the session accumulates):

| File | Detected as | Role |
|------|-------------|------|
| `req_pump_health.ttl` | Requirement TTL | Demand side — `REQ-03` → capability `AssetConditionMonitoring` |
| `catalogue_services.ttl` | **ABox TTL** | Supply side — `Service_Diagnostics_v1` (NE107), skills, bindings, connectors |
| `catalogue_assets.ttl` | **ABox TTL** | Layer 4 — physical asset, sensors, process variables, IEC CDD semantic concepts |
| `edge_nodes.ttl` | **ABox TTL** | Layer 2b — deployment context, cluster node, network zone, policy |

Behaviour:

- ABox TTL files (`catalogue_*`, `edge_*`) are **merged directly into the shared
  Service Catalogue model** (the same model the matcher queries), so they
  *update the catalogue service/asset/edge list*. This is how an uploaded
  `catalogue_services.ttl` augments the catalogue rather than a per-session copy.
- Uploaded ABox triples are removed again when you delete that file from the
  session, or when you **Reset** the session (so the shared model returns to its
  seeded state).
- After all four files are uploaded, the MATCH stage reports `Service_Diagnostics_v1`
  (skill `Skill_NE107_Diag`, offered capability `OCap_AssetConditionMonitoring`) as the
  semantic capability match for `REQ-03`.

> Note: because ABox TTL updates the shared catalogue, it persists for the life of
> the application process. Use **Reset** (or restart the app) to clear uploaded
> catalogue data between runs.

#### Combined "full paper" session (upload everything together)

1. Open **Mapping Hub** once and stay on the view — the canonical model **accumulates**
   across uploads within the session.
2. Drag in, one after another (or multi-select):
     - `docs/usecases/REQ-001-flowmeter-diagnostics/req001-uaserver-nodeset-complete.xml`
     - `docs/usecases/REQ-002-fermentation-skid/req002-mtp-fermentation-skid-complete.xml`
     - `docs/usecases/REQ-003-hvac-plant/req003-uaserver-ahu01-complete.json`
     - optionally `docs/usecases/REQ-003-hvac-plant/temperature-sensor-opcua-datasheet.json` for a PVA-style temperature source
3. After **each** upload the pipeline re-runs Parse → Lift → Infer → Match over all
   accumulated entities; the Coverage Dashboard and matched-services list grow.
4. Expected matched services in the combined session:
   `SVC-FlowmeterDiagnostics`, `SVC-FermentationSkid`, `SVC-HVACTempAnalysis`
   (and `SVC-ProcessVariableAnalytics` once a PVA-style input is present or REQ-004 is
   matched via REST).
5. Confirm the Coverage Dashboard reflects the union of all uploaded entities and that
   gaps/red items are limited to reference concepts (Phase 15 non-numbered classes).

> **REQ-004 note:** Process Variable Analysis is evaluated against the demo ABox
> (`demo-abox-v2.ttl`) already loaded. To include it in a session, call
> `POST /api/v1/requirements/match {"requirementId":"REQ-004"}`, or upload a temperature
> process-variable description and confirm it lifts with a `degC` unit + IEC CDD IRDI
> `semanticID`.

---

## 5. Session Overview (`/sessions`)

| Step | Action | Expected |
|------|--------|----------|
| 5.1 | Open Sessions | Grid of mapping sessions with status chips (Complete / Incomplete) |
| 5.2 | Verify summary counts | Per-user aggregate (including an "Unassigned" bucket) |
| 5.3 | Filter by status / user / search | Grid narrows accordingly |
| 5.4 | Open a session | Shows coverage %, gaps, matched services |
| 5.5 | Refresh / wait 30s | Grid auto-refreshes |

Sessions are user-attributed; incomplete vs complete is derived from persisted
coverage (100% & 0 blocking gaps = complete; `FAILED` is never complete).

---

## 6. Service & Discovery Catalogs (`/catalog`, `/catalog/discover`)

| Step | Action | Expected |
|------|--------|----------|
| 6.1 | Open Service Catalog | Browsable list of SDA services with capabilities |
| 6.2 | Open a service | Shows its capability, skill, bindings, connectors |
| 6.3 | Open Discovery Catalog | Lists discoverable servers/capabilities |
| 6.4 | Call `GET /api/v1/catalog/discover/servers` | JSON list of known/discoverable servers |

---

## 7. OPC UA Monitor (`/opcua-visualization`)

| Step | Action | Expected |
|------|--------|----------|
| 7.1 | Open OPC UA Monitor | Server grid (built-in Tank Server = Connected) |
| 7.2 | Click "Built-in Tank Server" | Detail panel with browsed nodes (Level, FlowRateIn/Out, Temperature, Pump) and live values |
| 7.3 | Wait ~5s | Node values refresh automatically |
| 7.4 | Click "Back to Servers" | Returns to grid |

---

## 8. Settings — LLM Semantic Translation (`/settings`)  · **Phase 18**

The LLM feature is **opt-in** and configured by the user (no key is bundled).

### 8.1 Configure via the Settings UI

| Step | Action | Expected |
|------|--------|----------|
| 8.1.1 | Open Settings → "LLM Semantic Translation" | Form with Enable, Provider, API key, Model, Base URL |
| 8.1.2 | Set Provider = `OPENROUTER` (default) | Base URL field hidden (OpenRouter default used) |
| 8.1.3 | Paste an OpenRouter API key, set Model to a free model (e.g. `openrouter/free`) | Fields accept values |
| 8.1.4 | Enable the checkbox and click **Save LLM settings** | "LLM settings saved" notification; settings persist on the running bean |
| 8.1.5 | Set Provider = `CUSTOM` or `AZURE` | Base URL field appears and is required |
| 8.1.6 | (Optional) Provider = `OPENAI` / `OLLAMA` | Works with the corresponding key / local endpoint |

### 8.2 Configure via environment / `application.yml`

```bash
export APP_LLM_ENABLED=true
export APP_LLM_PROVIDER=OPENROUTER
export APP_LLM_APIKEY=sk-or-...your-openrouter-key
export APP_LLM_MODEL=openrouter/free
```

Equivalent `app.llm.*` keys exist in `src/main/resources/application.yml` (the key is
never committed; supply it via env var).

### 8.3 Test the LLM translation

| Step | Action | Expected |
|------|--------|----------|
| 8.3.1 | `GET /api/v1/llm/status` | Returns `{enabled, provider, model, configured}` — **API key is never returned** |
| 8.3.2 | `POST /api/v1/llm/translate` with `{"format":"AutomationML","content":"<CAEX>...</CAEX>"}` | Returns JSON `TranslationResult` with `hints` (className/label/reason) mapping the description to SDA ontology concepts |
| 8.3.3 | With LLM disabled / no key | `POST /translate` returns **409** with `llm_not_configured` |
| 8.3.4 | Inspect `TranslationResult.raw` | Contains the raw model response for transparency |

The client speaks the OpenAI-compatible Chat Completions API, so OpenRouter (default,
free models), OpenAI, local Ollama, and any custom endpoint are all selectable.

### 8.4 User-defined requirements (generic matching)

Matching is now driven by a **single generic SPARQL query** (`match-generic.rq`) that
serves *any* requirement modeled with the standard SDA properties — no per-requirement
query file is needed. A requirement may be predefined (e.g. `REQ-001`…`REQ-004` from the
seeded catalog) or **user-defined** via the UI or REST.

The requirement definition & matching UI is embedded at the bottom of the
**Mapping Hub** (`/mapping-hub`) under the *Requirement Definition & Matching* section
(the former standalone `/requirement` view has been consolidated into the hub):

1. Enter a `Requirement ID` (mandatory), `Requirement name`, `Requirement description`,
   and a `Capability type` (e.g. `FlowmeterDiagnostics`, `ProcessVariableAnalysis`).
2. Optionally add constraints: `Protocol`, `Access mode`, `Min recall`, `Max latency`,
   `Network zone`, `Safety integrity level`, `Determinism class`.
3. Click **Create Requirement** (persists into the live ontology) and/or
   **Create & Match** (persists then matches). Results show the matched service,
   capability, protocol, recall, latency, and deployment context.
4. To match a seeded use case, enter its ID in **Match existing requirement by ID**
   (e.g. `REQ-001`) and click **Match**. You may enter **any** ID — if it does not
   yet exist in the model, providing a **Capability type** (or constraints) in the
   form above creates it on the fly and matches immediately (e.g. ID `REQ-ABC` with
   Capability type `ProcessVariableAnalysis` returns the Process Variable Analytics
   service). Unknown IDs with no capability/constraints return "no match".

**Via REST:**
```bash
# Persist a user-defined requirement
curl -X POST http://localhost:8080/api/v1/requirements \
  -H "Content-Type: application/json" \
  -d '{"requirementId":"REQ-USER-1","requirementName":"My analytics req",
       "capabilityType":"ProcessVariableAnalysis","protocol":"OPC-UA","accessMode":"read-only"}'

# Match it back
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" -d '{"requirementId":"REQ-USER-1"}'
```
A requirement with no `capabilityType` falls back to constraint-only (protocol/recall/
latency/…) matching, preserving the previous dynamic behaviour.

---

## 9. Deployment Manager Orchestration  · **Phase 17**

The Deployment Manager applies the generated manifest to a target environment with
**dry-run, health-gated rollout, and rollback**.

- The manifest (Docker Compose / Kubernetes) is generated by the existing manifest
  services; the K8s `nodeSelector` is already derived from the requirement's
  `boundContext` (deployment context).
- The shipped backend is **`LocalDryRunBackend`** (no external cluster required): it
  validates the YAML, simulates `Pending → Applying → Ready`, probes a health checker,
  and **rolls back** if the health gate fails. A real **Kubernetes backend**
  (`OrchestrationTarget.Type.KUBERNETES`) is a documented extension point that needs a
  Kubernetes client dependency + infrastructure approval and is **not** shipped.
- **UAT:** the orchestration logic is covered by automated tests
  (`LocalDryRunBackendTest`, `DeploymentManagerServiceTest`). For a manual check,
  generate a manifest in the Mapping Hub (§4.2.5) and confirm the YAML is valid and
  contains the expected `nodeSelector` constraints; the apply/rollback behaviour is
  verified by `mvn test`.

---

## 10. REST API

### 10.1 Match by Requirement ID (Mode A)

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId":"REQ-001"}'
```
→ `SVC-FlowmeterDiagnostics`, OPC-UA, recall ≈ 0.94, latency ≈ 45 ms, `lifecycleStatus=Matched`.

### 10.2 Match by Inline Fields (Mode B)

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"protocol":"OPC-UA","accessMode":"read-only","minRecall":0.90,"maxLatencyMs":60}'
```

### 10.3 Options & System Status

```bash
curl http://localhost:8080/api/v1/options/protocol
curl http://localhost:8080/api/v1/system/status
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

### 10.4 Pipeline & Catalog

```bash
curl -X POST http://localhost:8080/api/v1/pipeline/execute -H "Content-Type: application/json" -d '{}'
curl http://localhost:8080/api/v1/catalog/discover/servers
```

### 10.5 LLM (Phase 18)

```bash
curl http://localhost:8080/api/v1/llm/status
curl -X POST http://localhost:8080/api/v1/llm/translate \
  -H "Content-Type: application/json" \
  -d '{"format":"AAS","content":"<...>"}'
```
example 1: A flowmeter asset named "Cooling Water Flowmeter" exposes an OPC UA
           CommunicationBinding at opc.tcp://192.168.1.60:4840 using the OPC-UA protocol.
           It provides a "Diagnose Clogging" skill and a "Flowmeter Diagnostics" service.
           It includes a flow ProcessVariable and a diagnostic Status sensor, both
           reachable over OPC UA.

example 2: An HVAC asset named "AHU-01" exposes a BACnet/OPC UA CommunicationBinding at
           opc.tcp://192.168.1.70:4840 using the OPC-UA protocol. It provides a
           "Regulate Temperature" skill and an "HVAC Temperature Monitoring" service,
           with a supply-air temperature Sensor reachable over OPC UA.
example 3: A pump asset named "Cooling Pump P-101" exposes an OPC UA CommunicationBinding at
           opc.tcp://192.168.1.80:4840 using the OPC-UA protocol. It provides a
           "Monitor Condition" skill and a "Pump Asset Condition Monitoring" service,
           with a vibration Sensor and a status ProcessVariable reachable over OPC UA.


### 10.6 Pipeline audit history & observability SLIs · **Phase 21**

The API is form-login protected; run the calls from an authenticated browser session
or use the `test` profile for unauthenticated smoke tests.

```bash
# Chronological audit timeline (all filters optional; limit default 200, max 1000)
curl "http://localhost:8080/api/v1/pipeline-audits?requirementId=REQ-001&limit=50"
curl "http://localhost:8080/api/v1/pipeline-audits?sessionId=<session-id>"
curl "http://localhost:8080/api/v1/pipeline-audits?runId=<run-id>"
```

Each element: `{id, runId, sessionId, requirementId, stage, status, startedAt,
durationMs, detail}` ordered by `started_at`. `limit` outside 1–1000 → **400**
ProblemDetail.

```bash
# Observability SLIs exposed at the standard endpoint
curl -s http://localhost:8080/actuator/prometheus | grep sda_pipeline
#   sda_pipeline_runs_total{path,outcome}
#   sda_pipeline_match_result_total{result="hit|no-hit"}
#   sda_pipeline_stage_duration_seconds{stage}
curl -s http://localhost:8080/actuator/prometheus | grep sda_sparql
#   sda_sparql_query_seconds{query} / sda_sparql_results_total{query}
```

A ready-made Grafana dashboard visualizing these metrics ships at
`docs/grafana/sda-mapper-pipeline-dashboard.json`.

### 10.7 Validation errors

- Empty body `{}` → **400** (Mode B requires at least one field).
- Both `requirementId` **and** inline fields → **400**.

---

## 11. Evaluation / Paper Use-Case Scenarios

The input files for these scenarios are in `docs/usecases/`
(see §4.5 for the combined-session upload workflow). Match each requirement via
`POST /api/v1/requirements/match`:

| Use case | requirementId | Expected service | Notes |
|----------|---------------|------------------|-------|
| A — Flowmeter NE 107 | `REQ-001` | `SVC-FlowmeterDiagnostics` | OPC-UA, recall ≥ 0.90 |
| B — Fermentation Skid | `REQ-002` | `SVC-FermentationSkid` | PROFINET, SIL-2, HardRealTime |
| C — HVAC Plant | `REQ-003` | `SVC-HVACTempAnalysis` | BACnet, SoftRealTime, OT-Zone-3 |
| **D — Process Variable Analysis** | `REQ-004` | `SVC-ProcessVariableAnalytics` | **Phase 16** — analytics over a process variable with UCUM unit (`degC`) + IEC CDD IRDI semanticID; R2a concept-coverage enforced |

**Process Variable Analysis (Phase 16) detail:**

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId":"REQ-004"}'
```
→ `SVC-ProcessVariableAnalytics`, OPC-UA, with the process variable carrying the
`degC` unit and an IEC CDD IRDI `semanticID`. **Negative checks** (also in
`EvaluationTest`): a requirement asking for the *wrong* concept, *wrong* unit, or
*wrong* SIL must **not** match `REQ-004`.

**Cross-plant:** `cross-plant-temperature.rq` returns temperature services across both
plants (verify via Ontology Visualizer / a SPARQL run).

---

## 12. Negative Test Scenarios

| Scenario | Input | Expected |
|----------|-------|----------|
| Wrong protocol (Mode B) | `protocol=HTTP` | No match |
| Unknown requirement | `requirementId=REQ-999` | No match |
| Invalid upload | non-CAEX / corrupt XML | Clear parse error |
| LLM disabled | `POST /api/v1/llm/translate` | 409 `llm_not_configured` |
| Both match modes | `requirementId` + inline fields | 400 |
| Empty match body | `{}` | 400 |

---

## 13. Automated Test Suite

```bash
mvn clean test
```

**Expected:** `BUILD SUCCESS` — **556 tests, 0 failures, 0 errors**.

Coverage includes: ontology loading, SPARQL matching, parsers (AAS/MTP/OPC UA/AutomationML),
manifest generation, Mapping Hub pipeline/UI, Session overview, Ontology Visualizer,
REST API, evaluation use cases (incl. Process Variable Analysis), capability typing
(Phase 15), Deployment Manager orchestration (Phase 17), LLM translation
(Phase 18, stubbed transport — no external calls), and end-to-end pipeline
observability (Phase 21: trace shape, audit persistence/query, SPARQL metrics,
dimension span tags, Grafana JSON parse, live console components).

---

## 14. Acceptance Criteria Checklist

- [ ] App starts; login works; navigation shows all 7 routes.
- [ ] Ontology Visualizer renders 24 classes / 5 layers; Phase 15 relations present.
- [ ] Mapping Hub ingests AAS, MTP, OPC UA **and** AutomationML/CAEX (Phase 19) without MTP misclassification.
- [ ] Pipeline runs; coverage dashboard + matching work; manifest exports.
- [ ] **Phase 21:** Stage Timeline chips advance live during a run without refresh; Trace Panel and Pipeline Console fill in; no-match runs expose failed dimensions.
- [ ] **Phase 21:** `/api/v1/pipeline-audits` returns the persisted timeline; `/actuator/prometheus` exposes `sda_pipeline_*` / `sda_sparql_*`.
- [ ] Sessions overview shows complete/incomplete with filters.
- [ ] OPC UA Monitor shows live tank values.
- [ ] Settings LLM form saves; `/api/v1/llm/status` and `/translate` behave (409 when unconfigured).
- [ ] REST match returns REQ-001/002/003/004 correctly; negatives excluded.
- [ ] `mvn clean test` → 556 green.

---

## 15. Troubleshooting

| Issue | Solution |
|-------|----------|
| App won't start | Ensure PostgreSQL is up, or run with the `test` profile (H2). |
| Login fails | Use `user` / `user` or `admin` / `admin` (form login active outside `test` profile). |
| OPC UA refused | Milo server auto-starts on 4840; check the port. |
| Upload misclassified | Generic AutomationML must not contain MTP markers; it routes to `AutomationML`. |
| LLM 409 | Enable the feature and supply `APP_LLM_APIKEY` (or use the Settings form). |
| Blank dashboard / no entities | Check `/actuator/health`; confirm ontology loaded. |
| Manifest missing nodeSelector | The K8s `nodeSelector` is derived from the requirement's `boundContext`; ensure the match carried a deployment context. |
| Timeline/console not updating | Server push is enabled app-wide (`SdaAppShell`); check the browser console for a failed WebSocket/push connection and reload the page once. |
| Pipeline audits empty | Audit rows persist per executed stage — run the pipeline first; if rows are still missing, check the app log for `Failed to persist pipeline audit event` (persistence errors never abort the pipeline). |
| `pipeline-audits` 401/403 | The API requires form login; authenticate in the same browser session or use the `test` profile. |
