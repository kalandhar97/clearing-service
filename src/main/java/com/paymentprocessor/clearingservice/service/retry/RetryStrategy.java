package com.paymentprocessor.clearingservice.service.retry;

import java.time.Instant;

/**
 * Strategy that decides when a failed operation should be retried. Implementations
 * compute the next attempt time from the number of attempts already made.
 */
public interface RetryStrategy {

    Instant nextAttemptAt(int attemptsMade);

    int maxAttempts();
}
