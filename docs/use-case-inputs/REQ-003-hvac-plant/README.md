# REQ-003 — HVAC Plant

**Paper use case C.** An HVAC plant (AHU-01) described as an OPC UA datasheet/NodeSet.

## Input

- `req003-uaserver-ahu01-complete.json` — full OPC UA datasheet (JSON) for the AHU-01 server.
- `req003-uaserver-ahu01.json` — trimmed OPC UA datasheet variant.

## Expected result

- Matched service: **`SVC-HVACTempAnalysis`**
- Protocol: BACnet, SoftRealTime, deployment context `OT-Zone-3`.

## How to test

1. Mapping Hub: drag `req003-uaserver-ahu01-complete.json` into the dropzone.
2. REST: `POST /api/v1/requirements/match` with `{"requirementId":"REQ-003"}`.

## Source

`src/test/resources/examples/req003-uaserver-ahu01-complete.json`,
`src/test/resources/examples/req003-uaserver-ahu01.json`
