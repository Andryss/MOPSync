package ru.itmo.mopsync.datasimulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.mopsync.datasimulator.generated.iotcontroller.api.DeviceDataApi;
import ru.itmo.mopsync.datasimulator.generated.iotcontroller.invoker.ApiClient;

/**
 * Configuration for IoT controller client.
 */
@Configuration
public class IoTControllerClientConfig {

    /**
     * Creates DeviceDataApi bean for IoT controller API client.
     * Uses default server URL from OpenAPI spec.
     *
     * @return DeviceDataApi instance
     */
    @Bean
    public DeviceDataApi deviceDataApi() {
        ApiClient apiClient = new ApiClient();
        return new DeviceDataApi(apiClient);
    }
}
