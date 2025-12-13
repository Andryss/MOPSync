package ru.itmo.mopsync.ruleengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Main application class for Rule Engine service.
 */
@SpringBootApplication
@EnableMongoRepositories
public class RuleEngineApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(RuleEngineApplication.class, args);
    }

}
