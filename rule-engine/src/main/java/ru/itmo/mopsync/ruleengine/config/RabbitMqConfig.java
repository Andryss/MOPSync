package ru.itmo.mopsync.ruleengine.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RabbitMQ.
 */
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final RabbitQueueProperties rabbitQueueProperties;

    /**
     * Creates a queue for device data notifications.
     *
     * @return queue bean
     */
    @Bean
    public Queue deviceDataQueue() {
        return new Queue(rabbitQueueProperties.getDeviceData(), false);
    }
}
