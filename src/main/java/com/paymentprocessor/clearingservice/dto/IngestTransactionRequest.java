package com.paymentprocessor.clearingservice.dto;

import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Request to ingest a captured transaction that is eligible for clearing. */
public record IngestTransactionRequest(

        @NotBlank @Size(max = 64) String sourceTransactionId,
        @NotBlank @Size(max = 64) String merchantId,
        @NotNull Network network,
        @NotNull TransactionType transactionType,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @Positive Long amountMinor,
        @Size(max = 32) String region,
        @NotNull LocalDate settlementDate,
        @Size(max = 4) String mcc,
        @Size(max = 16) String authCode,
        @Size(max = 32) String arn,
        @Size(max = 64) String panToken,
        Instant capturedAt
) {
}
