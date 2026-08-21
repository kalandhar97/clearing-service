package com.paymentprocessor.clearingservice.service.retry;

import com.paymentprocessor.clearingservice.config.ClearingProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ExponentialBackoffRetryStrategyTest {

    private final ClearingProperties properties = new ClearingProperties(
            new ClearingProperties.Batching(5000, "0 */10 * * * *"),
            new ClearingProperties.Submission("0 * * * * *", 5, 60_000L, 2.0),
            new ClearingProperties.Storage("./clearing-files"),
            new ClearingProperties.Transport(true, "http://localhost:9099", "/clearing/submissions", "", 5000, 15000),
            new ClearingProperties.Outbox("*/5 * * * * *", 200),
            new ClearingProperties.Events("clearingservicetopic"),
            new ClearingProperties.Security(false, "")
    );

    private final ExponentialBackoffRetryStrategy strategy = new ExponentialBackoffRetryStrategy(properties);

    @Test
    void firstRetryIsImmediate() {
        Instant now = Instant.now();
        Instant next = strategy.nextAttemptAt(0);
        assertThat(next).isAfterOrEqualTo(now);
        assertThat(next).isBefore(now.plusSeconds(1));
    }

    @Test
    void backoffDoublesEachAttempt() {
        Instant first = strategy.nextAttemptAt(1);
        Instant second = strategy.nextAttemptAt(2);
        Instant third = strategy.nextAttemptAt(3);

        assertThat(second).isAfterOrEqualTo(first.plusMillis(60_000));
        assertThat(third).isAfterOrEqualTo(second.plusMillis(120_000));
    }

    @Test
    void maxAttemptsComesFromProperties() {
        assertThat(strategy.maxAttempts()).isEqualTo(5);
    }
}
