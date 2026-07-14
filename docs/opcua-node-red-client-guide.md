# OPC UA Client Dashboard in Node-RED — Reference Guide

## Overview

This guide covers building an OPC UA client application in Node-RED that connects to a third-party OPC UA server to browse the address space, read/write values, and display alarms/events on a dashboard.

The client communicates over the standardized OPC UA protocol — it works with any OPC UA server regardless of implementation language (Java, JavaScript, C++, C#).

---

## Custom Nodes Required

| Package | Purpose |
|---|---|
| `@flowfuse/node-red-dashboard` | Dashboard 2.0 UI widgets |
| `node-red-contrib-opcua` | OPC UA client nodes (browser, read, write, events) |
| `@flowfuse/node-red-dashboard-2-ui-led` | LED status indicators |

Install via Node-RED palette manager or:
```bash
cd ~/.node-red
npm install @flowfuse/node-red-dashboard node-red-contrib-opcua @flowfuse/node-red-dashboard-2-ui-led
```

---

## Four Core Operations

### 1. Browse Address Space

Use the **OPC UA Browser** node to discover the server's folder hierarchy.

**Configuration:**
- Endpoint: `opc.tcp://<host>:<port>`
- Topic: leave blank to browse entire Objects tree
- Security: None (for anonymous access)

**Output:** Array of objects, each containing:
- `nodeId` — unique identifier for the node (used for read/write)
- `browseName` — human-readable name
- `children` — sub-nodes

**Example output structure:**
```
Objects/
├── Tank1/
│   ├── Level          (ns=2;s=Level)
│   ├── FlowRateIn     (ns=2;s=FlowRateIn)
│   └── Temperature    (ns=2;s=Temperature)
└── Fermentation/
    ├── TempSetpoint   (ns=2;s=Fermentation.TempSetpoint)
    └── TempActual     (ns=2;s=Fermentation.TempActual)
```

**Parsing pattern:**
```
OPC UA Browser → debug node → change node (extract nodeId + browseName)
                                    ├── store nodeId in flow context
                                    └── use browseName as dashboard label
```

### 2. Read Values

Use the **OPC UA Browser** node with a specific `nodeId` in `msg.topic`.

**Flow pattern:**
```
inject (1s interval, msg.topic = nodeId)
  → OPC UA Browser (endpoint configured)
    → switch (filter empty arrays)
      → change (extract value from payload array)
        → gauge / chart dashboard widget
```

**Key detail:** The browser node returns an array of child nodes. Filter out empty arrays to prevent values resetting to 0:
```
msg.payload.length > 0 → pass
msg.payload.length == 0 → block
```

**Reading specific values:**
```javascript
// msg.topic contains the nodeId, e.g. "ns=2;s=Level"
// The browser returns the value in msg.payload[0].value
```

### 3. Write Values

Use **OpcUa-Item** + **Opc-Ua-Client** nodes for two-way communication.

**Flow pattern:**
```
dashboard input (toggle switch / slider)
  → OpcUa-Item (nodeId + dataType)
    → Opc-Ua-Client (endpoint, action: WRITE)
```

**OpcUa-Item configuration:**
- NodeId: paste from server (e.g., `ns=2;s=FlowRateIn`)
- DataType: must match server definition (Double, Boolean, etc.)

**Opc-Ua-Client configuration:**
- Endpoint: `opc.tcp://<host>:<port>`
- Action: `WRITE`

**Dashboard widgets for writing:**
| Widget | Use Case |
|---|---|
| Toggle switch | Boolean values (on/off, start/stop) |
| Slider | Numeric ranges (0-100%, temperature setpoint) |
| Text input | Free-form values |

### 4. Read Alarms & Events

Use **OpcUa-Event** + **Opc-Ua-Client** nodes to subscribe to server events.

**Flow pattern:**
```
inject (trigger)
  → OpcUa-Event (nodeId of event source, event type: BaseEvent)
    → Opc-Ua-Client (endpoint, action: EVENTS)
      → change (extract timestamp, severity, message)
        → table / notification dashboard widget
```

**Event data structure:**
```json
{
  "timestamp": "2023-07-27T08:56:11Z",
  "severity": 500,
  "message": "Level Exceeded - High",
  "source": "ns=2;s=MyLevelAlarm"
}
```

**Alarm thresholds (example):**
| Threshold | Severity | Message |
|---|---|---|
| > 10% | Warning | Level Exceeded |
| > 30% | Minor | Level High |
| > 70% | Major | Level Critical |
| > 90% | Emergency | Level Overflow |

---

## Node-RED OPC UA Node Reference

| Node | Direction | Purpose |
|---|---|---|
| `OpcUa-Browser` | Read | Browse address space, read child nodes |
| `OpcUa-Item` | Config | Define nodeId + dataType for read/write |
| `Opc-Ua-Client` | Read/Write/Events | Connect to server, execute operations |
| `OpcUa-Event` | Subscribe | Filter and receive server events |

---

## Connecting to Our Simulation Servers

### Approach A — Node-RED Server (port 4840)

```
Opc-Ua-Client endpoint: opc.tcp://nodered:4840
```

Browse Fermentation variables:
```
msg.topic = "ns=2;s=Fermentation.TempSetpoint"
msg.topic = "ns=2;s=Fermentation.TempActual"
msg.topic = "ns=2;s=Dosing.FlowSetpoint"
```

Browse HVAC variables:
```
msg.topic = "ns=3;s=HVAC.AHU_TempSupply"
msg.topic = "ns=3;s=Energy.ActivePower"
```

### Approach B — Node.js Tank Server (port 4334)

```
Opc-Ua-Client endpoint: opc.tcp://tank-simulation:4334
```

Browse Tank1 variables:
```
msg.topic = "ns=2;s=Level"
msg.topic = "ns=2;s=FlowRateIn"
msg.topic = "ns=2;s=FlowRateOut"
msg.topic = "ns=2;s=Temperature"
```

Write to Tank1:
```
OpcUa-Item nodeId: "ns=2;s=FlowRateIn"  (Double)
OpcUa-Item nodeId: "ns=2;s=FlowRateOut" (Double)
```

Call SetInletFlow method:
```
OpcUa-Item nodeId: "ns=2;s=SetInletFlow" (Double input argument)
```

### Approach C — Java Milo Server (in-process)

```
Opc-Ua-Client endpoint: opc.tcp://localhost:12686
```

Same NodeIds as Approach B (both simulate Tank1).

---

## Java Client Equivalent

The same operations in Java using Apache Milo:

| Node-RED Operation | Java Milo Equivalent |
|---|---|
| OPC UA Browser (browse) | `client.browse()` → `List<Node>` |
| OPC UA Browser (read) | `client.readValue()` → `DataValue` |
| OpcUa-Item + Client (write) | `client.writeValue()` → `StatusCode` |
| OpcUa-Event + Client (events) | `client.subscriptionHandling()` → `Event` |

```java
// Browse
List<Node> nodes = client.browse(0, null, null).get();

// Read
DataValue value = client.readValue(0, TimestampsToReturn.Both, nodeId).get();

// Write
client.writeValue(nodeId, new DataValue(new Variant(5.0))).get();
```

---

## Security Notes

| Topic | Development | Production |
|---|---|---|
| Security mode | None (anonymous) | SignAndEncrypt |
| Certificates | Not required | Client + server certificates |
| User authentication | Anonymous | Username/password or X.509 |

For the MVP, all simulation servers use anonymous access. Production deployments must configure certificates and security policies.

---

## References

- [FlowFuse: How to Build an OPC UA Client Dashboard in Node-RED](https://flowfuse.com/blog/2023/07/how-to-build-a-opc-client-dashboard-in-node-red/)
- [FlowFuse: How to Deploy a Basic OPC-UA Server in Node-RED](https://flowfuse.com/blog/2023/07/how-to-deploy-a-basic-opc-ua-server-in-node-red/)
- [node-red-contrib-opcua](https://flows.nodered.org/node/node-red-contrib-opcua)
- [Prosys OPC UA Browser (Free)](https://www.prosysopc.com/products/opc-ua-browser/)
- [Eclipse Milo Documentation](https://eclipse.dev/milo/)
- [Our OPC UA Server Guide](./opcua-node-red-server-guide.md)
