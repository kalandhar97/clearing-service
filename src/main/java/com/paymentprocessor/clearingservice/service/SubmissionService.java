package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.config.ClearingProperties;
import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.event.ClearingEventPayload;
import com.paymentprocessor.clearingservice.event.ClearingEvents;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingFileRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.service.storage.ClearingFileStorage;
import com.paymentprocessor.clearingservice.service.transport.ClearingTransport;
import com.paymentprocessor.clearingservice.service.transport.SubmissionReceipt;
import com.paymentprocessor.clearingservice.service.transport.TransportException;
import com.paymentprocessor.clearingservice.web.exception.ConflictException;
import com.paymentprocessor.clearingservice.web.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Submits validated batches to the external clearing application and manages
 * durable retry/backoff. Retry state (attempt count and next attempt time) is
 * persisted on the batch, so retries survive restarts and horizontal scaling.
 */
@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);
    private static final String ACTOR = "clearing-service";
    private static final int MAX_ERROR_LEN = 1024;

    private final ClearingBatchRepository batchRepository;
    private final ClearingFileRepository fileRepository;
    private final ClearingTransactionRepository txnRepository;
    private final ClearingTransport transport;
    private final ClearingFileStorage storage;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final int maxAttempts;
    private final long backoffInitialMs;
    private final double backoffMultiplier;

    public SubmissionService(ClearingBatchRepository batchRepository,
                             ClearingFileRepository fileRepository,
                             ClearingTransactionRepository txnRepository,
                             ClearingTransport transport,
                             ClearingFileStorage storage,
                             AuditService auditService,
                             OutboxService outboxService,
                             ClearingProperties properties) {
        this.batchRepository = batchRepository;
        this.fileRepository = fileRepository;
        this.txnRepository = txnRepository;
        this.transport = transport;
        this.storage = storage;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.maxAttempts = properties.submission().maxAttempts();
        this.backoffInitialMs = properties.submission().backoffInitialMs();
        this.backoffMultiplier = properties.submission().backoffMultiplier();
    }

    @Transactional(readOnly = true)
    public List<UUID> findDueBatchIds(int limit) {
        return batchRepository.findSubmittable(Instant.now(), PageRequest.of(0, limit))
                .stream().map(ClearingBatch::getId).toList();
    }

    /** Loads and submits a single batch in its own transaction. */
    @Transactional
    public void submitById(UUID batchId) {
        ClearingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));
        submit(batch);
    }

    /** Manually triggers submission of a batch; only VALIDATED batches are eligible. */
    @Transactional
    public ClearingBatch submitNow(UUID batchId) {
        ClearingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));
        if (batch.getStatus() != BatchStatus.VALIDATED) {
            throw new ConflictException("Batch " + batch.getReference()
                    + " is not in VALIDATED state (current: " + batch.getStatus() + ")");
        }
        submit(batch);
        return batch;
    }

    private void submit(ClearingBatch batch) {
        if (batch.getStatus() != BatchStatus.VALIDATED) {
            log.debug("Skipping submission of batch {} in state {}",
                    batch.getReference(), batch.getStatus());
            return;
        }
        ClearingFile file = fileRepository.findByBatchId(batch.getId()).orElse(null);
        if (file == null) {
            failTerminally(batch, "No clearing file found for batch", "MISSING_FILE");
            return;
        }

        byte[] content = storage.read(file.getStorageUri());
        batch.registerSubmissionAttempt();

        try {
            SubmissionReceipt receipt = transport.submit(batch, file, content);
            batch.setAckReference(receipt.receiptReference());
            batch.setLastError(null);
            batch.setNextAttemptAt(null);
            batch.transitionTo(BatchStatus.SENT);
            batchRepository.save(batch);

            auditService.record(AuditEntry.builder("SUBMITTED", ACTOR)
                    .batchId(batch.getId())
                    .participantId(batch.getNetwork().name())
                    .fileName(file.getFileName())
                    .fileHash(file.getContentHash())
                    .beforeState(BatchStatus.VALIDATED.name())
                    .afterState(BatchStatus.SENT.name())
                    .detail("Submission receipt " + receipt.receiptReference())
                    .build());

            outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                    ClearingEvents.CLEARING_SUBMITTED,
                    ClearingEventPayload.of(ClearingEvents.CLEARING_SUBMITTED, batch));

            log.info("Batch {} submitted (attempt {})", batch.getReference(), batch.getSubmissionAttempts());

        } catch (TransportException e) {
            handleFailure(batch, file, e.getMessage(), e.isRetryable());
        }
    }

    private void handleFailure(ClearingBatch batch, ClearingFile file, String message, boolean retryable) {
        batch.setLastError(truncate(message));
        boolean attemptsExhausted = batch.getSubmissionAttempts() >= maxAttempts;

        if (!retryable || attemptsExhausted) {
            failTerminally(batch, message, retryable ? "MAX_RETRIES_EXCEEDED" : "NON_RETRYABLE");
            return;
        }

        long delayMs = computeBackoffMs(batch.getSubmissionAttempts());
        batch.setNextAttemptAt(Instant.now().plusMillis(delayMs));
        batchRepository.save(batch);

        auditService.record(AuditEntry.builder("SUBMISSION_RETRY_SCHEDULED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .reasonCode("TRANSIENT")
                .detail("Attempt " + batch.getSubmissionAttempts() + " failed; retrying in "
                        + delayMs + "ms. " + truncate(message))
                .build());

        log.warn("Batch {} submission attempt {} failed (retryable); next attempt in {}ms: {}",
                batch.getReference(), batch.getSubmissionAttempts(), delayMs, message);
    }

    private void failTerminally(ClearingBatch batch, String message, String reasonCode) {
        BatchStatus before = batch.getStatus();
        batch.setLastError(truncate(message));
        batch.setNextAttemptAt(null);
        batch.transitionTo(BatchStatus.FAILED);
        batchRepository.save(batch);

        // Mark the batch's transactions as failed for manual intervention.
        List<ClearingTransaction> txns = txnRepository.findByBatchId(batch.getId());
        for (ClearingTransaction t : txns) {
            t.markFailed(reasonCode + ": " + truncate(message));
            txnRepository.save(t);
        }

        auditService.record(AuditEntry.builder("SUBMISSION_FAILED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .beforeState(before.name())
                .afterState(BatchStatus.FAILED.name())
                .reasonCode(reasonCode)
                .detail(truncate(message))
                .build());

        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batch.getId().toString(),
                ClearingEvents.CLEARING_REJECTED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_REJECTED, batch).withReason(reasonCode));

        log.error("Batch {} failed permanently ({}): {}", batch.getReference(), reasonCode, message);
    }

    private long computeBackoffMs(int attempt) {
        double factor = Math.pow(backoffMultiplier, Math.max(0, attempt - 1));
        return (long) (backoffInitialMs * factor);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_ERROR_LEN ? s : s.substring(0, MAX_ERROR_LEN);
    }
}
