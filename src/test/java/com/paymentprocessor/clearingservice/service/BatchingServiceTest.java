package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.dto.BatchFormationResponse;
import com.paymentprocessor.clearingservice.repository.BatchGroupKey;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchingServiceTest {

    @Mock
    private ClearingTransactionRepository txnRepository;
    @Mock
    private BatchPreparationService batchPreparationService;

    @InjectMocks
    private BatchingService batchingService;

    @Test
    void formBatchesReturnsCreatedBatchesAndCounts() {
        BatchGroupKey group = new BatchGroupKey() {
            @Override
            public Network getNetwork() { return Network.VISA; }
            @Override
            public String getCurrency() { return "USD"; }
            @Override
            public LocalDate getSettlementDate() { return LocalDate.now(); }
        };
        when(txnRepository.findPendingGroups()).thenReturn(List.of(group));
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.now(), com.paymentprocessor.clearingservice.domain.enums.ClearingFormat.ISO8583, null);
        batch.recordTotals(2, 2000L);
        when(batchPreparationService.prepareBatch(any(), any(), any())).thenReturn(Optional.of(batch));

        BatchFormationResponse response = batchingService.formBatches();

        assertThat(response.batchesCreated()).isEqualTo(1);
        assertThat(response.transactionsBatched()).isEqualTo(2);
        assertThat(response.batches()).hasSize(1);
    }

    @Test
    void formBatchesIsolatesFailures() {
        BatchGroupKey group = new BatchGroupKey() {
            @Override
            public Network getNetwork() { return Network.VISA; }
            @Override
            public String getCurrency() { return "USD"; }
            @Override
            public LocalDate getSettlementDate() { return LocalDate.now(); }
        };
        when(txnRepository.findPendingGroups()).thenReturn(List.of(group));
        when(batchPreparationService.prepareBatch(any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        BatchFormationResponse response = batchingService.formBatches();

        assertThat(response.batchesCreated()).isZero();
    }
}
