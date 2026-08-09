package com.paymentprocessor.clearingservice.domain.enums;

import java.util.Set;

/**
 * Lifecycle of a clearing batch.
 *
 * CREATED -> VALIDATED -> SENT -> ACKNOWLEDGED -> COMPLETED
 * Any non-terminal state may transition to FAILED.
 *
 * A batch is only moved to SENT once transmission actually succeeds; transient
 * submission failures leave it in VALIDATED and reschedule a retry.
 */
public enum BatchStatus {
    CREATED,
    VALIDATED,
    SENT,
    ACKNOWLEDGED,
    COMPLETED,
    FAILED;

    private static final Set<BatchStatus> TERMINAL = Set.of(COMPLETED, FAILED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** Whether a direct transition from this state to {@code target} is allowed. */
    public boolean canTransitionTo(BatchStatus target) {
        if (this == target) {
            return false;
        }
        return switch (this) {
            case CREATED      -> target == VALIDATED || target == FAILED;
            case VALIDATED    -> target == SENT || target == FAILED;
            case SENT         -> target == ACKNOWLEDGED || target == FAILED;
            case ACKNOWLEDGED -> target == COMPLETED || target == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}
