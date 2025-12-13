package ru.itmo.mopsync.datasimulator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import ru.itmo.mopsync.datasimulator.facade.IoTControllerFacade;
import ru.itmo.mopsync.datasimulator.model.DeviceSpec;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service for managing scheduled task lifecycle.
 * Handles scheduling, cancellation, and tracking of device data generation tasks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSchedulerService {

    private static final long SECONDS_PER_DAY = 24L * 60 * 60;
    private static final long MIN_DELAY_SECONDS = 1L;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final DataGenerator dataGenerator;
    private final IoTControllerFacade iotControllerFacade;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Schedules a periodic task for a device to generate and send data.
     *
     * @param deviceSpec device to schedule
     */
    public void scheduleDeviceTask(DeviceSpec deviceSpec) {
        Runnable task = () -> {
            try {
                iotControllerFacade.sendDeviceData(dataGenerator.generateSnapshot(deviceSpec));
                deviceSpec.setSentPackages(deviceSpec.getSentPackages() + 1);
            } catch (Exception e) {
                log.error("Error generating/sending data for device: {}", deviceSpec.getId(), e);
            }
        };

        long delaySeconds = calculateDelaySeconds(deviceSpec.getFrequency());
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, Duration.ofSeconds(delaySeconds));
        scheduledTasks.put(deviceSpec.getId(), future);
        log.debug("Scheduled task for device {} with delay {} sec", deviceSpec.getId(), delaySeconds);
    }

    /**
     * Cancels all scheduled tasks.
     */
    public void cancelAll() {
        scheduledTasks.forEach((deviceId, task) -> {
            task.cancel(false);
            log.debug("Cancelled task for device: {}", deviceId);
        });
        scheduledTasks.clear();
    }

    /**
     * Checks if there are any active scheduled tasks.
     *
     * @return true if tasks are running
     */
    public boolean hasActiveTasks() {
        return !scheduledTasks.isEmpty();
    }

    /**
     * Calculates the delay in seconds between data generation cycles.
     *
     * @param frequency packages per day
     * @return delay in seconds (minimum 1 second)
     */
    private long calculateDelaySeconds(int frequency) {
        return Math.max(SECONDS_PER_DAY / frequency, MIN_DELAY_SECONDS);
    }
}
