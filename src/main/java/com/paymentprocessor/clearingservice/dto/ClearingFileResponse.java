package com.paymentprocessor.clearingservice.dto;

import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import java.time.Instant;
import java.util.UUID;

public record ClearingFileResponse(
        UUID id,
        UUID batchId,
        ClearingFormat format,
        String fileName,
        String contentHash,
        long sizeBytes,
        int recordCount,
        long controlTotalMinor,
        String storageUri,
        String signature,
        Instant generatedAt
) {
}
