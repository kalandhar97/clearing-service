package com.paymentprocessor.clearingservice.service.acknowledgement;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.dto.AcknowledgementRequest;
import com.paymentprocessor.clearingservice.event.ClearingEventPayload;
import com.paymentprocessor.clearingservice.event.ClearingEvents;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.web.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Handler for rejections: marks the batch and all its transactions as failed. */
@Component
public class RejectedHandler implements AcknowledgementHandler {

    private static final Logger log = LoggerFactory.getLogger(RejectedHandler.class);
    private static final String ACTOR = "clearing-participant";

    @Override
    public AckStatus supportedStatus() {
        return AckStatus.REJECTED;
    }

    @Override
    public void handle(ClearingBatch batch, AcknowledgementRequest request, AcknowledgementHandlerContext context) {
        BatchStatus before = batch.getStatus();
        if (before != BatchStatus.SENT && before != BatchStatus.ACKNOWLEDGED) {
            throw new ConflictException("Cannot reject batch " + batch.getReference()
                    + " in state " + before);
        }
        String reason = request.reasonCode() != null ? request.reasonCode() : "REJECTED";
        batch.setLastError(request.message());
        batch.transitionTo(BatchStatus.FAILED);

        ClearingTransactionRepository txnRepository = context.transactionRepository();
        for (ClearingTransaction t : txnRepository.findByBatchId(batch.getId())) {
            t.markRejected(reason);
            txnRepository.save(t);
        }

        AuditService auditService = context.auditService();
        auditService.record(AuditEntry.builder("REJECTED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .beforeState(before.name())
                .afterState(BatchStatus.FAILED.name())
                .reasonCode(reason)
                .detail(request.message())
                .build());

        OutboxService outboxService = context.outboxService();
        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                ClearingEvents.CLEARING_REJECTED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_REJECTED, batch).withReason(reason));

        log.info("Batch {} rejected ({})", batch.getReference(), reason);
    }
}
