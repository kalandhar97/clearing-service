package com.paymentprocessor.clearingservice.dto;

import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClearingTransactionResponse(
        UUID id,
        String sourceTransactionId,
        String merchantId,
        Network network,
        TransactionType transactionType,
        String currency,
        long amountMinor,
        String region,
        LocalDate settlementDate,
        String mcc,
        String authCode,
        String arn,
        String panToken,
        Instant capturedAt,
        ClearingTransactionStatus status,
        String rejectionReason,
        UUID batchId,
        Instant createdAt,
        Instant updatedAt
) {
}
