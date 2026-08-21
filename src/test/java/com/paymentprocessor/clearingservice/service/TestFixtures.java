package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.config.ClearingProperties;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;

import java.time.LocalDate;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static ClearingTransaction sampleTransaction() {
        return ClearingTransaction.ingest("SRC-1", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 1000, null, LocalDate.now(),
                "1234", "AUTH", "ARN", "PAN", null);
    }

    public static ClearingBatch sampleBatch() {
        return ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.now(), ClearingFormat.ISO8583, null);
    }

    public static ClearingProperties defaultProperties() {
        return new ClearingProperties(
                new ClearingProperties.Batching(5000, "0 */10 * * * *"),
                new ClearingProperties.Submission("0 * * * * *", 5, 60_000L, 2.0),
                new ClearingProperties.Storage("./clearing-files-test"),
                new ClearingProperties.Transport(true, "http://localhost:9099", "/clearing/submissions", "", 5000, 15000),
                new ClearingProperties.Outbox("*/5 * * * * *", 200),
                new ClearingProperties.Events("clearingservicetopic"),
                new ClearingProperties.Security(false, "")
        );
    }
}
