package ru.itmo.mopsync.datasimulator.model;

import lombok.Data;
import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;
import java.util.Map;

/**
 * Model representing a device with its configuration.
 */
@Data
public class DeviceSpec {
    private String id;
    private int frequency; // packages per day
    private int sentPackages;
    private Map<String, MetricDefinition> metrics;
}

