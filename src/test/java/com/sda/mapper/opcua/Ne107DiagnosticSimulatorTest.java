package com.sda.mapper.opcua;

import com.sda.mapper.opcua.Ne107DiagnosticSimulator.DiagnosticResult;
import com.sda.mapper.opcua.Ne107DiagnosticSimulator.Ne107Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NAMUR NE 107 Diagnostic Simulator")
class Ne107DiagnosticSimulatorTest {

    private Ne107DiagnosticSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new Ne107DiagnosticSimulator();
    }

    @Test
    @DisplayName("Should return Good status for healthy pump FlowRate")
    void testHealthyPumpReturnsGoodStatus() {
        DiagnosticResult result = simulator.simulatePumpDiagnostics("FlowRate", 100, 0.5, 25);
        assertEquals(Ne107Status.Good, result.status());
    }

    @Test
    @DisplayName("Should return Failure status for failed pump FlowRate")
    void testFailedPumpReturnsFailureStatus() {
        DiagnosticResult result = simulator.simulatePumpDiagnostics("FlowRate", 0, 0.5, 25);
        assertEquals(Ne107Status.Failure, result.status());
    }

    @Test
    @DisplayName("Should return Maintenance status for degraded pump FlowRate")
    void testDegradedPumpReturnsMaintenanceStatus() {
        DiagnosticResult result = simulator.simulatePumpDiagnostics("FlowRate", 70, 0.5, 25);
        assertEquals(Ne107Status.Maintenance, result.status());
    }

    @Test
    @DisplayName("Should return Failure status for excessive Vibration")
    void testHighVibrationReturnsFailureStatus() {
        DiagnosticResult result = simulator.simulatePumpDiagnostics("Vibration", 100, 10, 25);
        assertEquals(Ne107Status.Failure, result.status());
    }

    @Test
    @DisplayName("Should return Failure status for overheating Temperature")
    void testHighTemperatureReturnsFailureStatus() {
        DiagnosticResult result = simulator.simulatePumpDiagnostics("Temperature", 100, 0.5, 45);
        assertEquals(Ne107Status.Failure, result.status());
    }

    @Test
    @DisplayName("Should map DeviceStatus to Maintenance for partially degraded pump")
    void testDeviceStatusMapsToOverallHealth() {
        DiagnosticResult result = simulator.simulatePumpDiagnostics("DeviceStatus", 50, 0.5, 25);
        assertEquals(Ne107Status.Maintenance, result.status());
    }
}
