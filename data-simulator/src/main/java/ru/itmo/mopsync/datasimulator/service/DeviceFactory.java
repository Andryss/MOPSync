package ru.itmo.mopsync.datasimulator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itmo.mopsync.datasimulator.generated.model.DeviceGroup;
import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;
import ru.itmo.mopsync.datasimulator.model.DeviceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Factory for creating device specifications from device groups.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceFactory {

    private final MetricsResolverService metricsResolver;
    private final DeviceRegistry deviceRegistry;

    /**
     * Creates devices for a device group and registers them.
     *
     * @param group device group configuration
     * @return list of created device IDs
     */
    public List<String> createDevicesForGroup(DeviceGroup group) {
        Map<String, MetricDefinition> metrics = metricsResolver.resolveMetrics(group);
        List<String> deviceIds = new ArrayList<>();

        for (int i = 0; i < group.getCount(); i++) {
            String deviceId = UUID.randomUUID().toString();
            DeviceSpec deviceSpec = createDeviceSpec(deviceId, group.getFrequency(), metrics);
            deviceRegistry.register(deviceSpec);
            deviceIds.add(deviceId);
        }

        log.info("Created {} devices for group with frequency {}", group.getCount(), group.getFrequency());
        return deviceIds;
    }

    /**
     * Creates a device specification.
     *
     * @param deviceId unique device identifier
     * @param frequency packages per day
     * @param metrics metrics definition
     * @return created device spec
     */
    private DeviceSpec createDeviceSpec(String deviceId, Integer frequency, Map<String, MetricDefinition> metrics) {
        DeviceSpec deviceSpec = new DeviceSpec();
        deviceSpec.setId(deviceId);
        deviceSpec.setFrequency(frequency);
        deviceSpec.setSentPackages(0);
        deviceSpec.setMetrics(metrics);
        return deviceSpec;
    }
}
