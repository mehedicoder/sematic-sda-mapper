package com.sda.mapper.opcua;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tank Process State")
class TankProcessStateTest {

    private TankProcessState tankState;

    @BeforeEach
    void setUp() {
        tankState = new TankProcessState();
    }

    @Test
    @DisplayName("Should initialize with level at 20 cm")
    void testInitialLevel() {
        assertEquals(20.0, tankState.getLevelCm(), 0.001);
    }

    @Test
    @DisplayName("Should increase level when inlet flow exceeds outlet flow")
    void testLevelIncreasesWhenFlowInExceedsFlowOut() {
        double initialLevel = tankState.getLevelCm();
        tankState.stepSimulation(1);
        assertTrue(tankState.getLevelCm() > initialLevel);
    }

    @Test
    @DisplayName("Should clamp level at maximum when tank overflows")
    void testLevelClampsAtMaxLevel() {
        tankState.setLevelCm(99.9);
        tankState.setFlowInLMin(100);
        tankState.setFlowOutLMin(0);
        tankState.stepSimulation(100);
        assertTrue(tankState.getLevelCm() <= tankState.getMaxLevelCm());
    }

    @Test
    @DisplayName("Should clamp level at zero when tank drains completely")
    void testLevelClampsAtZero() {
        tankState.setLevelCm(0.1);
        tankState.setFlowInLMin(0);
        tankState.setFlowOutLMin(100);
        tankState.stepSimulation(100);
        assertTrue(tankState.getLevelCm() >= 0);
    }
}
