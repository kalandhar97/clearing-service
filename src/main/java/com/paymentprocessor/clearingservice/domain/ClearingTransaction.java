package com.paymentprocessor.clearingservice.domain;

import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
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
 * An individual transaction eligible for clearing. {@code sourceTransactionId}
 * is the idempotency key provided by the Payment Service.
 */
@Entity
@Table(name = "clearing_transaction")
public class ClearingTransaction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "source_transaction_id", nullable = false, updatable = false, length = 64)
    private String sourceTransactionId;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false, length = 32)
    private Network network;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 24)
    private TransactionType transactionType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "region", length = 32)
    private String region;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "mcc", length = 4)
    private String mcc;

    @Column(name = "auth_code", length = 16)
    private String authCode;

    @Column(name = "arn", length = 32)
    private String arn;

    @Column(name = "pan_token", length = 64)
    private String panToken;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ClearingTransactionStatus status;

    @Column(name = "rejection_reason", length = 512)
    private String rejectionReason;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClearingTransaction() {
        // for JPA
    }

    public static ClearingTransaction ingest(String sourceTransactionId, String merchantId,
                                             Network network, TransactionType transactionType,
                                             String currency, long amountMinor, String region,
                                             LocalDate settlementDate, String mcc, String authCode,
                                             String arn, String panToken, Instant capturedAt) {
        ClearingTransaction t = new ClearingTransaction();
        t.id = UUID.randomUUID();
        t.sourceTransactionId = sourceTransactionId;
        t.merchantId = merchantId;
        t.network = network;
        t.transactionType = transactionType;
        t.currency = currency;
        t.amountMinor = amountMinor;
        t.region = region;
        t.settlementDate = settlementDate;
        t.mcc = mcc;
        t.authCode = authCode;
        t.arn = arn;
        t.panToken = panToken;
        t.capturedAt = capturedAt;
        t.status = ClearingTransactionStatus.PENDING;
        return t;
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

    public void assignToBatch(UUID batchId) {
        this.batchId = batchId;
        this.status = ClearingTransactionStatus.BATCHED;
        this.rejectionReason = null;
    }

    public void markCleared() {
        this.status = ClearingTransactionStatus.CLEARED;
    }

    public void markRejected(String reason) {
        this.status = ClearingTransactionStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void markFailed(String reason) {
        this.status = ClearingTransactionStatus.FAILED;
        this.rejectionReason = reason;
    }

    /** Detach from its batch and return to the pending pool for re-batching. */
    public void returnToPending() {
        this.batchId = null;
        this.status = ClearingTransactionStatus.PENDING;
    }

    public UUID getId() { return id; }
    public String getSourceTransactionId() { return sourceTransactionId; }
    public String getMerchantId() { return merchantId; }
    public Network getNetwork() { return network; }
    public TransactionType getTransactionType() { return transactionType; }
    public String getCurrency() { return currency; }
    public long getAmountMinor() { return amountMinor; }
    public String getRegion() { return region; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public String getMcc() { return mcc; }
    public String getAuthCode() { return authCode; }
    public String getArn() { return arn; }
    public String getPanToken() { return panToken; }
    public Instant getCapturedAt() { return capturedAt; }
    public ClearingTransactionStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getBatchId() { return batchId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
