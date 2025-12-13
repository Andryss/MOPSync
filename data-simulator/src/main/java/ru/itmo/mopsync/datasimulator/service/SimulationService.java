package ru.itmo.mopsync.datasimulator.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import ru.itmo.mopsync.datasimulator.exception.Errors;
import ru.itmo.mopsync.datasimulator.facade.IoTControllerFacade;
import ru.itmo.mopsync.datasimulator.generated.model.DeviceGroup;
import ru.itmo.mopsync.datasimulator.generated.model.DeviceGroupResponse;
import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecRequest;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecResponse;
import ru.itmo.mopsync.datasimulator.model.DeviceSpec;

/**
 * Service for managing simulation lifecycle and device data generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService implements DisposableBean {

    private static final long SECONDS_PER_DAY = 24L * 60 * 60;
    private static final long MIN_DELAY_SECONDS = 1L;

    private final MetricsTemplateResolver templateResolver;
    private final DataGenerator dataGenerator;
    private final IoTControllerFacade iotControllerFacade;
    private final ThreadPoolTaskScheduler taskScheduler;

    private final Map<String, DeviceSpec> devices = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Lock stateLock = new ReentrantLock();

    /**
     * Sets the simulation specification, resolving templates and creating devices.
     *
     * @param request simulation specification request
     * @return response with created device groups
     */
    public SimulationSpecResponse setSimulationSpec(SimulationSpecRequest request) {
        stateLock.lock();
        try {
            log.info("Setting simulation specification with {} groups", request.getGroups().size());

            // Reset simulation state
            boolean hasActiveTasks = !scheduledTasks.isEmpty();
            boolean hasDevices = !devices.isEmpty();
            if (hasActiveTasks || hasDevices) {
                log.info("Resetting simulation state");
                cancelAllScheduledTasks();
                clearAllDevices();
            }

            List<DeviceGroupResponse> groupResponses = new ArrayList<>();
            for (DeviceGroup group : request.getGroups()) {
                // Resolve metrics
                Map<String, MetricDefinition> metrics;
                String template = group.getMetricsTemplate();
                if (template != null && !template.isEmpty()) {
                    metrics = templateResolver.resolveTemplate(template);
                } else {
                    Map<String, MetricDefinition> manualMetrics = group.getMetrics();
                    if (manualMetrics != null && !manualMetrics.isEmpty()) {
                        metrics = new HashMap<>(manualMetrics);
                    } else {
                        throw Errors.metricsRequiredError();
                    }
                }

                // Create devices for group
                List<String> deviceIds = new ArrayList<>();
                for (int i = 0; i < group.getCount(); i++) {
                    String deviceId = UUID.randomUUID().toString();
                    deviceIds.add(deviceId);
                    DeviceSpec deviceSpec = new DeviceSpec();
                    deviceSpec.setId(deviceId);
                    deviceSpec.setFrequency(group.getFrequency());
                    deviceSpec.setSentPackages(0);
                    deviceSpec.setMetrics(metrics);
                    devices.put(deviceId, deviceSpec);
                }

                log.info("Created {} devices for group with frequency {}", group.getCount(), group.getFrequency());

                DeviceGroupResponse response = new DeviceGroupResponse()
                        .frequency(group.getFrequency())
                        .metrics(metrics)
                        .deviceIds(deviceIds);
                groupResponses.add(response);
            }

            return new SimulationSpecResponse().groups(groupResponses);
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Starts the simulation by scheduling data generation tasks for all devices.
     */
    public void startSimulation() {
        stateLock.lock();
        try {
            if (!scheduledTasks.isEmpty()) {
                throw Errors.simulationAlreadyRunningError();
            }
            if (devices.isEmpty()) {
                throw Errors.noDevicesConfiguredError();
            }

            log.info("Starting simulation for {} devices", devices.size());

            devices.values().forEach(this::scheduleDeviceTask);

            log.info("Simulation started successfully");
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Stops the simulation by cancelling all scheduled tasks and clearing devices.
     */
    public void stopSimulation() {
        stateLock.lock();
        try {
            boolean hasActiveTasks = !scheduledTasks.isEmpty();
            boolean hasDevices = !devices.isEmpty();

            if (!hasActiveTasks && !hasDevices) {
                return;
            }

            log.info("Stopping simulation");
            cancelAllScheduledTasks();
            clearAllDevices();
            log.info("Simulation stopped successfully");
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Schedules a periodic task for a device to generate and send data.
     * Must be called while holding the stateLock.
     *
     * @param deviceSpec device to schedule
     */
    private void scheduleDeviceTask(DeviceSpec deviceSpec) {
        Runnable task = () -> {
            try {
                iotControllerFacade.sendDeviceData(dataGenerator.generateSnapshot(deviceSpec));
                deviceSpec.setSentPackages(deviceSpec.getSentPackages() + 1);
            } catch (Exception e) {
                log.error("Error generating/sending data for device: {}", deviceSpec.getId(), e);
            }
        };

        long delaySeconds = Math.max(SECONDS_PER_DAY / deviceSpec.getFrequency(), MIN_DELAY_SECONDS);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, Duration.ofSeconds(delaySeconds));
        scheduledTasks.put(deviceSpec.getId(), future);
        log.debug("Scheduled task for device {} with delay {} sec", deviceSpec.getId(), delaySeconds);
    }

    @Override
    public void destroy() {
        stopSimulation();
    }

    // Private helper methods
    // Note: These methods assume the caller holds the stateLock

    private void cancelAllScheduledTasks() {
        scheduledTasks.forEach((deviceId, task) -> {
            task.cancel(false);
            log.debug("Cancelled task for device: {}", deviceId);
        });
        scheduledTasks.clear();
    }

    private void clearAllDevices() {
        devices.clear();
    }
}

