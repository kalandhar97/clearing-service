package com.paymentprocessor.clearingservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.clearingservice.domain.OutboxEvent;
import com.paymentprocessor.clearingservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Appends domain events to the transactional outbox. Callers must already be
 * inside a transaction so the event is committed atomically with the state
 * change that produced it; a separate publisher relays events to Kafka.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String aggregateType, String aggregateId, String eventType, Object payload) {
        String json = serialize(payload);
        repository.save(OutboxEvent.of(aggregateType, aggregateId, eventType, json));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}
