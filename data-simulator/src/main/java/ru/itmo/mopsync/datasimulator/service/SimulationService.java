package ru.itmo.mopsync.datasimulator.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

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

    public static final long DAY_SECS = 24L * 60 * 60;

    private final MetricsTemplateResolver templateResolver;
    private final DataGenerator dataGenerator;
    private final IoTControllerFacade iotControllerFacade;
    private final ThreadPoolTaskScheduler taskScheduler;

    private final Map<String, DeviceSpec> devices = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Sets the simulation specification, resolving templates and creating devices.
     *
     * @param request simulation specification request
     * @return response with created device groups
     */
    public SimulationSpecResponse setSimulationSpec(SimulationSpecRequest request) {
        log.info("Setting simulation specification with {} groups", request.getGroups().size());

        // Clear existing devices and stop any running simulation
        stopSimulation();
        devices.clear();

        List<DeviceGroupResponse> groupResponses = new ArrayList<>();

        for (DeviceGroup group : request.getGroups()) {
            // Resolve metrics
            Map<String, MetricDefinition> metrics;
            if (group.getMetricsTemplate() != null) {
                metrics = templateResolver.resolveTemplate(group.getMetricsTemplate());
            } else if (group.getMetrics() != null) {
                metrics = new HashMap<>(group.getMetrics());
            } else {
                throw Errors.metricsRequiredError();
            }

            // Generate device IDs
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

            DeviceGroupResponse response = new DeviceGroupResponse()
                    .frequency(group.getFrequency())
                    .metrics(metrics)
                    .deviceIds(deviceIds);
            groupResponses.add(response);

            log.info("Created {} devices for group with frequency {}", group.getCount(), group.getFrequency());
        }

        return new SimulationSpecResponse()
                .groups(groupResponses);
    }

    /**
     * Starts the simulation by scheduling data generation tasks for all devices.
     */
    public void startSimulation() {
        if (!scheduledTasks.isEmpty()) {
            throw Errors.simulationAlreadyRunningError();
        }

        if (devices.isEmpty()) {
            throw Errors.noDevicesConfiguredError();
        }

        log.info("Starting simulation for {} devices", devices.size());

        // Schedule tasks for each device
        for (DeviceSpec deviceSpec : devices.values()) {
            scheduleDeviceTask(deviceSpec);
        }

        log.info("Simulation started successfully");
    }

    /**
     * Stops the simulation by cancelling all scheduled tasks.
     */
    public void stopSimulation() {
        if (scheduledTasks.isEmpty()) {
            return;
        }

        log.info("Stopping simulation");

        // Cancel all scheduled tasks
        scheduledTasks.forEach((device, task) -> {
            task.cancel(false);
            log.debug("Cancelled task for device: {}", device);
        });
        scheduledTasks.clear();

        log.info("Simulation stopped successfully");
    }

    /**
     * Schedules a periodic task for a device to generate and send data.
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

        long delaySecs = Math.max(DAY_SECS / deviceSpec.getFrequency(), 1);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, Duration.ofSeconds(delaySecs));
        scheduledTasks.put(deviceSpec.getId(), future);
        log.debug("Scheduled task for device {} with delay {} sec", deviceSpec.getId(), delaySecs);
    }

    @Override
    public void destroy() {
        stopSimulation();
    }
}

