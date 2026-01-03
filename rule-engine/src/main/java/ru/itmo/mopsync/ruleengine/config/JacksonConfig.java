package ru.itmo.mopsync.ruleengine.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Jackson JSON message conversion.
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates a JSON message converter bean.
     * Spring Boot will automatically use this for both RabbitTemplate and listener containers.
     * This ensures messages are properly serialized/deserialized as JSON,
     * matching the configuration in iot-controller.
     *
     * @return Jackson2JsonMessageConverter bean
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

