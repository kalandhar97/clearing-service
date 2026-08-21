package com.paymentprocessor.clearingservice.service.acknowledgement;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;

/**
 * Shared dependencies passed to acknowledgement handlers so each handler remains
 * a pure strategy that operates on the batch/request without wiring its own
 * collaborators.
 */
public record AcknowledgementHandlerContext(
        ClearingBatchRepository batchRepository,
        ClearingTransactionRepository transactionRepository,
        AuditService auditService,
        OutboxService outboxService
) {
}
