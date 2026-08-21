package com.paymentprocessor.clearingservice.service.acknowledgement;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import com.paymentprocessor.clearingservice.dto.AcknowledgementRequest;
import com.paymentprocessor.clearingservice.dto.AcknowledgementResponse;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.repository.ClearingAcknowledgementRepository;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.service.AcknowledgementService;
import com.paymentprocessor.clearingservice.service.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcknowledgementServiceTest {

    @Mock
    private ClearingBatchRepository batchRepository;
    @Mock
    private ClearingAcknowledgementRepository ackRepository;
    @Mock
    private ClearingTransactionRepository txnRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private OutboxService outboxService;

    private AcknowledgementService service;

    @BeforeEach
    void setUp() {
        AcknowledgementHandlerRegistry registry = new AcknowledgementHandlerRegistry(List.of(
                new AcknowledgedHandler(),
                new AcceptedHandler(),
                new PartialAcceptedHandler(),
                new RejectedHandler()));
        service = new AcknowledgementService(batchRepository, ackRepository, txnRepository,
                auditService, outboxService, registry);
    }

    @Test
    void acceptMovesBatchToCompletedAndClearsTransactions() {
        ClearingBatch batch = validatedBatch();
        batch.transitionTo(BatchStatus.SENT);
        ClearingTransaction txn = transaction(batch.getId());

        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(txnRepository.findByBatchId(batch.getId())).thenReturn(Collections.singletonList(txn));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AcknowledgementResponse response = service.process(batch.getId(), request(AckStatus.ACCEPTED));

        assertThat(response.resultingBatchStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(txn.getStatus()).isEqualTo(com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus.CLEARED);
    }

    @Test
    void partialAcceptRejectsListedTransactions() {
        ClearingBatch batch = validatedBatch();
        batch.transitionTo(BatchStatus.SENT);
        ClearingTransaction txn = transaction(batch.getId());

        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(txnRepository.findByBatchId(batch.getId())).thenReturn(Collections.singletonList(txn));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AcknowledgementRequest request = new AcknowledgementRequest(
                AckStatus.PARTIAL, null, "PARTIAL", null, null, Collections.singletonList("SRC-1"));
        AcknowledgementResponse response = service.process(batch.getId(), request);

        assertThat(response.resultingBatchStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(txn.getStatus()).isEqualTo(com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus.REJECTED);
    }

    @Test
    void rejectMarksBatchAndTransactionsFailed() {
        ClearingBatch batch = validatedBatch();
        batch.transitionTo(BatchStatus.SENT);
        ClearingTransaction txn = transaction(batch.getId());

        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(txnRepository.findByBatchId(batch.getId())).thenReturn(Collections.singletonList(txn));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AcknowledgementResponse response = service.process(batch.getId(), request(AckStatus.REJECTED));

        assertThat(response.resultingBatchStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(txn.getStatus()).isEqualTo(com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus.REJECTED);
    }

    private ClearingBatch validatedBatch() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.now(), com.paymentprocessor.clearingservice.domain.enums.ClearingFormat.ISO8583, null);
        batch.transitionTo(BatchStatus.VALIDATED);
        return batch;
    }

    private ClearingTransaction transaction(UUID batchId) {
        ClearingTransaction txn = ClearingTransaction.ingest("SRC-1", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 1000, null, LocalDate.now(), null, null, null, null, null);
        txn.assignToBatch(batchId);
        return txn;
    }

    private AcknowledgementRequest request(AckStatus status) {
        return new AcknowledgementRequest(status, null, null, null, null, null);
    }
}
