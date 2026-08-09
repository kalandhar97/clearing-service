package com.paymentprocessor.clearingservice.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(
        UUID id,
        UUID batchId,
        UUID transactionId,
        String action,
        String actor,
        String participantId,
        String fileName,
        String fileHash,
        String reasonCode,
        String beforeState,
        String afterState,
        String detail,
        Instant createdAt
) {
}
