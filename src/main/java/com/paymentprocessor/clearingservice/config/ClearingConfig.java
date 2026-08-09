package com.paymentprocessor.clearingservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Core wiring: binds {@link ClearingProperties}, enables scheduling, and
 * provides a dedicated scheduler pool so the batching, submission and outbox
 * jobs do not block one another.
 */
@Configuration
@EnableConfigurationProperties(ClearingProperties.class)
@EnableScheduling
public class ClearingConfig {

    @Bean
    public TaskScheduler clearingTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("clearing-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
