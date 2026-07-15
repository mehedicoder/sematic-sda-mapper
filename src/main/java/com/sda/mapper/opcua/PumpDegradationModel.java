package com.sda.mapper.opcua;

import org.springframework.stereotype.Component;

/**
 * Pump degradation model for OPC UA simulation.
 */
@Component
public class PumpDegradationModel {

    private double healthPercent = 100.0;
    private double wearRate = 0.001;
    private double vibration = 0.5;
    private double temperature = 25.0;
    private double setpointLMin = 5.0;
    private String phase = "Healthy";

    /**
     * Steps the degradation simulation forward by dtSeconds.
     */
    public void stepSimulation(double dtSeconds) {
        if (healthPercent <= 0) {
            phase = "Failure";
            return;
        }

        // Accelerating degradation: wear rate increases as health decreases
        double degradationFactor = 1 + (100 - healthPercent) / 50;
        healthPercent -= wearRate * degradationFactor * dtSeconds;

        // Effective flow capacity decreases with health
        double effectiveFlow = setpointLMin * (healthPercent / 100);

        // Vibration increases with degradation
        vibration = 0.5 + (100 - healthPercent) * 0.1;

        // Temperature offset from bearing heat
        temperature = 25 + (100 - healthPercent) * 0.15;

        // Determine phase
        if (healthPercent > 80) {
            phase = "Healthy";
        } else if (healthPercent > 60) {
            phase = "EarlyWear";
        } else if (healthPercent > 30) {
            phase = "SealLeak";
        } else if (healthPercent > 0) {
            phase = "ImpellerErosion";
        } else {
            phase = "Failure";
        }
    }

    /**
     * Gets the effective flow rate based on health.
     */
    public double getEffectiveFlowLMin() {
        return setpointLMin * (healthPercent / 100);
    }

    // Getters and setters
    public double getHealthPercent() { return healthPercent; }
    public void setHealthPercent(double healthPercent) { this.healthPercent = Math.max(0, Math.min(100, healthPercent)); }

    public double getWearRate() { return wearRate; }
    public void setWearRate(double wearRate) { this.wearRate = wearRate; }

    public double getVibration() { return vibration; }
    public void setVibration(double vibration) { this.vibration = vibration; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getSetpointLMin() { return setpointLMin; }
    public void setSetpointLMin(double setpointLMin) { this.setpointLMin = setpointLMin; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
}
