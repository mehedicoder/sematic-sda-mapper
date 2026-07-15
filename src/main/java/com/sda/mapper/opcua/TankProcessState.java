package com.sda.mapper.opcua;

import org.springframework.stereotype.Component;

/**
 * Tank physics model for OPC UA simulation.
 */
@Component
public class TankProcessState {

    private double levelCm = 20.0;
    private double flowInLMin = 5.0;
    private double flowOutLMin = 4.0;
    private double temperatureC = 22.0;
    private double tankAreaCm2 = 500;
    private double maxLevelCm = 100;

    /**
     * Steps the simulation forward by dtSeconds.
     */
    public void stepSimulation(double dtSeconds) {
        double netFlowLMin = flowInLMin - flowOutLMin;
        double netFlowCm3s = (netFlowLMin * 1000) / 60;
        double dLevelCm = (netFlowCm3s / tankAreaCm2) * dtSeconds;

        levelCm = Math.min(maxLevelCm, Math.max(0, levelCm + dLevelCm));

        // Small thermal drift
        temperatureC += (Math.random() - 0.5) * 0.05;
    }

    // Getters and setters
    public double getLevelCm() { return levelCm; }
    public void setLevelCm(double levelCm) { this.levelCm = levelCm; }

    public double getFlowInLMin() { return flowInLMin; }
    public void setFlowInLMin(double flowInLMin) { this.flowInLMin = flowInLMin; }

    public double getFlowOutLMin() { return flowOutLMin; }
    public void setFlowOutLMin(double flowOutLMin) { this.flowOutLMin = flowOutLMin; }

    public double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(double temperatureC) { this.temperatureC = temperatureC; }

    public double getTankAreaCm2() { return tankAreaCm2; }
    public void setTankAreaCm2(double tankAreaCm2) { this.tankAreaCm2 = tankAreaCm2; }

    public double getMaxLevelCm() { return maxLevelCm; }
    public void setMaxLevelCm(double maxLevelCm) { this.maxLevelCm = maxLevelCm; }
}
