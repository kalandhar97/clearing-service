package com.paymentprocessor.clearingservice.dto;

import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClearingBatchResponse(
        UUID id,
        String reference,
        Network network,
        String currency,
        String region,
        LocalDate settlementDate,
        ClearingFormat format,
        BatchStatus status,
        int transactionCount,
        long totalAmountMinor,
        Instant cutoffAt,
        int submissionAttempts,
        Instant nextAttemptAt,
        String ackReference,
        String lastError,
        Instant createdAt,
        Instant validatedAt,
        Instant sentAt,
        Instant acknowledgedAt,
        Instant completedAt,
        Instant failedAt,
        Instant updatedAt
) {
}
