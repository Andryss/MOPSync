package ru.itmo.mopsync.iotcontroller.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for RabbitMQ.
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "rabbitmq.queue")
public class RabbitQueueProperties {
    /**
     * Queue name for device data notifications.
     */
    private String deviceData = "device-data-notifications";
}
