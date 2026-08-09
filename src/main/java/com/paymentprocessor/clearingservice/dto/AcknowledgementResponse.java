package com.paymentprocessor.clearingservice.dto;

import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import java.time.Instant;
import java.util.UUID;

public record AcknowledgementResponse(
        UUID id,
        UUID batchId,
        String ackReference,
        AckStatus status,
        String reasonCode,
        String message,
        Instant receivedAt,
        BatchStatus resultingBatchStatus
) {
}
