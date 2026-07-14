/**
 * OPC UA Flow Meter / Tank Prototype Server
 * ------------------------------------------
 * Simulates a tank with an inlet flow meter and outlet valve.
 * Exposes Level, FlowRateIn, FlowRateOut, Temperature as OPC UA variables,
 * and a SetInletFlow method to actuate the simulated process from a client
 * (e.g. Node-RED, or later an SDA CSS-SDA service).
 */

const {
  OPCUAServer,
  Variant,
  DataType,
  StatusCodes,
  standardUnits,
} = require("node-opcua");

// ---- Simulated process state -------------------------------------------
const process_state = {
  level_cm: 20.0,          // current tank level
  flowIn_Lmin: 5.0,        // controllable inlet flow
  flowOut_Lmin: 4.0,       // fixed/manual outlet draw
  temperature_C: 22.0,
  tankAreaCm2: 500,        // cross-sectional area -> converts flow to level change
  maxLevel_cm: 100,
};

function stepSimulation(dtSeconds) {
  // Level change = (FlowIn - FlowOut) / Area, unit-adjusted (L/min -> cm/s)
  const netFlow_Lmin = process_state.flowIn_Lmin - process_state.flowOut_Lmin;
  const netFlow_cm3s = (netFlow_Lmin * 1000) / 60; // L/min -> cm3/s
  const dLevel_cm = (netFlow_cm3s / process_state.tankAreaCm2) * dtSeconds;

  process_state.level_cm = Math.min(
    process_state.maxLevel_cm,
    Math.max(0, process_state.level_cm + dLevel_cm)
  );

  // small thermal drift just to make the temperature tag look "alive"
  process_state.temperature_C += (Math.random() - 0.5) * 0.05;
}

// ---- Server setup --------------------------------------------------------
(async () => {
  const server = new OPCUAServer({
    port: 4334,
    resourcePath: "/UA/FlowMeterServer",
    buildInfo: {
      productName: "SDA-FlowMeter-Prototype",
      buildNumber: "1",
      buildDate: new Date(),
    },
  });

  await server.initialize();
  const addressSpace = server.engine.addressSpace;
  const namespace = addressSpace.getOwnNamespace();

  const tank = namespace.addObject({
    organizedBy: addressSpace.rootFolder.objects,
    browseName: "Tank1",
  });

  const levelVar = namespace.addVariable({
    componentOf: tank,
    browseName: "Level",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: process_state.level_cm }),
    },
  });

  namespace.addVariable({
    componentOf: tank,
    browseName: "FlowRateIn",
    dataType: "Double",
    minimumSamplingInterval: 200,
    value: {
      get: () => new Variant({ dataType: DataType.Double, value: process_state.flowIn_Lmin }),
      set: (variant) => {
        process_state.flowIn_Lmin = variant.value;
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
      get: () => new Variant({ dataType: DataType.Double, value: process_state.flowOut_Lmin }),
      set: (variant) => {
        process_state.flowOut_Lmin = variant.value;
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
      get: () => new Variant({ dataType: DataType.Double, value: process_state.temperature_C }),
    },
  });

  // A method a client can call to set inlet flow (alternative to writing the variable)
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
    process_state.flowIn_Lmin = newFlow;
    callback(null, {
      statusCode: StatusCodes.Good,
      outputArguments: [
        new Variant({ dataType: DataType.String, value: `Inlet flow set to ${newFlow} L/min` }),
      ],
    });
  });

  // Advance the simulated physics every second
  setInterval(() => stepSimulation(1), 1000);

  await server.start();
  console.log("OPC UA Flow Meter server listening at:");
  console.log(server.getEndpointUrl());
})();
