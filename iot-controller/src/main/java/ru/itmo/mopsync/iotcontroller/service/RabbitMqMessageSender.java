package ru.itmo.mopsync.iotcontroller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ru.itmo.mopsync.iotcontroller.config.RabbitQueueProperties;
import ru.itmo.mopsync.iotcontroller.model.DeviceDataNotification;

/**
 * Service for sending messages to RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitQueueProperties rabbitQueueProperties;

    /**
     * Sends a notification to RabbitMQ when device data is saved.
     *
     * @param deviceDataId MongoDB document ID of the saved device data
     */
    public void sendDeviceDataNotification(String deviceDataId) {
        String queueName = rabbitQueueProperties.getDeviceData();
        log.debug("Sending notification to queue {} for device data id: {}", queueName, deviceDataId);
        DeviceDataNotification notification = new DeviceDataNotification(deviceDataId);
        rabbitTemplate.convertAndSend(queueName, notification);
        log.debug("Notification sent successfully");
    }
}
