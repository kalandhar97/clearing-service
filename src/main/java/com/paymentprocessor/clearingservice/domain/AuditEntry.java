package com.paymentprocessor.clearingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Immutable audit record of a clearing lifecycle action. */
@Entity
@Table(name = "audit_entry")
public class AuditEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "action", nullable = false, length = 48)
    private String action;

    @Column(name = "actor", nullable = false, length = 64)
    private String actor;

    @Column(name = "participant_id", length = 64)
    private String participantId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(name = "before_state", length = 24)
    private String beforeState;

    @Column(name = "after_state", length = 24)
    private String afterState;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEntry() {
        // for JPA
    }

    AuditEntry(UUID batchId, UUID transactionId, String action, String actor, String participantId,
               String fileName, String fileHash, String reasonCode, String beforeState,
               String afterState, String detail) {
        this.id = UUID.randomUUID();
        this.batchId = batchId;
        this.transactionId = transactionId;
        this.action = action;
        this.actor = actor;
        this.participantId = participantId;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.reasonCode = reasonCode;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.detail = detail;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static Builder builder(String action, String actor) {
        return new Builder(action, actor);
    }

    /** Fluent builder for audit entries; only action and actor are required. */
    public static final class Builder {
        private final String action;
        private final String actor;
        private UUID batchId;
        private UUID transactionId;
        private String participantId;
        private String fileName;
        private String fileHash;
        private String reasonCode;
        private String beforeState;
        private String afterState;
        private String detail;

        private Builder(String action, String actor) {
            this.action = action;
            this.actor = actor;
        }

        public Builder batchId(UUID v) { this.batchId = v; return this; }
        public Builder transactionId(UUID v) { this.transactionId = v; return this; }
        public Builder participantId(String v) { this.participantId = v; return this; }
        public Builder fileName(String v) { this.fileName = v; return this; }
        public Builder fileHash(String v) { this.fileHash = v; return this; }
        public Builder reasonCode(String v) { this.reasonCode = v; return this; }
        public Builder beforeState(String v) { this.beforeState = v; return this; }
        public Builder afterState(String v) { this.afterState = v; return this; }
        public Builder detail(String v) { this.detail = v; return this; }

        public AuditEntry build() {
            return new AuditEntry(batchId, transactionId, action, actor, participantId,
                    fileName, fileHash, reasonCode, beforeState, afterState, detail);
        }
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public UUID getTransactionId() { return transactionId; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public String getParticipantId() { return participantId; }
    public String getFileName() { return fileName; }
    public String getFileHash() { return fileHash; }
    public String getReasonCode() { return reasonCode; }
    public String getBeforeState() { return beforeState; }
    public String getAfterState() { return afterState; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
