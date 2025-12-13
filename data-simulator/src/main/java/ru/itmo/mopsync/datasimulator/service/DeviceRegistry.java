package ru.itmo.mopsync.datasimulator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itmo.mopsync.datasimulator.model.DeviceSpec;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing device storage and retrieval.
 * Thread-safe implementation using ConcurrentHashMap.
 */
@Slf4j
@Component
public class DeviceRegistry {

    private final Map<String, DeviceSpec> devices = new ConcurrentHashMap<>();

    /**
     * Stores a device in the registry.
     *
     * @param deviceSpec device to store
     */
    public void register(DeviceSpec deviceSpec) {
        devices.put(deviceSpec.getId(), deviceSpec);
        log.debug("Registered device: {}", deviceSpec.getId());
    }

    /**
     * Retrieves a device by ID.
     *
     * @param deviceId device identifier
     * @return device spec or null if not found
     */
    public DeviceSpec get(String deviceId) {
        return devices.get(deviceId);
    }

    /**
     * Returns all registered devices.
     *
     * @return collection of all devices
     */
    public Collection<DeviceSpec> getAll() {
        return devices.values();
    }

    /**
     * Checks if registry is empty.
     *
     * @return true if no devices registered
     */
    public boolean isEmpty() {
        return devices.isEmpty();
    }

    /**
     * Returns the number of registered devices.
     *
     * @return device count
     */
    public int size() {
        return devices.size();
    }

    /**
     * Clears all devices from the registry.
     */
    public void clear() {
        int count = devices.size();
        devices.clear();
        log.debug("Cleared {} devices from registry", count);
    }
}
