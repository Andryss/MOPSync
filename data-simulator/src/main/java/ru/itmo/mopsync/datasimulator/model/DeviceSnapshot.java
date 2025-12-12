package ru.itmo.mopsync.datasimulator.model;

import java.time.Instant;
import java.util.Map;

import lombok.Data;

/**
 * Model representing a device with its configuration.
 */
@Data
public class DeviceSnapshot {
    private String deviceId;
    private int sequenceId;
    private Instant timestamp;
    private Map<String, Object> metrics;
}

