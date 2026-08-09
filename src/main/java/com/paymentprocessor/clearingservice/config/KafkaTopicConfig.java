package com.paymentprocessor.clearingservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declares the clearing events topic so it is auto-created on startup. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic clearingEventsTopic(ClearingProperties properties) {
        return TopicBuilder.name(properties.events().topic())
                .partitions(6)
                .replicas(1)
                .build();
    }
}
