package com.paymentprocessor.clearingservice.domain;

import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClearingTransactionTest {

    @Test
    void ingestCreatesPendingTransaction() {
        ClearingTransaction txn = ClearingTransaction.ingest("SRC-1", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 1000, null, LocalDate.now(),
                "1234", "AUTH", "ARN", "PAN", null);
        assertThat(txn.getStatus()).isEqualTo(ClearingTransactionStatus.PENDING);
        assertThat(txn.getSourceTransactionId()).isEqualTo("SRC-1");
    }

    @Test
    void assignToBatchSetsStatusBatched() {
        ClearingTransaction txn = sampleTransaction();
        UUID batchId = UUID.randomUUID();
        txn.assignToBatch(batchId);
        assertThat(txn.getStatus()).isEqualTo(ClearingTransactionStatus.BATCHED);
        assertThat(txn.getBatchId()).isEqualTo(batchId);
    }

    @Test
    void markClearedAndRejectedTransitions() {
        ClearingTransaction txn = sampleTransaction();
        txn.markCleared();
        assertThat(txn.getStatus()).isEqualTo(ClearingTransactionStatus.CLEARED);
        txn.markRejected("reason");
        assertThat(txn.getStatus()).isEqualTo(ClearingTransactionStatus.REJECTED);
        assertThat(txn.getRejectionReason()).isEqualTo("reason");
    }

    @Test
    void returnToPendingResetsBatch() {
        ClearingTransaction txn = sampleTransaction();
        txn.assignToBatch(UUID.randomUUID());
        txn.returnToPending();
        assertThat(txn.getStatus()).isEqualTo(ClearingTransactionStatus.PENDING);
        assertThat(txn.getBatchId()).isNull();
    }

    private ClearingTransaction sampleTransaction() {
        return ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA, TransactionType.SALE,
                "USD", 100, null, LocalDate.now(), null, null, null, null, null);
    }
}
