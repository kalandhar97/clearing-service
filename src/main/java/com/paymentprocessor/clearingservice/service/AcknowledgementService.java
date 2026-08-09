package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingAcknowledgement;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.dto.AcknowledgementRequest;
import com.paymentprocessor.clearingservice.dto.AcknowledgementResponse;
import com.paymentprocessor.clearingservice.event.ClearingEventPayload;
import com.paymentprocessor.clearingservice.event.ClearingEvents;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.mapper.ClearingMapper;
import com.paymentprocessor.clearingservice.repository.ClearingAcknowledgementRepository;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.web.exception.ConflictException;
import com.paymentprocessor.clearingservice.web.exception.NotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Processes inbound acknowledgements from clearing participants. */
@Service
public class AcknowledgementService {

    private static final Logger log = LoggerFactory.getLogger(AcknowledgementService.class);
    private static final String ACTOR = "clearing-participant";

    private final ClearingBatchRepository batchRepository;
    private final ClearingTransactionRepository txnRepository;
    private final ClearingAcknowledgementRepository ackRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public AcknowledgementService(ClearingBatchRepository batchRepository,
                                  ClearingTransactionRepository txnRepository,
                                  ClearingAcknowledgementRepository ackRepository,
                                  AuditService auditService,
                                  OutboxService outboxService) {
        this.batchRepository = batchRepository;
        this.txnRepository = txnRepository;
        this.ackRepository = ackRepository;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public AcknowledgementResponse process(UUID batchId, AcknowledgementRequest request) {
        ClearingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));

        ClearingAcknowledgement ack = ClearingAcknowledgement.of(
                batch.getId(),
                request.ackReference(),
                request.status(),
                request.reasonCode(),
                request.message(),
                request.rawPayload());
        ackRepository.save(ack);

        if (request.ackReference() != null && !request.ackReference().isBlank()) {
            batch.setAckReference(request.ackReference());
        }

        auditService.record(AuditEntry.builder("ACK_RECEIVED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .reasonCode(request.reasonCode())
                .detail("Acknowledgement " + request.status()
                        + (request.message() != null ? ": " + request.message() : ""))
                .build());

        // Idempotency: if the batch already reached a terminal state, record the
        // acknowledgement but do not attempt a further transition.
        if (batch.getStatus().isTerminal()) {
            log.info("Acknowledgement {} for already-terminal batch {} ({}); recorded only",
                    request.status(), batch.getReference(), batch.getStatus());
            return ClearingMapper.toResponse(ack, batch.getStatus());
        }

        switch (request.status()) {
            case ACKNOWLEDGED -> moveToAcknowledged(batch);
            case ACCEPTED -> accept(batch);
            case PARTIAL -> partiallyAccept(batch, request);
            case REJECTED -> reject(batch, request);
        }

        batch = batchRepository.save(batch);
        return ClearingMapper.toResponse(ack, batch.getStatus());
    }

    private void moveToAcknowledged(ClearingBatch batch) {
        if (batch.getStatus() == BatchStatus.SENT) {
            batch.transitionTo(BatchStatus.ACKNOWLEDGED);
        } else if (batch.getStatus() != BatchStatus.ACKNOWLEDGED) {
            throw new ConflictException("Cannot acknowledge batch " + batch.getReference()
                    + " in state " + batch.getStatus());
        }
    }

    private void accept(ClearingBatch batch) {
        moveToAcknowledged(batch);
        batch.transitionTo(BatchStatus.COMPLETED);
        for (ClearingTransaction t : txnRepository.findByBatchId(batch.getId())) {
            t.markCleared();
            txnRepository.save(t);
        }
        auditService.record(AuditEntry.builder("ACCEPTED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .afterState(BatchStatus.COMPLETED.name())
                .build());
        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                ClearingEvents.CLEARING_ACCEPTED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_ACCEPTED, batch));
        log.info("Batch {} accepted and completed", batch.getReference());
    }

    private void partiallyAccept(ClearingBatch batch, AcknowledgementRequest request) {
        moveToAcknowledged(batch);
        Set<String> rejected = request.rejectedSourceTransactionIds() == null
                ? Set.of() : new HashSet<>(request.rejectedSourceTransactionIds());
        String reason = request.reasonCode() != null ? request.reasonCode() : "PARTIAL_REJECTION";

        int accepted = 0;
        int rejectedCount = 0;
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

        auditService.record(AuditEntry.builder("PARTIALLY_ACCEPTED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .reasonCode(reason)
                .afterState(BatchStatus.COMPLETED.name())
                .detail(accepted + " accepted, " + rejectedCount + " rejected")
                .build());
        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                ClearingEvents.CLEARING_ACCEPTED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_ACCEPTED, batch).withReason(reason));
        log.info("Batch {} partially accepted: {} accepted, {} rejected",
                batch.getReference(), accepted, rejectedCount);
    }

    private void reject(ClearingBatch batch, AcknowledgementRequest request) {
        BatchStatus before = batch.getStatus();
        if (before != BatchStatus.SENT && before != BatchStatus.ACKNOWLEDGED) {
            throw new ConflictException("Cannot reject batch " + batch.getReference()
                    + " in state " + before);
        }
        String reason = request.reasonCode() != null ? request.reasonCode() : "REJECTED";
        batch.setLastError(request.message());
        batch.transitionTo(BatchStatus.FAILED);
        for (ClearingTransaction t : txnRepository.findByBatchId(batch.getId())) {
            t.markRejected(reason);
            txnRepository.save(t);
        }
        auditService.record(AuditEntry.builder("REJECTED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .beforeState(before.name())
                .afterState(BatchStatus.FAILED.name())
                .reasonCode(reason)
                .detail(request.message())
                .build());
        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                ClearingEvents.CLEARING_REJECTED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_REJECTED, batch).withReason(reason));
        log.info("Batch {} rejected ({})", batch.getReference(), reason);
    }

    @Transactional(readOnly = true)
    public List<ClearingAcknowledgement> forBatch(UUID batchId) {
        return ackRepository.findByBatchIdOrderByReceivedAtDesc(batchId);
    }
}
