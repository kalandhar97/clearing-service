package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.dto.IngestTransactionRequest;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.web.exception.BusinessValidationException;
import com.paymentprocessor.clearingservice.web.exception.ConflictException;
import com.paymentprocessor.clearingservice.web.exception.NotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ingestion and querying of transactions eligible for clearing. */
@Service
public class ClearingTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ClearingTransactionService.class);
    private static final String ACTOR = "clearing-service";

    private final ClearingTransactionRepository repository;
    private final ClearingValidationService validationService;
    private final AuditService auditService;

    public ClearingTransactionService(ClearingTransactionRepository repository,
                                      ClearingValidationService validationService,
                                      AuditService auditService) {
        this.repository = repository;
        this.validationService = validationService;
        this.auditService = auditService;
    }

    /**
     * Ingests a transaction for clearing. Idempotent on
     * {@code sourceTransactionId}: a repeated submission returns the existing
     * record rather than creating a duplicate.
     */
    @Transactional
    public ClearingTransaction ingest(IngestTransactionRequest request) {
        String sourceId = request.sourceTransactionId();
        var existing = repository.findBySourceTransactionId(sourceId);
        if (existing.isPresent()) {
            log.debug("Idempotent ingest: transaction {} already exists", sourceId);
            return existing.get();
        }

        String currency = request.currency() == null ? null
                : request.currency().toUpperCase(Locale.ROOT);

        ClearingTransaction txn = ClearingTransaction.ingest(
                sourceId,
                request.merchantId(),
                request.network(),
                request.transactionType(),
                currency,
                request.amountMinor(),
                request.region(),
                request.settlementDate(),
                request.mcc(),
                request.authCode(),
                request.arn(),
                request.panToken(),
                request.capturedAt());

        List<String> violations = validationService.validate(txn);
        if (!violations.isEmpty()) {
            throw new BusinessValidationException("Transaction failed clearing validation", violations);
        }

        // The unique constraint on source_transaction_id is the final guard: a
        // concurrent duplicate insert surfaces as DataIntegrityViolationException,
        // which the global handler maps to HTTP 409 (idempotency conflict).
        txn = repository.save(txn);

        auditService.record(AuditEntry.builder("TRANSACTION_INGESTED", ACTOR)
                .transactionId(txn.getId())
                .participantId(txn.getNetwork().name())
                .afterState(ClearingTransactionStatus.PENDING.name())
                .detail("Ingested source transaction " + sourceId)
                .build());

        log.info("Ingested transaction {} for {} {} {}",
                sourceId, txn.getNetwork(), txn.getAmountMinor(), txn.getCurrency());
        return txn;
    }

    @Transactional(readOnly = true)
    public ClearingTransaction getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public ClearingTransaction getBySourceId(String sourceTransactionId) {
        return repository.findBySourceTransactionId(sourceTransactionId)
                .orElseThrow(() -> new NotFoundException(
                        "Transaction not found for source id: " + sourceTransactionId));
    }

    @Transactional(readOnly = true)
    public Page<ClearingTransaction> list(ClearingTransactionStatus status, Pageable pageable) {
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        return repository.findAll(pageable);
    }

    /** Cancels a transaction that has not yet been cleared. */
    @Transactional
    public ClearingTransaction cancel(UUID id) {
        ClearingTransaction txn = getById(id);
        if (txn.getStatus() == ClearingTransactionStatus.CLEARED) {
            throw new ConflictException("Cannot cancel a cleared transaction: " + id);
        }
        txn.markFailed("CANCELLED");
        txn = repository.save(txn);
        auditService.record(AuditEntry.builder("TRANSACTION_CANCELLED", ACTOR)
                .transactionId(id)
                .afterState(ClearingTransactionStatus.FAILED.name())
                .reasonCode("CANCELLED")
                .build());
        return txn;
    }
}
