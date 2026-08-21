package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import com.paymentprocessor.clearingservice.repository.ClearingFileRepository;
import com.paymentprocessor.clearingservice.service.format.ClearingMessageFormatter;
import com.paymentprocessor.clearingservice.service.format.FormatterRegistry;
import com.paymentprocessor.clearingservice.service.storage.ClearingFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileGenerationServiceTest {

    @Mock
    private FormatterRegistry formatterRegistry;
    @Mock
    private ClearingFileStorage storage;
    @Mock
    private ClearingFileRepository fileRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private FileGenerationService service;

    @Test
    void generateStoresFileAndReturnsMetadata() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.of(2026, 8, 21), ClearingFormat.ISO8583, null);
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 1000, null, LocalDate.of(2026, 8, 21),
                null, null, null, null, null);
        ClearingMessageFormatter formatter = mock(ClearingMessageFormatter.class);

        when(formatterRegistry.get(ClearingFormat.ISO8583)).thenReturn(formatter);
        when(formatter.format(batch, List.of(txn))).thenReturn("content".getBytes(StandardCharsets.UTF_8));
        when(formatter.fileExtension()).thenReturn("8583");
        when(storage.store(any(), any())).thenReturn("file:///tmp/REF.8583");
        when(fileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClearingFile file = service.generate(batch, List.of(txn));

        assertThat(file.getBatchId()).isEqualTo(batch.getId());
        assertThat(file.getFileName()).isEqualTo("REF.8583");
        assertThat(file.getContentHash()).hasSize(64);
        assertThat(file.getControlTotalMinor()).isEqualTo(1000L);
        verify(auditService).record(any());
    }
}
