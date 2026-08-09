package com.paymentprocessor.clearingservice.domain.enums;

/** Publication state of a transactional-outbox event. */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
