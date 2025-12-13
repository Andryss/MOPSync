package ru.itmo.mopsync.datasimulator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itmo.mopsync.datasimulator.exception.BaseException;
import ru.itmo.mopsync.datasimulator.exception.Errors;
import ru.itmo.mopsync.datasimulator.generated.model.DeviceGroup;
import ru.itmo.mopsync.datasimulator.generated.model.DeviceGroupResponse;
import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecRequest;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecResponse;
import ru.itmo.mopsync.datasimulator.model.DeviceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages simulation state and coordinates between components.
 * Thread-safe state management with lock protection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationStateManager {

    private final DeviceRegistry deviceRegistry;
    private final TaskSchedulerService taskScheduler;
    private final DeviceFactory deviceFactory;
    private final MetricsResolverService metricsResolver;

    private final Lock stateLock = new ReentrantLock();

    /**
     * Resets simulation state by stopping tasks and clearing devices.
     * Must be called while holding the stateLock.
     */
    public void resetState() {
        boolean hasActiveTasks = taskScheduler.hasActiveTasks();
        boolean hasDevices = !deviceRegistry.isEmpty();

        if (hasActiveTasks || hasDevices) {
            log.info("Resetting simulation state");
            taskScheduler.cancelAll();
            deviceRegistry.clear();
        }
    }

    /**
     * Creates devices from simulation specification.
     * Must be called while holding the stateLock.
     *
     * @param request simulation specification
     * @return response with created device groups
     */
    public SimulationSpecResponse createDevicesFromSpec(SimulationSpecRequest request) {
        log.info("Setting simulation specification with {} groups", request.getGroups().size());

        List<DeviceGroupResponse> groupResponses = new ArrayList<>();
        for (DeviceGroup group : request.getGroups()) {
            Map<String, MetricDefinition> metrics = metricsResolver.resolveMetrics(group);
            List<String> deviceIds = deviceFactory.createDevicesForGroup(group);

            DeviceGroupResponse response = new DeviceGroupResponse()
                    .frequency(group.getFrequency())
                    .metrics(metrics)
                    .deviceIds(deviceIds);
            groupResponses.add(response);
        }

        return new SimulationSpecResponse().groups(groupResponses);
    }

    /**
     * Validates that simulation can be started.
     * Must be called while holding the stateLock.
     *
     * @throws BaseException if validation fails
     */
    public void validateCanStart() {
        if (taskScheduler.hasActiveTasks()) {
            throw Errors.simulationAlreadyRunningError();
        }
        if (deviceRegistry.isEmpty()) {
            throw Errors.noDevicesConfiguredError();
        }
    }

    /**
     * Starts simulation by scheduling tasks for all devices.
     * Must be called while holding the stateLock.
     */
    public void startSimulation() {
        log.info("Starting simulation for {} devices", deviceRegistry.size());

        for (DeviceSpec deviceSpec : deviceRegistry.getAll()) {
            taskScheduler.scheduleDeviceTask(deviceSpec);
        }

        log.info("Simulation started successfully");
    }

    /**
     * Stops simulation by cancelling tasks and clearing devices.
     * Must be called while holding the stateLock.
     */
    public void stopSimulation() {
        boolean hasActiveTasks = taskScheduler.hasActiveTasks();
        boolean hasDevices = !deviceRegistry.isEmpty();

        if (!hasActiveTasks && !hasDevices) {
            return;
        }

        log.info("Stopping simulation");
        taskScheduler.cancelAll();
        deviceRegistry.clear();
        log.info("Simulation stopped successfully");
    }

    /**
     * Acquires the state lock. Must be released in finally block.
     */
    public void lock() {
        stateLock.lock();
    }

    /**
     * Releases the state lock.
     */
    public void unlock() {
        stateLock.unlock();
    }
}
