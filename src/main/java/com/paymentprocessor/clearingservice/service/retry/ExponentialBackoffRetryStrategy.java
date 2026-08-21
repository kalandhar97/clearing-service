package com.paymentprocessor.clearingservice.service.retry;

import com.paymentprocessor.clearingservice.config.ClearingProperties;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Exponential-backoff retry strategy driven by {@code clearing.submission.*}
 * configuration.
 */
@Component
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

    private final int maxAttempts;
    private final long backoffInitialMs;
    private final double backoffMultiplier;

    public ExponentialBackoffRetryStrategy(ClearingProperties properties) {
        this.maxAttempts = properties.submission().maxAttempts();
        this.backoffInitialMs = properties.submission().backoffInitialMs();
        this.backoffMultiplier = properties.submission().backoffMultiplier();
    }

    @Override
    public Instant nextAttemptAt(int attemptsMade) {
        if (attemptsMade <= 0) {
            return Instant.now();
        }
        double factor = Math.pow(backoffMultiplier, Math.max(0, attemptsMade - 1));
        long delayMs = (long) (backoffInitialMs * factor);
        return Instant.now().plusMillis(delayMs);
    }

    public int maxAttempts() {
        return maxAttempts;
    }
}
