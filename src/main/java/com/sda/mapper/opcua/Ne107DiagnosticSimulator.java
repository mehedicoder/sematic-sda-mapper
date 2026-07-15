package com.sda.mapper.opcua;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * NAMUR NE 107 diagnostic simulator for OPC UA variables.
 * Supports fault injection and auto-recovery with configurable duration.
 */
@Component
public class Ne107DiagnosticSimulator {

    /**
     * NE 107 status codes.
     */
    public enum Ne107Status {
        Good(0),
        Maintenance(1),
        FunctionCheck(2),
        Failure(3);

        private final int code;

        Ne107Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * NE 107 sub-codes.
     */
    public enum Ne107SubCode {
        None(""),
        Sensor("S"),
        Function("F"),
        WireBreak("W"),
        ShortCircuit("K");

        private final String code;

        Ne107SubCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Fault types that can be injected into variables.
     */
    public enum FaultType {
        SensorFail(Ne107Status.Failure, Ne107SubCode.Sensor, "Sensor failure"),
        WireBreak(Ne107Status.Failure, Ne107SubCode.WireBreak, "Wire break"),
        ShortCircuit(Ne107Status.Failure, Ne107SubCode.ShortCircuit, "Short circuit"),
        FunctionFail(Ne107Status.Failure, Ne107SubCode.Function, "Function failure"),
        MaintenanceRequired(Ne107Status.Maintenance, Ne107SubCode.None, "Maintenance required");

        private final Ne107Status status;
        private final Ne107SubCode subCode;
        private final String description;

        FaultType(Ne107Status status, Ne107SubCode subCode, String description) {
            this.status = status;
            this.subCode = subCode;
            this.description = description;
        }

        public Ne107Status getStatus() { return status; }
        public Ne107SubCode getSubCode() { return subCode; }
        public String getDescription() { return description; }
    }

    /**
     * Diagnostic result for a variable.
     */
    public record DiagnosticResult(Ne107Status status, Ne107SubCode subCode, String diagnosticString) {
        public int getStatusCode() {
            return status.getCode();
        }

        public String getSubCode() {
            return subCode.getCode();
        }
    }

    private final Map<String, FaultRecord> activeFaults = new ConcurrentHashMap<>();
    private final ScheduledExecutorService recoveryScheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Records an active fault with expiry time.
     */
    private record FaultRecord(FaultType faultType, long expiresAtMillis) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    /**
     * Injects a fault into a specific variable. The fault clears automatically
     * after the specified duration, or stays active indefinitely if durationMs is 0.
     *
     * @param variable the OPC UA variable name (e.g. "FlowRate", "Vibration")
     * @param faultType the type of fault to inject
     * @param durationMs duration in milliseconds before auto-recovery (0 = no auto-recovery)
     */
    public void injectFault(String variable, FaultType faultType, long durationMs) {
        long expiresAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : Long.MAX_VALUE;
        activeFaults.put(variable, new FaultRecord(faultType, expiresAt));
        if (durationMs > 0) {
            recoveryScheduler.schedule(
                () -> activeFaults.remove(variable),
                durationMs, TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Injects a fault into a specific variable that stays active until manually cleared.
     *
     * @param variable the OPC UA variable name
     * @param faultType the type of fault to inject
     */
    public void injectFault(String variable, FaultType faultType) {
        injectFault(variable, faultType, 0);
    }

    /**
     * Clears a fault from a specific variable, restoring its normal diagnostic behavior.
     *
     * @param variable the OPC UA variable name
     */
    public void clearFault(String variable) {
        activeFaults.remove(variable);
    }

    /**
     * Clears all active faults.
     */
    public void clearAllFaults() {
        activeFaults.clear();
    }

    /**
     * Checks if a variable currently has an injected fault.
     *
     * @param variable the OPC UA variable name
     * @return true if the variable has an active fault
     */
    public boolean hasFault(String variable) {
        FaultRecord record = activeFaults.get(variable);
        if (record == null) return false;
        if (record.isExpired()) {
            activeFaults.remove(variable);
            return false;
        }
        return true;
    }

    /**
     * Simulates diagnostic status for a pump variable based on health percentage.
     * Injected faults take priority over model-based diagnostics.
     */
    public DiagnosticResult simulatePumpDiagnostics(String variable, double healthPercent, double vibration, double temperature) {
        FaultRecord fault = activeFaults.get(variable);
        if (fault != null && !fault.isExpired()) {
            return new DiagnosticResult(
                fault.faultType().getStatus(),
                fault.faultType().getSubCode(),
                fault.faultType().getDescription()
            );
        } else if (fault != null && fault.isExpired()) {
            activeFaults.remove(variable);
        }

        Ne107Status status = Ne107Status.Good;
        Ne107SubCode subCode = Ne107SubCode.None;
        String diagString = "";

        switch (variable) {
            case "FlowRate":
                if (healthPercent <= 0) {
                    status = Ne107Status.Failure;
                    subCode = Ne107SubCode.Function;
                    diagString = "Pump failed - no flow";
                } else if (healthPercent < 30) {
                    status = Ne107Status.Failure;
                    subCode = Ne107SubCode.Function;
                    diagString = "Impeller erosion - flow reduced";
                } else if (healthPercent < 60) {
                    status = Ne107Status.Maintenance;
                    subCode = Ne107SubCode.Function;
                    diagString = "Seal leak detected";
                } else if (healthPercent < 80) {
                    status = Ne107Status.Maintenance;
                    subCode = Ne107SubCode.None;
                    diagString = "Early wear detected";
                }
                break;

            case "Vibration":
                if (vibration > 8) {
                    status = Ne107Status.Failure;
                    subCode = Ne107SubCode.Sensor;
                    diagString = "Excessive vibration";
                } else if (vibration > 5) {
                    status = Ne107Status.Maintenance;
                    subCode = Ne107SubCode.None;
                    diagString = "High vibration";
                }
                break;

            case "Temperature":
                if (temperature > 40) {
                    status = Ne107Status.Failure;
                    subCode = Ne107SubCode.Sensor;
                    diagString = "Overheating";
                } else if (temperature > 35) {
                    status = Ne107Status.Maintenance;
                    subCode = Ne107SubCode.None;
                    diagString = "Temperature elevated";
                }
                break;

            case "DeviceStatus":
                if (healthPercent <= 0) {
                    status = Ne107Status.Failure;
                    subCode = Ne107SubCode.Function;
                    diagString = "Device failure";
                } else if (healthPercent < 30) {
                    status = Ne107Status.Failure;
                    subCode = Ne107SubCode.Function;
                    diagString = "Impeller failure";
                } else if (healthPercent < 60) {
                    status = Ne107Status.Maintenance;
                    subCode = Ne107SubCode.Function;
                    diagString = "Seal maintenance required";
                } else if (healthPercent < 80) {
                    status = Ne107Status.Maintenance;
                    subCode = Ne107SubCode.None;
                    diagString = "Scheduled maintenance";
                }
                break;
        }

        return new DiagnosticResult(status, subCode, diagString);
    }
}
