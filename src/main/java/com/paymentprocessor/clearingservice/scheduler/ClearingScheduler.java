package com.paymentprocessor.clearingservice.scheduler;

import com.paymentprocessor.clearingservice.event.OutboxPublisher;
import com.paymentprocessor.clearingservice.service.BatchingService;
import com.paymentprocessor.clearingservice.service.SubmissionService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the clearing pipeline on a schedule: forms batches from pending
 * transactions, submits validated batches (with durable retry), and relays
 * outbox events to Kafka. Each unit of work runs in its own service-level
 * transaction so failures are isolated.
 */
@Component
public class ClearingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClearingScheduler.class);
    private static final int SUBMISSION_SWEEP_LIMIT = 200;

    private final BatchingService batchingService;
    private final SubmissionService submissionService;
    private final OutboxPublisher outboxPublisher;

    public ClearingScheduler(BatchingService batchingService,
                             SubmissionService submissionService,
                             OutboxPublisher outboxPublisher) {
        this.batchingService = batchingService;
        this.submissionService = submissionService;
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(cron = "${clearing.batching.formation-cron}")
    public void formBatches() {
        try {
            batchingService.formBatches();
        } catch (RuntimeException e) {
            log.error("Batch formation run failed", e);
        }
    }

    @Scheduled(cron = "${clearing.submission.sweep-cron}")
    public void submitDueBatches() {
        List<UUID> due;
        try {
            due = submissionService.findDueBatchIds(SUBMISSION_SWEEP_LIMIT);
        } catch (RuntimeException e) {
            log.error("Failed to load submittable batches", e);
            return;
        }
        for (UUID id : due) {
            try {
                submissionService.submitById(id);
            } catch (RuntimeException e) {
                log.error("Submission run failed for batch {}", id, e);
            }
        }
    }

    @Scheduled(cron = "${clearing.outbox.publish-cron}")
    public void publishOutbox() {
        try {
            outboxPublisher.publishPending();
        } catch (RuntimeException e) {
            log.error("Outbox publish run failed", e);
        }
    }
}
