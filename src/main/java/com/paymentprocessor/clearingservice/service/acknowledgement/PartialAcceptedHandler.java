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
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for partial acceptances: rejects the listed transactions and clears the
 * remainder, then completes the batch.
 */
@Component
public class PartialAcceptedHandler extends AcknowledgedHandler {

    private static final Logger log = LoggerFactory.getLogger(PartialAcceptedHandler.class);
    private static final String ACTOR = "clearing-participant";

    @Override
    public AckStatus supportedStatus() {
        return AckStatus.PARTIAL;
    }

    @Override
    public void handle(ClearingBatch batch, AcknowledgementRequest request, AcknowledgementHandlerContext context) {
        super.handle(batch, request, context);

        Set<String> rejected = request.rejectedSourceTransactionIds() == null
                ? Set.of() : new HashSet<>(request.rejectedSourceTransactionIds());
        String reason = request.reasonCode() != null ? request.reasonCode() : "PARTIAL_REJECTION";

        int accepted = 0;
        int rejectedCount = 0;
        ClearingTransactionRepository txnRepository = context.transactionRepository();
        for (ClearingTransaction t : txnRepository.findByBatchId(batch.getId())) {
            if (rejected.contains(t.getSourceTransactionId())) {
                t.markRejected(reason);
                rejectedCount++;
            } else {
                t.markCleared();
                accepted++;
            }
            txnRepository.save(t);
        }
        batch.transitionTo(BatchStatus.COMPLETED);

        AuditService auditService = context.auditService();
        auditService.record(AuditEntry.builder("PARTIALLY_ACCEPTED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .reasonCode(reason)
                .afterState(BatchStatus.COMPLETED.name())
                .detail(accepted + " accepted, " + rejectedCount + " rejected")
                .build());

        OutboxService outboxService = context.outboxService();
        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                ClearingEvents.CLEARING_ACCEPTED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_ACCEPTED, batch).withReason(reason));

        log.info("Batch {} partially accepted: {} accepted, {} rejected",
                batch.getReference(), accepted, rejectedCount);
    }
}
