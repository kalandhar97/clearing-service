package com.paymentprocessor.clearingservice.dto;

import java.util.List;

/** Result of a batch-formation run. */
public record BatchFormationResponse(
        int batchesCreated,
        int transactionsBatched,
        List<ClearingBatchResponse> batches
) {
}
