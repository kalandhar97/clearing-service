package com.paymentprocessor.clearingservice.domain.enums;

/** Outcome reported by a clearing participant for a submitted batch. */
public enum AckStatus {
    /** Receipt confirmed; structural validation passed. */
    ACKNOWLEDGED,
    /** All transactions accepted for clearing and settlement. */
    ACCEPTED,
    /** File or all transactions rejected; correction required. */
    REJECTED,
    /** Some transactions accepted, others rejected. */
    PARTIAL
}
