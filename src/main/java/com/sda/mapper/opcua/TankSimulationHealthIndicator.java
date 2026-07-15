package com.sda.mapper.opcua;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the in-process OPC UA tank simulation server.
 */
@Component
public class TankSimulationHealthIndicator implements HealthIndicator {

    private final TankOpcUaServer tankServer;

    public TankSimulationHealthIndicator(TankOpcUaServer tankServer) {
        this.tankServer = tankServer;
    }

    @Override
    public Health health() {
        if (!tankServer.isEnabled()) {
            return Health.unknown()
                .withDetail("tank-simulation", "disabled")
                .withDetail("hint", "Enable with simulation.opcua.enabled=true")
                .build();
        }

        if (tankServer.isRunning()) {
            return Health.up()
                .withDetail("tank-simulation", "running")
                .withDetail("port", tankServer.getPort())
                .build();
        } else {
            return Health.down()
                .withDetail("tank-simulation", "failed")
                .build();
        }
    }
}
