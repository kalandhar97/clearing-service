package com.paymentprocessor.clearingservice.domain.enums;

/**
 * Lifecycle of an individual transaction as it moves through clearing.
 *
 * PENDING  -> accepted for clearing, not yet assigned to a batch
 * BATCHED  -> assigned to a batch that is being prepared/submitted
 * CLEARED  -> batch was accepted by the participant
 * REJECTED -> participant rejected this transaction; requires correction
 * FAILED   -> unrecoverable error; requires manual intervention
 */
public enum ClearingTransactionStatus {
    PENDING,
    BATCHED,
    CLEARED,
    REJECTED,
    FAILED
}
