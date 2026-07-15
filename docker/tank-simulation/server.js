/**
 * OPC UA Tank Simulation Server with NAMUR NE 107 Pump Degradation
 * -----------------------------------------------------------------
 * Simulates a tank with inlet/outlet flow and a degrading pump.
 * Exposes OPC UA variables with NAMUR NE 107 diagnostics.
 * 
 * Variables:
 * - Tank1.Level (cm)
 * - Tank1.FlowRateIn (L/min)
 * - Tank1.FlowRateOut (L/min)
 * - Tank1.Temperature (°C)
 * - Pump1.FlowRate (L/min) - with degradation
 * - Pump1.Vibration (mm/s)
 * - Pump1.Temperature (°C)
 * - Pump1.Status (NE 107 status code)
 * - Pump1.FlowRate_Diag (DiagnosticString)
 * - Pump1.Vibration_Diag (DiagnosticString)
 * - Pump1.Temperature_Diag (DiagnosticString)
 * - Pump1.DeviceStatus (NE 107 aggregate)
 */

const { OPCUAServer, OPCUAServerOptions, Variant, DataType, StatusCodes, 
        standardUnits, SecurityPolicy, MessageSecurityMode } = require("node-opcua");
const express = require("express");
const https = require("https");
const fs = require("fs");
const { WebSocketServer } = require("ws");

// ---- Tank Physics Model ----
const tankState = {
  level_cm: 20.0,
  flowIn_Lmin: 5.0,
  flowOut_Lmin: 4.0,
  temperature_C: 22.0,
  tankAreaCm2: 500,
  maxLevel_cm: 100,
};

function stepTankPhysics(dtSeconds) {
  const netFlow_Lmin = tankState.flowIn_Lmin - tankState.flowOut_Lmin;
  const netFlow_cm3s = (netFlow_Lmin * 1000) / 60;
  const dLevel_cm = (netFlow_cm3s / tankState.tankAreaCm2) * dtSeconds;

  tankState.level_cm = Math.min(
    tankState.maxLevel_cm,
    Math.max(0, tankState.level_cm + dLevel_cm)
  );

  tankState.temperature_C += (Math.random() - 0.5) * 0.05;
}

// ---- Pump Degradation Model ----
const pumpState = {
  healthPercent: 100.0,
  wearRate: 0.001,
  vibration: 0.5,
  temperature: 25.0,
  setpoint_Lmin: 5.0,
  phase: "Healthy", // Healthy, EarlyWear, SealLeak, ImpellerErosion, Failure
};

function stepPumpDegradation(dtSeconds) {
  if (pumpState.healthPercent <= 0) {
    pumpState.phase = "Failure";
    return;
  }

  // Accelerating degradation: wear rate increases as health decreases
  const degradationFactor = 1 + (100 - pumpState.healthPercent) / 50;
  pumpState.healthPercent -= pumpState.wearRate * degradationFactor * dtSeconds;

  // Effective flow capacity decreases with health
  const effectiveFlow = pumpState.setpoint_Lmin * (pumpState.healthPercent / 100);

  // Vibration increases with degradation
  pumpState.vibration = 0.5 + (100 - pumpState.healthPercent) * 0.1;

  // Temperature offset from bearing heat
  pumpState.temperature = 25 + (100 - pumpState.healthPercent) * 0.15;

  // Determine phase
  if (pumpState.healthPercent > 80) {
    pumpState.phase = "Healthy";
  } else if (pumpState.healthPercent > 60) {
    pumpState.phase = "EarlyWear";
  } else if (pumpState.healthPercent > 30) {
    pumpState.phase = "SealLeak";
  } else if (pumpState.healthPercent > 0) {
    pumpState.phase = "ImpellerErosion";
  } else {
    pumpState.phase = "Failure";
  }

  // Update tank with effective flow
  tankState.flowIn_Lmin = effectiveFlow;
}

// ---- NAMUR NE 107 Diagnostic Simulator ----
const Ne107Status = {
  Good: 0,
  Maintenance: 1,
  FunctionCheck: 2,
  Failure: 3,
};

const Ne107SubCode = {
  None: "",
  Sensor: "S",
  Function: "F",
  WireBreak: "W",
  ShortCircuit: "K",
};

function getNe107Status(variable) {
  const status = { code: Ne107Status.Good, subCode: Ne107SubCode.None, diagString: "" };

  switch (variable) {
    case "FlowRate":
      if (pumpState.phase === "Failure") {
        status.code = Ne107Status.Failure;
        status.subCode = Ne107SubCode.Function;
        status.diagString = "Pump failed - no flow";
      } else if (pumpState.phase === "ImpellerErosion") {
        status.code = Ne107Status.Failure;
        status.subCode = Ne107SubCode.Function;
        status.diagString = "Impeller erosion - flow reduced";
      } else if (pumpState.phase === "SealLeak") {
        status.code = Ne107Status.Maintenance;
        status.subCode = Ne107SubCode.Function;
        status.diagString = "Seal leak detected";
      } else if (pumpState.phase === "EarlyWear") {
        status.code = Ne107Status.Maintenance;
        status.subCode = Ne107SubCode.None;
        status.diagString = "Early wear detected";
      }
      break;

    case "Vibration":
      if (pumpState.vibration > 8) {
        status.code = Ne107Status.Failure;
        status.subCode = Ne107SubCode.Sensor;
        status.diagString = "Excessive vibration";
      } else if (pumpState.vibration > 5) {
        status.code = Ne107Status.Maintenance;
        status.subCode = Ne107SubCode.None;
        status.diagString = "High vibration";
      }
      break;

    case "Temperature":
      if (pumpState.temperature > 40) {
        status.code = Ne107Status.Failure;
        status.subCode = Ne107SubCode.Sensor;
        status.diagString = "Overheating";
      } else if (pumpState.temperature > 35) {
        status.code = Ne107Status.Maintenance;
        status.subCode = Ne107SubCode.None;
        status.diagString = "Temperature elevated";
      }
      break;

    case "DeviceStatus":
      if (pumpState.phase === "Failure") {
        status.code = Ne107Status.Failure;
        status.subCode = Ne107SubCode.Function;
        status.diagString = "Device failure";
      } else if (pumpState.phase === "ImpellerErosion") {
        status.code = Ne107Status.Failure;
        status.subCode = Ne107SubCode.Function;
        status.diagString = "Impeller failure";
      } else if (pumpState.phase === "SealLeak") {
        status.code = Ne107Status.Maintenance;
        status.subCode = Ne107SubCode.Function;
        status.diagString = "Seal maintenance required";
      } else if (pumpState.phase === "EarlyWear") {
        status.code = Ne107Status.Maintenance;
        status.subCode = Ne107SubCode.None;
        status.diagString = "Scheduled maintenance";
      }
      break;
  }

  return status;
}

// ---- OPC UA Server ----
const serverOptions = {
  port: 4334,
  resourcePath: "/UA/FlowMeterServer",
  buildInfo: {
    productName: "SDA-Tank-Simulation-NE107",
    buildNumber: "1",
    buildDate: new Date(),
  },
  securityPolicies: [SecurityPolicy.Basic256Sha256],
  securityModes: [MessageSecurityMode.SignAndEncrypt],
  allowAnonymous: false,
  certificateFile: null, // Will be set if SSL enabled
  privateKeyFile: null,
};

let server;
let addressSpace;
let namespace;

async function initOpcUaServer() {
  server = new OPCUAServer(serverOptions);
  await server.initialize();

  addressSpace = server.engine.addressSpace;
  namespace = addressSpace.getOwnNamespace();

  // Tank1 object
  const tank = namespace.addObject({
    organizedBy: addressSpace.rootFolder.objects,
    browseName: "Tank1",
  });

  // Tank variables
  namespace.addVariable({
    componentOf: tank,
    browseName: "Level",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: tankState.level_cm }),
    },
  });

  namespace.addVariable({
    componentOf: tank,
    browseName: "FlowRateIn",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: tankState.flowIn_Lmin }),
      set: (variant) => {
        tankState.flowIn_Lmin = variant.value;
        return StatusCodes.Good;
      },
    },
  });

  namespace.addVariable({
    componentOf: tank,
    browseName: "FlowRateOut",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: tankState.flowOut_Lmin }),
      set: (variant) => {
        tankState.flowOut_Lmin = variant.value;
        return StatusCodes.Good;
      },
    },
  });

  namespace.addVariable({
    componentOf: tank,
    browseName: "Temperature",
    dataType: "Double",
    minimumSamplingInterval: 500,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: tankState.temperature_C }),
    },
  });

  // SetInletFlow method
  namespace.addMethod(tank, {
    browseName: "SetInletFlow",
    inputArguments: [
      {
        name: "value",
        description: { text: "New inlet flow rate in L/min" },
        dataType: DataType.Double,
      },
    ],
    outputArguments: [
      {
        name: "result",
        description: { text: "Confirmation message" },
        dataType: DataType.String,
      },
    ],
  }).bindMethod((inputArguments, context, callback) => {
    const newFlow = inputArguments[0].value;
    tankState.flowIn_Lmin = newFlow;
    callback(null, {
      statusCode: StatusCodes.Good,
      outputArguments: [
        new Variant({ dataType: DataType.String, value: `Inlet flow set to ${newFlow} L/min` }),
      ],
    });
  });

  // Pump1 object
  const pump = namespace.addObject({
    organizedBy: addressSpace.rootFolder.objects,
    browseName: "Pump1",
  });

  // Pump variables
  namespace.addVariable({
    componentOf: pump,
    browseName: "FlowRate",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: tankState.flowIn_Lmin }),
    },
  });

  namespace.addVariable({
    componentOf: pump,
    browseName: "Vibration",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: pumpState.vibration }),
    },
  });

  namespace.addVariable({
    componentOf: pump,
    browseName: "Temperature",
    dataType: "Double",
    minimumSamplingInterval: 500,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: pumpState.temperature }),
    },
  });

  namespace.addVariable({
    componentOf: pump,
    browseName: "Status",
    dataType: "UInt32",
    minimumSamplingInterval: 500,
    value: {
      get: () => {
        const status = getNe107Status("DeviceStatus");
        return new Variant({ dataType: DataType.UInt32, value: status.code });
      },
    },
  });

  // NE 107 diagnostic variables
  const diagnosticVars = ["FlowRate", "Vibration", "Temperature"];
  diagnosticVars.forEach((varName) => {
    namespace.addVariable({
      componentOf: pump,
      browseName: `${varName}_Status`,
      dataType: "UInt32",
      minimumSamplingInterval: 500,
      value: {
        get: () => {
          const status = getNe107Status(varName);
          return new Variant({ dataType: DataType.UInt32, value: status.code });
        },
      },
    });

    namespace.addVariable({
      componentOf: pump,
      browseName: `${varName}_Diag`,
      dataType: "String",
      minimumSamplingInterval: 500,
      value: {
        get: () => {
          const status = getNe107Status(varName);
          return new Variant({ dataType: DataType.String, value: status.diagString });
        },
      },
    });
  });

  // SetPumpDegradation method for manual fault injection
  namespace.addMethod(pump, {
    browseName: "SetDegradation",
    inputArguments: [
      {
        name: "healthPercent",
        description: { text: "Health percentage (0-100)" },
        dataType: DataType.Double,
      },
    ],
    outputArguments: [
      {
        name: "result",
        description: { text: "Confirmation message" },
        dataType: DataType.String,
      },
    ],
  }).bindMethod((inputArguments, context, callback) => {
    const health = inputArguments[0].value;
    pumpState.healthPercent = Math.max(0, Math.min(100, health));
    callback(null, {
      statusCode: StatusCodes.Good,
      outputArguments: [
        new Variant({ dataType: DataType.String, value: `Pump health set to ${pumpState.healthPercent}%` }),
      ],
    });
  });

  console.log("OPC UA Server initialized with Tank1 and Pump1");
}

// ---- Express API ----
const app = express();
app.use(express.json());

app.get("/api/tank/health", (req, res) => {
  res.json({
    status: "running",
    pumpHealth: pumpState.healthPercent,
    pumpPhase: pumpState.phase,
    tankLevel: tankState.level_cm,
  });
});

app.get("/api/tank/physics", (req, res) => {
  res.json({
    tank: { ...tankState },
    pump: { ...pumpState },
  });
});

// ---- WebSocket Bridge ----
const wss = new WebSocketServer({ port: 8081 });

wss.on("connection", (ws) => {
  console.log("WebSocket client connected");
  
  const sendUpdate = () => {
    ws.send(JSON.stringify({
      tank: { ...tankState },
      pump: { ...pumpState },
      diagnostics: {
        FlowRate: getNe107Status("FlowRate"),
        Vibration: getNe107Status("Vibration"),
        Temperature: getNe107Status("Temperature"),
        DeviceStatus: getNe107Status("DeviceStatus"),
      },
    }));
  };

  const interval = setInterval(sendUpdate, 500);

  ws.on("close", () => {
    clearInterval(interval);
    console.log("WebSocket client disconnected");
  });
});

// ---- Main ----
async function main() {
  try {
    await initOpcUaServer();
    await server.start();
    console.log("OPC UA Server listening at:", server.getEndpointUrl());

    // Physics simulation tick
    setInterval(() => {
      stepTankPhysics(1);
      stepPumpDegradation(1);
    }, 1000);

    // Start Express server
    app.listen(3000, () => {
      console.log("Express API listening on port 3000");
    });

    console.log("Tank Simulation Server started successfully");
    console.log("  - OPC UA: port 4334");
    console.log("  - Express API: port 3000");
    console.log("  - WebSocket: port 8081");
  } catch (err) {
    console.error("Failed to start server:", err);
    process.exit(1);
  }
}

main();
