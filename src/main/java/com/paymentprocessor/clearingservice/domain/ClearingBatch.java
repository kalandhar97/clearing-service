package com.paymentprocessor.clearingservice.domain;

import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregate root for a batch of transactions prepared for a single clearing
 * participant and settlement date.
 */
@Entity
@Table(name = "clearing_batch")
public class ClearingBatch {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "reference", nullable = false, updatable = false, length = 64)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false, length = 32)
    private Network network;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "region", length = 32)
    private String region;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 16)
    private ClearingFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private BatchStatus status;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "total_amount_minor", nullable = false)
    private long totalAmountMinor;

    @Column(name = "cutoff_at")
    private Instant cutoffAt;

    @Column(name = "submission_attempts", nullable = false)
    private int submissionAttempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "ack_reference", length = 128)
    private String ackReference;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClearingBatch() {
        // for JPA
    }

    public static ClearingBatch create(String reference, Network network, String currency,
                                       String region, LocalDate settlementDate,
                                       ClearingFormat format, Instant cutoffAt) {
        ClearingBatch b = new ClearingBatch();
        b.id = UUID.randomUUID();
        b.reference = reference;
        b.network = network;
        b.currency = currency;
        b.region = region;
        b.settlementDate = settlementDate;
        b.format = format;
        b.status = BatchStatus.CREATED;
        b.transactionCount = 0;
        b.totalAmountMinor = 0L;
        b.cutoffAt = cutoffAt;
        b.submissionAttempts = 0;
        return b;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Applies a lifecycle transition, enforcing the batch state machine.
     *
     * @throws IllegalStateException if the transition is not allowed
     */
    public void transitionTo(BatchStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Illegal batch transition " + status + " -> " + target + " for batch " + reference);
        }
        Instant now = Instant.now();
        this.status = target;
        switch (target) {
            case VALIDATED -> this.validatedAt = now;
            case SENT -> this.sentAt = now;
            case ACKNOWLEDGED -> this.acknowledgedAt = now;
            case COMPLETED -> this.completedAt = now;
            case FAILED -> this.failedAt = now;
            default -> { /* no timestamp */ }
        }
    }

    public void recordTotals(int count, long totalMinor) {
        this.transactionCount = count;
        this.totalAmountMinor = totalMinor;
    }

    public void registerSubmissionAttempt() {
        this.submissionAttempts++;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public Network getNetwork() { return network; }
    public String getCurrency() { return currency; }
    public String getRegion() { return region; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public ClearingFormat getFormat() { return format; }
    public BatchStatus getStatus() { return status; }
    public int getTransactionCount() { return transactionCount; }
    public long getTotalAmountMinor() { return totalAmountMinor; }
    public Instant getCutoffAt() { return cutoffAt; }
    public int getSubmissionAttempts() { return submissionAttempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public String getAckReference() { return ackReference; }
    public void setAckReference(String ackReference) { this.ackReference = ackReference; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getValidatedAt() { return validatedAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getFailedAt() { return failedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
