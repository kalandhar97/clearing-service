package com.paymentprocessor.clearingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Strongly-typed binding for all {@code clearing.*} configuration. */
@ConfigurationProperties(prefix = "clearing")
public record ClearingProperties(
        Batching batching,
        Submission submission,
        Storage storage,
        Transport transport,
        Outbox outbox,
        Events events,
        Security security
) {

    public record Batching(
            @DefaultValue("5000") int maxBatchSize,
            @DefaultValue("0 */10 * * * *") String formationCron
    ) {
    }

    public record Submission(
            @DefaultValue("0 * * * * *") String sweepCron,
            @DefaultValue("5") int maxAttempts,
            @DefaultValue("60000") long backoffInitialMs,
            @DefaultValue("2.0") double backoffMultiplier
    ) {
    }

    public record Storage(
            @DefaultValue("./clearing-files") String directory
    ) {
    }

    public record Transport(
            @DefaultValue("true") boolean mock,
            @DefaultValue("http://localhost:9099") String baseUrl,
            @DefaultValue("/clearing/submissions") String submitPath,
            @DefaultValue("") String apiKey,
            @DefaultValue("5000") int connectTimeoutMs,
            @DefaultValue("15000") int responseTimeoutMs
    ) {
    }

    public record Outbox(
            @DefaultValue("*/5 * * * * *") String publishCron,
            @DefaultValue("200") int batchSize
    ) {
    }

    public record Events(
            @DefaultValue("clearingservicetopic") String topic
    ) {
    }

    public record Security(
            @DefaultValue("false") boolean apiKeyEnabled,
            @DefaultValue("") String apiKey
    ) {
    }
}
