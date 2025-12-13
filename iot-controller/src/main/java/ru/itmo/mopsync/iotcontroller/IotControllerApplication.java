package ru.itmo.mopsync.iotcontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Main application class for IoT Controller service.
 */
@SpringBootApplication
@EnableMongoRepositories
public class IotControllerApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(IotControllerApplication.class, args);
    }

}
