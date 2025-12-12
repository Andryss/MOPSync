package ru.itmo.mopsync.datasimulator.service;

import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;
import ru.itmo.mopsync.datasimulator.model.DeviceSnapshot;
import ru.itmo.mopsync.datasimulator.model.DeviceSpec;

/**
 * Service for generating metric values based on metric definitions.
 */
@Service
public class DataGenerator {

    private final Random random = new Random();

    /**
     * Generates a data point for a device based on its metrics definition.
     *
     * @param deviceSpec device definition
     * @return generated data point as a map
     */
    public DeviceSnapshot generateSnapshot(DeviceSpec deviceSpec) {
        DeviceSnapshot snapshot = new DeviceSnapshot();
        snapshot.setDeviceId(deviceSpec.getId());
        snapshot.setSequenceId(deviceSpec.getSentPackages());
        snapshot.setTimestamp(Instant.now());

        Map<String, Object> metrics = new HashMap<>();
        deviceSpec.getMetrics().forEach((name, definition) -> metrics.put(name, generateValue(definition)));
        snapshot.setMetrics(metrics);

        return snapshot;
    }

    /**
     * Generates a single metric value based on its definition.
     *
     * @param definition metric definition
     * @return generated value
     */
    private Object generateValue(MetricDefinition definition) {
        MetricDefinition.TypeEnum type = definition.getType();
        if (type == null) {
            throw new IllegalArgumentException("Metric type cannot be null");
        }
        return switch (type) {
            case NUMBER -> generateNumber(definition.getMin(), definition.getMax());
            case INTEGER -> generateInteger(definition.getMin(), definition.getMax());
            case STRING -> generateString(definition.getValues());
        };
    }

    /**
     * Generates a random number within the specified range.
     *
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return generated number
     */
    private Double generateNumber(Double min, Double max) {
        if (min == null || max == null) {
            return random.nextDouble() * 100.0;
        }
        return min + (max - min) * random.nextDouble();
    }

    /**
     * Generates a random integer within the specified range.
     *
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return generated integer
     */
    private Integer generateInteger(Double min, Double max) {
        if (min == null || max == null) {
            return random.nextInt(100);
        }
        int minInt = min.intValue();
        int maxInt = max.intValue();
        return minInt + random.nextInt(maxInt - minInt + 1);
    }

    /**
     * Generates a random string from the list of possible values.
     *
     * @param values list of possible string values
     * @return generated string
     */
    private String generateString(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "default";
        }
        return values.get(random.nextInt(values.size()));
    }
}

