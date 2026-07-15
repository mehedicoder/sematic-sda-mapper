package com.sda.mapper.opcua;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pump Degradation Model")
class PumpDegradationModelTest {

    private PumpDegradationModel pumpModel;

    @BeforeEach
    void setUp() {
        pumpModel = new PumpDegradationModel();
    }

    @Test
    @DisplayName("Should initialize with 100% health")
    void testInitialHealth() {
        assertEquals(100.0, pumpModel.getHealthPercent(), 0.001);
    }

    @Test
    @DisplayName("Should initialize in Healthy phase")
    void testInitialPhase() {
        assertEquals("Healthy", pumpModel.getPhase());
    }

    @Test
    @DisplayName("Should decrease health over time")
    void testDegradationOverTime() {
        double initialHealth = pumpModel.getHealthPercent();
        pumpModel.stepSimulation(100);
        assertTrue(pumpModel.getHealthPercent() < initialHealth);
    }

    @Test
    @DisplayName("Should transition through all phases to Failure")
    void testPhaseTransitions() {
        while (pumpModel.getHealthPercent() > 0) {
            pumpModel.stepSimulation(10);
        }
        assertEquals("Failure", pumpModel.getPhase());
    }

    @Test
    @DisplayName("Should compute effective flow proportional to health percentage")
    void testEffectiveFlowDecreasesWithHealth() {
        pumpModel.setHealthPercent(50);
        double effectiveFlow = pumpModel.getEffectiveFlowLMin();
        assertEquals(2.5, effectiveFlow, 0.001);
    }

    @Test
    @DisplayName("Should increase vibration as degradation progresses")
    void testVibrationIncreasesWithDegradation() {
        double initialVibration = pumpModel.getVibration();
        pumpModel.stepSimulation(100);
        assertTrue(pumpModel.getVibration() > initialVibration);
    }
}
