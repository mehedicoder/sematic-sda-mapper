# REQ-004 — Process Variable Analysis

**Paper "next steps" validation.** An analytics requirement over a temperature process
variable that carries a UCUM unit (`degC`) and an IEC CDD IRDI `semanticID`, exercising
R1–R3 **and** R2a (`requiresConcept` CDD coverage via `FILTER NOT EXISTS`).

## Input

This use case is **ontology-native**: it is defined directly in the evaluation ABox
(`src/main/resources/ontology/demo-abox-v2.ttl`) as `UC-PVA` / `REQ-004` with the
process variable `degC` + IRDI `semanticID`, so there is **no uploaded device-description
file** for it.

For manual Mapping Hub testing of an analytics-style description, reuse any of the
`formats/` samples (e.g. `formats/opcua/temperature-sensor-opcua-datasheet.json`).

## Expected result

- Matched service: **`SVC-ProcessVariableAnalytics`**
- Protocol: OPC-UA; R2a enforces that the analytics capability's required concepts (CDD
  IRDI coverage) are present.

## Negative checks (also in `EvaluationTest`)

A requirement asking for the **wrong** concept, **wrong** unit, or **wrong** SIL must
**not** match `SVC-ProcessVariableAnalytics`.

## How to test

```bash
curl -X POST http://localhost:8080/api/v1/requirements/match \
  -H "Content-Type: application/json" \
  -d '{"requirementId":"REQ-004"}'
```
