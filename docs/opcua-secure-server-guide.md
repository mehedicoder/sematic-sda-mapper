# OPC UA Secure Server with PLC Data — Reference Guide

## Overview

This guide covers building a production-ready, SSL-secured OPC UA server in Node-RED that pulls data from a PLC (Allen Bradley) over EtherNet/IP, stores it in context memory, and publishes it as a structured OPC UA information model with encrypted communication.

The 6-step architecture:

```
1. PLC tags configured
2. Node-RED reads PLC tags via EtherNet/IP
3. Tags stored in flow context memory
4. OPC UA server address space programmed from context
5. Server encrypted with SSL certificates
6. OPC UA client connects securely
```

---

## Custom Nodes Required

| Package | Purpose |
|---|---|
| `node-red-contrib-cip-ethernet-ip` | Read Allen Bradley PLC tags via EtherNet/IP |
| `node-red-contrib-opcua-server` | Compact OPC UA server node |
| `node-red-contrib-opcua` | Full OPC UA client/server nodes |

Install all three via Node-RED palette manager.

---

## Step 1: Read PLC Data via EtherNet/IP

### Configure Ethernet/IP Endpoint

| Setting | Value |
|---|---|
| PLC IP address | Match your Allen Bradley IP |
| CPU slot number | Match your PLC slot |
| Poll cycle time | 1000ms (change from default 500ms) |

### PLC Tags to Read

| Tag | Data Type | Description |
|---|---|---|
| Conveyor_RTS | BOOL | Conveyor Ready to Start |
| Robot_RTS | BOOL | Robot Ready to Start |
| Robot_Position | REAL | Robot arm position (degrees) |
| Conveyor_Running | BOOL | Conveyor is running |
| Line4_State | DINT | Line 4 Machine State |
| Line4_Fault | BOOL | Line 4 is faulted |

### eth-ip in Node Configuration

- Select the configured endpoint
- Mode: `All tags`
- Node reads all PLC tags when any value changes

### Verify Data Flow

Wire a debug node to the eth-ip in node. The output is a key/value dictionary:
```json
{
  "Conveyor_RTS": true,
  "Robot_RTS": false,
  "Robot_Position": 45.7,
  "Conveyor_Running": true,
  "Line4_State": 3,
  "Line4_Fault": false
}
```

---

## Step 2: Store PLC Data in Flow Context Memory

Use a **change** node to move each PLC tag from `msg.payload` into a `flow.conveyorData` object:

```
msg.payload.Conveyor_RTS  → flow.conveyorData.Conveyor_RTS
msg.payload.Robot_RTS     → flow.conveyorData.Robot_RTS
msg.payload.Robot_Position → flow.conveyorData.Robot_Position
...
```

Verify by checking Node-RED context data → flow → `conveyorData`. Values update on every PLC poll cycle.

---

## Step 3: Program the OPC UA Server Address Space

### Context Variable Initialization

```javascript
this.sandboxFlowContext.set("conveyorData.Conveyor_RTS", false);
this.sandboxFlowContext.set("conveyorData.Robot_RTS", false);
this.sandboxFlowContext.set("conveyorData.Robot_Position", 0);
this.sandboxFlowContext.set("conveyorData.Conveyor_Running", false);
this.sandboxFlowContext.set("conveyorData.Line4_State", 0);
this.sandboxFlowContext.set("conveyorData.Line4_Fault", false);
```

### Folder Hierarchy

```
Root/Objects/
└── Line 4 PLC/
    └── Conveyor/
        ├── Bools/
        │   ├── Conveyor Ready to Start  (ns=1;s=Conveyor_RTS, Boolean)
        │   ├── Robot Ready to Start     (ns=1;s=Robot_RTS, Boolean)
        │   ├── Conveyor Running         (ns=1;s=Conveyor_Running, Boolean)
        │   └── Line 4 Faulted           (ns=1;s=Line4_Fault, Boolean)
        ├── DINTs/
        │   └── Line 4 State             (ns=1;s=Line4_State, Int32)
        └── Floats/
            └── Robot Axis A1 Position   (ns=1;s=Robot_Position, Float)
```

### Node Definition Pattern

```javascript
const Conveyor_RTS = namespace.addVariable({
    "organizedBy": conveyorBools,
    "browseName": "Conveyor Ready to Start",
    "nodeId": "ns=1;s=Conveyor_RTS",
    "dataType": "Boolean",
    "value": {
        "get": function () {
            return new Variant({
                "dataType": DataType.Boolean,
                "value": flexServerInternals.sandboxFlowContext.get("conveyorData.Conveyor_RTS")
            });
        },
        "set": function (variant) {
            flexServerInternals.sandboxFlowContext.set(
                "conveyorData.Conveyor_RTS",
                variant.value
            );
            return opcua.StatusCodes.Good;
        }
    }
});
```

### Browser Views

Create custom browsing hierarchies:

```javascript
const viewBools = namespace.addView({
    "organizedBy": rootFolder.views,
    "browseName": "Line 4 Conveyor Bools"
});

viewBools.addReference({
    "referenceType": "Organizes",
    "nodeId": Conveyor_RTS.nodeId
});
viewBools.addReference({
    "referenceType": "Organizes",
    "nodeId": Robot_RTS.nodeId
});
// ... add more references
```

---

## Step 4: SSL Encryption

### Server-Side Certificate Setup

1. Navigate to `node-red-contrib-opcua-server` certificates directory:
   ```
   ~/.node-red/node_modules/node-red-contrib-opcua-server/certificates/
   ```

2. Key files:
   | File | Purpose |
   |---|---|
   | `server_selfsigned_cert_2048.pem` | Public certificate |
   | `server_key_2048.pem` | Private key |

3. In Compact Server node → Security tab:
   - Uncheck `Allow Anonymous`
   - Check `Use individual Certificate Files`
   - Set full absolute paths to both cert files

4. Redeploy and verify server reports `active`

### Client-Side Certificate Setup

1. Copy cert files to a location accessible by the OPC UA client
2. Convert PEM to DER format for the client:
   ```bash
   openssl x509 -in server_selfsigned_cert_2048.pem \
                -out server_selfsigned_cert_2048.der \
                -outform DER
   ```

3. Client needs:
   | File | Format | Purpose |
   |---|---|---|
   | `server_selfsigned_cert_2048.der` | DER | Public key (client trusts server) |
   | `server_key_2048.pem` | PEM | Private key (if mutual auth) |

### Client Connection Settings

| Setting | Value |
|---|---|
| Security mode | Sign & Encrypt |
| Security policy | Aes128Sha256RsaOaep |
| Authentication | Certificate and Private Key |
| Public key | `server_selfsigned_cert_2048.der` |
| Private key | `server_key_2048.pem` |

First connection prompts certificate acceptance — select "Accept Permanently".

---

## Connection Flow Summary

```
PLC (Allen Bradley)
  │ EtherNet/IP (CIP)
  ▼
Node-RED (eth-ip in node)
  │ msg.payload
  ▼
Change node → flow.conveyorData
  │ context read
  ▼
Compact Server (address space)
  │ OPC UA (SSL: SignAndEncrypt / Aes128Sha256RsaOaep)
  ▼
OPC UA Client (Prosys Browser, Java Milo, etc.)
```

---

## Mapping to Our Project

| Tutorial Concept | Our Project Equivalent |
|---|---|
| Allen Bradley PLC via EtherNet/IP | Fermentation Skid / HVAC Plant (simulated) |
| `flow.conveyorData` context | `process_state` object in `docs/opcua-tank-simulation-server.js` |
| Folder hierarchy | `Root/Objects/Tank1/` or `Root/Objects/Fermentation/` |
| SSL certificates | Not configured for MVP (anonymous access) |
| Prosys OPC UA Browser | Apache Milo Java client |

### Our Address Space Structure

```
Root/Objects/
├── Tank1/                    (Approach B + C)
│   ├── Level                 (ns=2;s=Level, Double)
│   ├── FlowRateIn            (ns=2;s=FlowRateIn, Double)
│   ├── FlowRateOut           (ns=2;s=FlowRateOut, Double)
│   └── Temperature           (ns=2;s=Temperature, Double)
├── Fermentation/             (Approach A, ns=2)
│   ├── TempSetpoint
│   ├── TempActual
│   └── AgitatorSpeed
└── HVAC/                     (Approach A, ns=3)
    ├── AHU_TempSupply
    ├── AHU_TempReturn
    └── ActivePower
```

---

## Security for Production (Future Work)

| Requirement | Implementation |
|---|---|
| SSL certificates | Self-signed or CA-signed, PEM + DER |
| Security policy | Basic256Sha256 or Aes128Sha256RsaOaep |
| Security mode | SignAndEncrypt |
| Anonymous access | Disabled |
| User authentication | Username/password or X.509 certificate |
| Certificate exchange | Server trusts client cert, client trusts server cert |

---

## References

- [FlowFuse: Building Secure OPC-UA Server in Node-RED](https://flowfuse.com/node-red/protocol/opc-ua/)
- [FlowFuse: How to Deploy a Basic OPC-UA Server in Node-RED](https://flowfuse.com/blog/2023/07/how-to-deploy-a-basic-opc-ua-server-in-node-red/)
- [FlowFuse: How to Build an OPC UA Client Dashboard in Node-RED](https://flowfuse.com/blog/2023/07/how-to-build-a-opc-client-dashboard-in-node-red/)
- [node-red-contrib-opcua-server](https://flows.nodered.org/node/node-red-contrib-opcua-server)
- [node-red-contrib-cip-ethernet-ip](https://flows.nodered.org/node/node-red-contrib-cip-ethernet-ip)
- [Our OPC UA Server Guide](./opcua-node-red-server-guide.md)
- [Our OPC UA Client Guide](./opcua-node-red-client-guide.md)
