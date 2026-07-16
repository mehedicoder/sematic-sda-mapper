package com.sda.mapper.opcua;

public record OpcUaSnapshot(
    Double tankLevel,
    Double tankFlowRateIn,
    Double tankFlowRateOut,
    Double tankTemperature,
    Double pumpFlowRate,
    Double pumpVibration,
    Double pumpTemperature,
    String pumpDeviceStatus
) {
}
