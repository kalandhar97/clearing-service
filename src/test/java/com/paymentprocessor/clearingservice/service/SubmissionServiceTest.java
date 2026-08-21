package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingFileRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.service.retry.RetryStrategy;
import com.paymentprocessor.clearingservice.service.storage.ClearingFileStorage;
import com.paymentprocessor.clearingservice.service.transport.ClearingTransport;
import com.paymentprocessor.clearingservice.service.transport.SubmissionReceipt;
import com.paymentprocessor.clearingservice.service.transport.TransportException;
import com.paymentprocessor.clearingservice.web.exception.ConflictException;
import com.paymentprocessor.clearingservice.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.paymentprocessor.clearingservice.service.TestFixtures.sampleBatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private ClearingBatchRepository batchRepository;
    @Mock
    private ClearingFileRepository fileRepository;
    @Mock
    private ClearingTransactionRepository txnRepository;
    @Mock
    private ClearingTransport transport;
    @Mock
    private ClearingFileStorage storage;
    @Mock
    private AuditService auditService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private RetryStrategy retryStrategy;

    private SubmissionService service;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(batchRepository, fileRepository, txnRepository,
                transport, storage, auditService, outboxService, retryStrategy);
    }

    @Test
    void submitByIdThrowsNotFoundForMissingBatch() {
        UUID id = UUID.randomUUID();
        when(batchRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void submitNowThrowsConflictWhenBatchNotValidated() {
        ClearingBatch batch = sampleBatch();
        UUID id = batch.getId();
        when(batchRepository.findById(id)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> service.submitNow(id))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submitSucceedsAndTransitionsToSent() {
        ClearingBatch batch = sampleBatch();
        batch.transitionTo(BatchStatus.VALIDATED);
        ClearingFile file = ClearingFile.of(batch.getId(), com.paymentprocessor.clearingservice.domain.enums.ClearingFormat.ISO8583,
                "REF.8583", "HASH", 100, 1, 1000L, "file:///x", "SHA256:HASH");

        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(fileRepository.findByBatchId(batch.getId())).thenReturn(Optional.of(file));
        when(storage.read(file.getStorageUri())).thenReturn(new byte[0]);
        when(transport.submit(any(), any(), any())).thenReturn(new SubmissionReceipt("REC", 200, "{}"));

        service.submitById(batch.getId());

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.SENT);
        assertThat(batch.getAckReference()).isEqualTo("REC");
    }

    @Test
    void submitRetriesWhenTransportFails() {
        ClearingBatch batch = sampleBatch();
        batch.transitionTo(BatchStatus.VALIDATED);
        ClearingFile file = ClearingFile.of(batch.getId(), com.paymentprocessor.clearingservice.domain.enums.ClearingFormat.ISO8583,
                "REF.8583", "HASH", 100, 1, 1000L, "file:///x", "SHA256:HASH");

        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(fileRepository.findByBatchId(batch.getId())).thenReturn(Optional.of(file));
        when(storage.read(file.getStorageUri())).thenReturn(new byte[0]);
        when(transport.submit(any(), any(), any()))
                .thenThrow(new TransportException("timeout", true));
        when(retryStrategy.maxAttempts()).thenReturn(5);
        when(retryStrategy.nextAttemptAt(anyInt())).thenReturn(Instant.now().plusSeconds(60));

        service.submitById(batch.getId());

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.VALIDATED);
        assertThat(batch.getNextAttemptAt()).isAfter(Instant.now());
    }
}
