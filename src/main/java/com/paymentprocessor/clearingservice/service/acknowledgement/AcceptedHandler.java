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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Handler for full acceptances: moves the batch to COMPLETED and clears all transactions. */
@Component
public class AcceptedHandler extends AcknowledgedHandler {

    private static final Logger log = LoggerFactory.getLogger(AcceptedHandler.class);
    private static final String ACTOR = "clearing-participant";

    @Override
    public AckStatus supportedStatus() {
        return AckStatus.ACCEPTED;
    }

    @Override
    public void handle(ClearingBatch batch, AcknowledgementRequest request, AcknowledgementHandlerContext context) {
        super.handle(batch, request, context);
        batch.transitionTo(BatchStatus.COMPLETED);

        ClearingTransactionRepository txnRepository = context.transactionRepository();
        for (ClearingTransaction t : txnRepository.findByBatchId(batch.getId())) {
            t.markCleared();
            txnRepository.save(t);
        }

        AuditService auditService = context.auditService();
        auditService.record(AuditEntry.builder("ACCEPTED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .afterState(BatchStatus.COMPLETED.name())
                .build());

        OutboxService outboxService = context.outboxService();
        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                ClearingEvents.CLEARING_ACCEPTED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_ACCEPTED, batch));

        log.info("Batch {} accepted and completed", batch.getReference());
    }
}
