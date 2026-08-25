# Use-Case Input Files

Sample **device-description input files** for the evaluation use cases described in the
Semantic SDA Mapper paper. Use them for manual UAT in the **Mapping Hub**
(`/mapping-hub`) by dragging a file into the upload dropzone, or to drive the
**REST matcher** (`POST /api/v1/requirements/match` with `{"requirementId":"..."}`).

> These are copies of the files under `src/main/resources/device-descriptions` and
> `src/test/resources/examples`. They are kept here so testers have a single,
> centralized location for UAT inputs.

## Use cases (paper §5)

| Folder | Paper use case | Input format | Expected matched service |
|--------|---------------|--------------|--------------------------|
| `REQ-001-flowmeter-diagnostics/` | A — Flowmeter (NE 107) diagnostics | OPC UA NodeSet (XML) | `SVC-FlowmeterDiagnostics` |
| `REQ-002-fermentation-skid/` | B — Fermentation Skid | MTP AML (XML) | `SVC-FermentationSkid` |
| `REQ-003-hvac-plant/` | C — HVAC Plant | OPC UA datasheet (JSON) | `SVC-HVACTempAnalysis` |
| `REQ-004-process-variable-analysis/` | D — Process Variable Analysis | Ontology-native (no upload) | `SVC-ProcessVariableAnalytics` |

## Quick check via REST

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId":"REQ-001"}'   # -> SVC-FlowmeterDiagnostics
```

## Reference format samples

`formats/` holds one representative file per supported ingest format (useful for
uploading through the Mapping Hub dropzone and verifying format detection):

| Format | Files |
|--------|-------|
| AAS | `formats/aas/motor-aas.json`, `formats/aas/pump-aas.json` |
| MTP AML | `formats/mtp/mixer-module-mtp.xml`, `formats/mtp/pump-module-mtp.xml` |
| OPC UA | `formats/opcua/pump-opcua-datasheet.json`, `formats/opcua/temperature-sensor-opcua-datasheet.json` |
| AutomationML / CAEX | `formats/automationml/sample-caex.aml` |

## Notes

- **REQ-004** is evaluated directly against the evaluation ABox
  (`src/main/resources/ontology/demo-abox-v2.ttl`) rather than an uploaded file; see
  `REQ-004-process-variable-analysis/README.md`. It exercises the Process Variable
  Analysis path (`queries/process-variable-analysis.rq`) with UCUM unit `degC` + IEC CDD
  IRDI `semanticID`.
- Negative scenarios (wrong protocol/unit/SIL) are covered by `EvaluationTest`; to
  reproduce manually, edit an input file's protocol/unit or use an empty `{}` match body.
