package ru.itmo.mopsync.ruleengine.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.CommandLineRunner;
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
        return new Queue(rabbitQueueProperties.getDeviceData(), true);
    }

    /**
     * Configures RabbitTemplate with JSON message converter after auto-configuration.
     * This ensures messages are properly serialized/deserialized as JSON,
     * matching the configuration in iot-controller.
     *
     * @param rabbitTemplate auto-configured RabbitTemplate
     * @return CommandLineRunner to configure the template
     */
    @Bean
    public CommandLineRunner configureRabbitTemplate(RabbitTemplate rabbitTemplate) {
        return args -> rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }
}
