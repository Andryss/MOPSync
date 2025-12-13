package ru.itmo.mopsync.ruleengine.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.itmo.mopsync.ruleengine.exception.BaseException;
import ru.itmo.mopsync.ruleengine.service.DeviceDataProcessingService;

/**
 * RabbitMQ listener for device data notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceDataNotificationListener {

    private final DeviceDataProcessingService deviceDataProcessingService;

    /**
     * Listens to device data notifications queue.
     * Receives MongoDB document ID and processes the device data.
     *
     * @param deviceDataId MongoDB document ID of the device data
     */
    @RabbitListener(queues = "${rabbitmq.queue.device-data:device-data-notifications}")
    public void handleDeviceDataNotification(String deviceDataId) {
        log.debug("Received device data notification for id: {}", deviceDataId);
        try {
            deviceDataProcessingService.processDeviceData(deviceDataId);
        } catch (BaseException e) {
            log.error("Error processing device data with id: {} - {} ({})",
                    deviceDataId, e.getHumanMessage(), e.getMessage());
            // Don't rethrow - let RabbitMQ handle retries if configured
        } catch (Exception e) {
            log.error("Unexpected error processing device data with id: {}", deviceDataId, e);
            // Don't rethrow - let RabbitMQ handle retries if configured
        }
    }
}
