package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.dto.IngestTransactionRequest;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.web.exception.BusinessValidationException;
import com.paymentprocessor.clearingservice.web.exception.ConflictException;
import com.paymentprocessor.clearingservice.web.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.paymentprocessor.clearingservice.service.TestFixtures.sampleTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearingTransactionServiceTest {

    @Mock
    private ClearingTransactionRepository repository;
    @Mock
    private ClearingValidationService validationService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClearingTransactionService service;

    @Test
    void ingestReturnsExistingTransactionWhenIdempotentKeyExists() {
        ClearingTransaction existing = sampleTransaction();
        when(repository.findBySourceTransactionId("SRC-1")).thenReturn(Optional.of(existing));

        ClearingTransaction result = service.ingest(request());

        assertThat(result).isEqualTo(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void ingestSavesNewTransaction() {
        when(repository.findBySourceTransactionId("SRC-1")).thenReturn(Optional.empty());
        when(validationService.validate(any())).thenReturn(java.util.Collections.emptyList());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClearingTransaction result = service.ingest(request());

        assertThat(result.getStatus()).isEqualTo(ClearingTransactionStatus.PENDING);
        verify(repository).save(any(ClearingTransaction.class));
        verify(auditService).record(any());
    }

    @Test
    void ingestThrowsWhenValidationFails() {
        when(repository.findBySourceTransactionId("SRC-1")).thenReturn(Optional.empty());
        when(validationService.validate(any())).thenReturn(List.of("amountMinor must be positive"));

        assertThatThrownBy(() -> service.ingest(request()))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void cancelRejectsClearedTransactions() {
        ClearingTransaction txn = sampleTransaction();
        txn.markCleared();
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(txn));

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getByIdThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(NotFoundException.class);
    }

    private IngestTransactionRequest request() {
        return new IngestTransactionRequest(
                "SRC-1", "MERCHANT", com.paymentprocessor.clearingservice.domain.enums.Network.VISA,
                com.paymentprocessor.clearingservice.domain.enums.TransactionType.SALE, "USD", 1000L,
                null, LocalDate.now(), null, null, null, null, null);
    }
}
