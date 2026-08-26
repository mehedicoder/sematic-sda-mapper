# REQ-001 — Flowmeter (NE 107) diagnostics

**Paper use case A.** A flowmeter exposing NE 107 status/diagnostics, described as an
OPC UA NodeSet.

## Input

- `req001-uaserver-nodeset-complete.xml` — OPC UA NodeSet (XML) for the flowmeter UA server.

## Expected result

- Matched service: **`SVC-FlowmeterDiagnostics`**
- Protocol: OPC-UA, recall ≈ 0.94, latency ≈ 45 ms, `lifecycleStatus=Matched`.

## How to test

1. Mapping Hub: drag `req001-uaserver-nodeset-complete.xml` into the dropzone → pipeline
   runs → coverage dashboard + matched service appear.
2. REST: `POST /api/v1/requirements/match` with `{"requirementId":"REQ-001"}`.

## Source

`src/test/resources/examples/req001-uaserver-nodeset-complete.xml`
