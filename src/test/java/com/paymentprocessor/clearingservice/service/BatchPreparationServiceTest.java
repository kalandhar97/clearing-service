package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.config.ClearingProperties;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.service.reference.BatchReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.paymentprocessor.clearingservice.service.TestFixtures.defaultProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchPreparationServiceTest {

    @Mock
    private ClearingTransactionRepository txnRepository;
    @Mock
    private ClearingBatchRepository batchRepository;
    @Mock
    private ClearingValidationService validationService;
    @Mock
    private FileGenerationService fileGenerationService;
    @Mock
    private AuditService auditService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private BatchReferenceGenerator referenceGenerator;

    private BatchPreparationService service;
    private final ClearingProperties properties = defaultProperties();

    @BeforeEach
    void setUp() {
        service = new BatchPreparationService(txnRepository, batchRepository, validationService,
                fileGenerationService, auditService, outboxService, referenceGenerator, properties);
    }

    @Test
    void prepareBatchReturnsEmptyWhenNoPendingTransactions() {
        when(txnRepository.findPendingForGroup(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Optional<ClearingBatch> result = service.prepareBatch(Network.VISA, "USD", LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    void prepareBatchCreatesBatchForValidTransactions() {
        ClearingTransaction txn = pendingTransaction();
        when(txnRepository.findPendingForGroup(Network.VISA, "USD", txn.getSettlementDate(),
                PageRequest.of(0, properties.batching().maxBatchSize())))
                .thenReturn(List.of(txn));
        when(validationService.validate(txn)).thenReturn(Collections.emptyList());
        when(referenceGenerator.generate(any(), any(), any())).thenReturn("REF-123");
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<ClearingBatch> result = service.prepareBatch(Network.VISA, "USD", txn.getSettlementDate());

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BatchStatus.VALIDATED);
        verify(fileGenerationService).generate(any(), any());
        verify(outboxService).append(any(), any(), any(), any());
    }

    @Test
    void prepareBatchReturnsEmptyWhenAllTransactionsInvalid() {
        ClearingTransaction txn = pendingTransaction();
        when(txnRepository.findPendingForGroup(Network.VISA, "USD", txn.getSettlementDate(),
                PageRequest.of(0, properties.batching().maxBatchSize())))
                .thenReturn(List.of(txn));
        when(validationService.validate(txn)).thenReturn(List.of("amountMinor must be positive"));
        when(txnRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<ClearingBatch> result = service.prepareBatch(Network.VISA, "USD", txn.getSettlementDate());

        assertThat(result).isEmpty();
    }

    private ClearingTransaction pendingTransaction() {
        return ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA, TransactionType.SALE,
                "USD", 1000, null, LocalDate.now(), null, null, null, null, null);
    }
}
