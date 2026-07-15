package com.sda.mapper.opcua;

import com.sda.mapper.opcua.Ne107DiagnosticSimulator.DiagnosticResult;
import com.sda.mapper.opcua.Ne107DiagnosticSimulator.FaultType;
import com.sda.mapper.opcua.Ne107DiagnosticSimulator.Ne107Status;
import com.sda.mapper.opcua.Ne107DiagnosticSimulator.Ne107SubCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NAMUR NE 107 Diagnostic Simulator Extended")
class Ne107DiagnosticSimulatorExtendedTest {

    private Ne107DiagnosticSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new Ne107DiagnosticSimulator();
    }

    @Test
    @DisplayName("Should inject SensorFail fault and override model-based diagnostics")
    void testInjectSensorFailFault() {
        simulator.injectFault("FlowRate", FaultType.SensorFail);
        DiagnosticResult result = simulator.simulatePumpDiagnostics("FlowRate", 100, 0.5, 25);
        assertEquals(Ne107Status.Failure, result.status());
        assertEquals(Ne107SubCode.Sensor, result.subCode());
        assertEquals("Sensor failure", result.diagnosticString());
    }

    @Test
    @DisplayName("Should inject WireBreak fault with correct sub-code")
    void testInjectWireBreakFault() {
        simulator.injectFault("Vibration", FaultType.WireBreak);
        DiagnosticResult result = simulator.simulatePumpDiagnostics("Vibration", 100, 0.5, 25);
        assertEquals(Ne107Status.Failure, result.status());
        assertEquals(Ne107SubCode.WireBreak, result.subCode());
    }

    @Test
    @DisplayName("Should inject ShortCircuit fault with correct sub-code")
    void testInjectShortCircuitFault() {
        simulator.injectFault("Temperature", FaultType.ShortCircuit);
        DiagnosticResult result = simulator.simulatePumpDiagnostics("Temperature", 100, 0.5, 25);
        assertEquals(Ne107Status.Failure, result.status());
        assertEquals(Ne107SubCode.ShortCircuit, result.subCode());
    }

    @Test
    @DisplayName("Should inject FunctionFail fault with correct sub-code")
    void testInjectFunctionFailFault() {
        simulator.injectFault("DeviceStatus", FaultType.FunctionFail);
        DiagnosticResult result = simulator.simulatePumpDiagnostics("DeviceStatus", 100, 0.5, 25);
        assertEquals(Ne107Status.Failure, result.status());
        assertEquals(Ne107SubCode.Function, result.subCode());
    }

    @Test
    @DisplayName("Should inject MaintenanceRequired fault with None sub-code")
    void testInjectMaintenanceRequiredFault() {
        simulator.injectFault("FlowRate", FaultType.MaintenanceRequired);
        DiagnosticResult result = simulator.simulatePumpDiagnostics("FlowRate", 100, 0.5, 25);
        assertEquals(Ne107Status.Maintenance, result.status());
        assertEquals(Ne107SubCode.None, result.subCode());
    }

    @Test
    @DisplayName("Should clear fault and restore model-based diagnostics")
    void testClearFaultRestoresNormalBehavior() {
        simulator.injectFault("FlowRate", FaultType.SensorFail);
        assertTrue(simulator.hasFault("FlowRate"));

        simulator.clearFault("FlowRate");
        assertFalse(simulator.hasFault("FlowRate"));

        DiagnosticResult result = simulator.simulatePumpDiagnostics("FlowRate", 100, 0.5, 25);
        assertEquals(Ne107Status.Good, result.status());
    }

    @Test
    @DisplayName("Should clear all faults at once")
    void testClearAllFaults() {
        simulator.injectFault("FlowRate", FaultType.SensorFail);
        simulator.injectFault("Vibration", FaultType.WireBreak);
        assertTrue(simulator.hasFault("FlowRate"));
        assertTrue(simulator.hasFault("Vibration"));

        simulator.clearAllFaults();
        assertFalse(simulator.hasFault("FlowRate"));
        assertFalse(simulator.hasFault("Vibration"));
    }

    @Test
    @DisplayName("Should report hasFault correctly for clean variable")
    void testHasFaultReturnsFalseForCleanVariable() {
        assertFalse(simulator.hasFault("FlowRate"));
        assertFalse(simulator.hasFault("Vibration"));
    }

    @Test
    @DisplayName("Should report hasFault returns true after injection")
    void testHasFaultReturnsTrueAfterInjection() {
        simulator.injectFault("FlowRate", FaultType.SensorFail);
        assertTrue(simulator.hasFault("FlowRate"));
    }
}
