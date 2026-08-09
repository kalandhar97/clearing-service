package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.config.ClearingProperties;
import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.event.ClearingEventPayload;
import com.paymentprocessor.clearingservice.event.ClearingEvents;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prepares a single clearing batch for one (network, currency, settlement date)
 * group: validates the pending transactions, creates the batch, generates the
 * clearing file, and moves the batch to VALIDATED. Runs in its own transaction
 * so one failing group does not roll back others.
 */
@Service
public class BatchPreparationService {

    private static final Logger log = LoggerFactory.getLogger(BatchPreparationService.class);
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ACTOR = "clearing-service";

    private final ClearingTransactionRepository txnRepository;
    private final ClearingBatchRepository batchRepository;
    private final ClearingValidationService validationService;
    private final FileGenerationService fileGenerationService;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final int maxBatchSize;

    public BatchPreparationService(ClearingTransactionRepository txnRepository,
                                   ClearingBatchRepository batchRepository,
                                   ClearingValidationService validationService,
                                   FileGenerationService fileGenerationService,
                                   AuditService auditService,
                                   OutboxService outboxService,
                                   ClearingProperties properties) {
        this.txnRepository = txnRepository;
        this.batchRepository = batchRepository;
        this.validationService = validationService;
        this.fileGenerationService = fileGenerationService;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.maxBatchSize = properties.batching().maxBatchSize();
    }

    @Transactional
    public Optional<ClearingBatch> prepareBatch(Network network, String currency, LocalDate settlementDate) {
        List<ClearingTransaction> pending = txnRepository.findPendingForGroup(
                network, currency, settlementDate, PageRequest.of(0, maxBatchSize));
        if (pending.isEmpty()) {
            return Optional.empty();
        }

        List<ClearingTransaction> valid = new ArrayList<>(pending.size());
        for (ClearingTransaction t : pending) {
            List<String> violations = validationService.validate(t);
            if (violations.isEmpty()) {
                valid.add(t);
            } else {
                String reason = String.join("; ", violations);
                t.markRejected(reason);
                txnRepository.save(t);
                auditService.record(AuditEntry.builder("VALIDATION_REJECTED", ACTOR)
                        .transactionId(t.getId())
                        .reasonCode("INVALID_FIELD")
                        .detail(reason)
                        .build());
            }
        }

        if (valid.isEmpty()) {
            return Optional.empty();
        }

        ClearingFormat format = network.defaultFormat();
        String reference = generateReference(network, currency, settlementDate);
        ClearingBatch batch = ClearingBatch.create(reference, network, currency, null,
                settlementDate, format, null);
        batch = batchRepository.save(batch);
        UUID batchId = batch.getId();

        long total = 0L;
        for (ClearingTransaction t : valid) {
            t.assignToBatch(batchId);
            total += t.getAmountMinor();
            txnRepository.save(t);
        }
        batch.recordTotals(valid.size(), total);

        auditService.record(AuditEntry.builder("BATCH_CREATED", ACTOR)
                .batchId(batchId)
                .participantId(network.name())
                .afterState(BatchStatus.CREATED.name())
                .detail("Formed batch " + reference + " with " + valid.size() + " transactions")
                .build());

        fileGenerationService.generate(batch, valid);

        batch.transitionTo(BatchStatus.VALIDATED);
        batch = batchRepository.save(batch);

        auditService.record(AuditEntry.builder("BATCH_VALIDATED", ACTOR)
                .batchId(batchId)
                .participantId(network.name())
                .beforeState(BatchStatus.CREATED.name())
                .afterState(BatchStatus.VALIDATED.name())
                .build());

        outboxService.append(ClearingEvents.AGGREGATE_BATCH, batchId.toString(),
                ClearingEvents.CLEARING_BATCH_CREATED,
                ClearingEventPayload.of(ClearingEvents.CLEARING_BATCH_CREATED, batch));

        log.info("Prepared batch {} ({} txns, total {} minor {})",
                reference, valid.size(), total, currency);
        return Optional.of(batch);
    }

    private String generateReference(Network network, String currency, LocalDate settlementDate) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CLR-" + settlementDate.format(REF_DATE) + "-" + network.name()
                + "-" + currency + "-" + suffix;
    }
}
