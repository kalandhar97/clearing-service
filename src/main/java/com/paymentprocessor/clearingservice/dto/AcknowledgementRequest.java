package com.paymentprocessor.clearingservice.dto;

import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Inbound acknowledgement from a clearing participant for a submitted batch.
 * For {@code PARTIAL} acknowledgements, {@code rejectedSourceTransactionIds}
 * identifies the transactions that were not accepted.
 */
public record AcknowledgementRequest(
        @NotNull AckStatus status,
        String ackReference,
        String reasonCode,
        String message,
        String rawPayload,
        List<String> rejectedSourceTransactionIds
) {
}
