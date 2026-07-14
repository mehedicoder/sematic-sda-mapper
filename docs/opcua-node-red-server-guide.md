# OPC UA Server in Node-RED — Reference Guide

## What is OPC UA?

Open Platform Communications Unified Architecture (OPC UA) is an open, platform-independent communication framework for industrial automation. It is a key protocol standard for Industry 4.0 and Industrial IoT.

Unlike traditional fieldbus protocols (PROFINET, EtherCAT) that transmit raw register values, OPC UA represents data as **structured node objects** organized in a **folder hierarchy** within a server address space.

---

## OPC UA Information Model

The OPC UA information model represents automation data as nodes, not raw values. A device is modeled as an object with multiple attributes:

```
/Root/Objects/Tank1/Level
/Root/Objects/Tank1/FlowRateIn
/Root/Objects/Tank1/FlowRateOut
/Root/Objects/Tank1/Temperature
```

Each node has:
- **NodeId** — unique identifier (`ns=2;s=Level` or `ns=2;i=1001`)
- **DataType** — Double, Float, Boolean, String, etc.
- **Value** — current value (readable, sometimes writable)
- **BrowseName** — human-readable name for client browsing
- **References** — links to parent/child nodes (folder structure)

---

## Address Space Hierarchy

The address space is a tree structure rooted at `RootFolder`:

```
RootFolder
└── Objects
    └── Tank1
        ├── Level          (ns=2;s=Level, Double, read-only)
        ├── FlowRateIn     (ns=2;s=FlowRateIn, Double, read-write)
        ├── FlowRateOut    (ns=2;s=FlowRateOut, Double, read-write)
        └── Temperature    (ns=2;s=Temperature, Double, read-only)
```

Clients browse this tree to discover available data. The hierarchy makes it easy to navigate large device fleets.

---

## Node-RED OPC UA Server Setup

### Package

`node-red-contrib-opcua-server` — a compact OPC UA server node with fewer dependencies than the full `node-red-contrib-opcua` package.

Install in Dockerfile:
```dockerfile
RUN npm install node-red-contrib-opcua-server
```

### Compact Server Node Properties

| Tab | Key Settings |
|---|---|
| **Settings** | Port (default: 54845, we use 4840), Show Errors |
| **Limits** | maxNodesPerBrowse, maxNodesPerRead, etc. (0 = unlimited) |
| **Security** | Allow Anonymous (true for dev, false for production) |
| **Users & Sets** | Role-based access (empty for testing) |
| **Address Space** | JavaScript code defining the information model |
| **Discovery** | Endpoint URL format: `opc.tcp://<host>:<port>` |

### Address Space Configuration

The Address Space tab contains JavaScript that uses the `node-opcua` SDK to build the information model:

```javascript
// Get the server's namespace
const namespace = addressSpace.getOwnNamespace();

// Create a folder under Objects
const tank = namespace.addObject({
    organizedBy: addressSpace.rootFolder.objects,
    browseName: "Tank1"
});

// Add a variable to the folder
namespace.addVariable({
    componentOf: tank,
    browseName: "Level",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
        get: () => new Variant({
            dataType: DataType.Double,
            value: process_state.level_cm
        }),
        set: (variant) => {
            process_state.level_cm = variant.value;
            return StatusCodes.Good;
        }
    }
});
```

### Key API Calls

| Method | Purpose |
|---|---|
| `namespace.addObject({organizedBy, browseName})` | Create a folder/object in the address space |
| `namespace.addVariable({componentOf, browseName, dataType, value})` | Add a readable/writable variable |
| `namespace.addMethod(parent, {browseName, inputArguments, outputArguments})` | Add a callable method |
| `namespace.addView({organizedBy, browseName})` | Create a custom browsing hierarchy |

### Endpoint Discovery

Clients connect using the endpoint URL:
```
opc.tcp://<host>:<port>
```

Example: `opc.tcp://192.168.0.114:4840`

The endpoint must be configured in the Discovery tab of the Compact Server node.

---

## OPC UA Views

Views create custom hierarchies that clients can browse as an alternative to the default folder structure. Useful for grouping related variables across different folders.

```javascript
const view = namespace.addView({
    organizedBy: rootFolder.views,
    browseName: "Fermentation-Skid-A1"
});

view.addReference({
    referenceType: "Organizes",
    nodeId: tempSetpoint.nodeId
});
```

---

## Connecting with an OPC UA Client

### Prosys OPC UA Browser (Free)

1. Open Prosys OPC UA Browser
2. Enter endpoint URL: `opc.tcp://<host>:<port>`
3. Select security mode: `None` (for anonymous access)
4. Click "Connect to Server"
5. Browse the address space tree

### Java Client (Apache Milo)

```java
OpcUaClient client = OpcUaClient.create("opc.tcp://localhost:4840");
client.connect().get();

CompletableFuture<DataValue> future = client.readValue(
    0,                          // max age
    TimestampsToReturn.Both,
    new NodeId(2, "Level")      // namespace=2, identifier="Level"
);

DataValue value = future.get();
System.out.println("Level: " + value.getValue().getValue());

client.disconnect().get();
```

### NodeId Format

NodeIds differ between servers. Always verify the exact namespace index and identifier type:

| Format | Example | Meaning |
|---|---|---|
| String | `ns=2;s=Level` | Namespace 2, string identifier "Level" |
| Numeric | `ns=2;i=1001` | Namespace 2, numeric identifier 1001 |
| GUID | `ns=2;g=09087e75-8e5e-4d7d-9933-57e5f1a5dca0` | Namespace 2, GUID identifier |

---

## Security Modes

| Mode | Description | Use Case |
|---|---|---|
| None | No encryption, anonymous access | Development, testing |
| Sign | Messages signed but not encrypted | Basic integrity |
| SignAndEncrypt | Messages signed and encrypted | Production |

For the MVP, all simulation servers use `None` (anonymous). Production deployments must configure certificates and security policies.

---

## Our Project Setup

### Three OPC UA Simulation Approaches

| Approach | Runtime | Port | Endpoints | Variables |
|---|---|---|---|---|
| **A — Node-RED** | Docker container | 4840 | Fermentation (ns=2), HVAC (ns=3), test (ns=1) | Static inject values + periodic simulation |
| **B — Node.js Tank** | Docker container | 4334 | Tank1 (ns=2) | Physical process model (level dynamics) |
| **C — Java Milo** | In-process JVM | configurable | Tank1 (ns=2) | Physical process model (level dynamics) |

### Port Allocation

| Service | Port | Protocol |
|---|---|---|
| Spring Boot application | 8080 | HTTP |
| Node-RED UI | 1880 | HTTP |
| Node-RED OPC UA server | 4840 | OPC UA |
| Node.js Tank OPC UA server | 4334 | OPC UA |
| Java Milo OPC UA server | configured via `simulation.opcua.port` | OPC UA |

### Unified Java Client

A single Apache Milo client reads from all three servers:

```java
// Read from Node-RED (Approach A)
readNodeValue("opc.tcp://localhost:4840", new NodeId(2, "Fermentation.TempSetpoint"));

// Read from Node.js tank (Approach B)
readNodeValue("opc.tcp://localhost:4334", new NodeId(2, "Level"));

// Read from Java Milo tank (Approach C)
readNodeValue("opc.tcp://localhost:12686", new NodeId(2, "Level"));
```

OPC UA is a standardized protocol — the same Java client works regardless of the server's implementation language.

---

## References

- [FlowFuse: How to Deploy a Basic OPC-UA Server in Node-RED](https://flowfuse.com/blog/2023/07/how-to-deploy-a-basic-opc-ua-server-in-node-red/)
- [node-red-contrib-opcua-server](https://flows.nodered.org/node/node-red-contrib-opcua-server)
- [node-opcua SDK](https://node-opcua.github.io/)
- [Eclipse Milo](https://eclipse.dev/milo/)
- [OPC UA Foundation](https://opcfoundation.org/)
