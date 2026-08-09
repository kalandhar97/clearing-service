package com.paymentprocessor.clearingservice.event;

import com.paymentprocessor.clearingservice.config.ClearingProperties;
import com.paymentprocessor.clearingservice.domain.OutboxEvent;
import com.paymentprocessor.clearingservice.domain.enums.OutboxStatus;
import com.paymentprocessor.clearingservice.repository.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Relays outbox events to Kafka. Reads pending (and previously failed) events
 * and publishes each with the aggregate id as the partition key, preserving
 * per-batch ordering. Uses the aggregate id as key and marks each row published
 * only after the broker acknowledges the send.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public OutboxPublisher(OutboxEventRepository repository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           ClearingProperties properties) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.events().topic();
        this.batchSize = properties.outbox().batchSize();
    }

    public void publishPending() {
        publish(OutboxStatus.PENDING);
        publish(OutboxStatus.FAILED);
    }

    private void publish(OutboxStatus status) {
        List<OutboxEvent> events = repository.findByStatusOrderByCreatedAtAsc(
                status, PageRequest.of(0, batchSize));
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                        .get(30, TimeUnit.SECONDS);
                event.markPublished();
                repository.save(event);
                log.debug("Published event {} ({}) for {}",
                        event.getEventType(), event.getId(), event.getAggregateId());
            } catch (Exception e) {
                event.markFailed(e.getMessage());
                repository.save(event);
                log.error("Failed to publish outbox event {} ({}); will retry",
                        event.getId(), event.getEventType(), e);
            }
        }
    }
}
