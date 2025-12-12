package ru.itmo.mopsync.datasimulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for task scheduler.
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * Creates ThreadPoolTaskScheduler bean for scheduling tasks.
     *
     * @return ThreadPoolTaskScheduler instance
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("simulation-");
        scheduler.initialize();
        return scheduler;
    }
}

