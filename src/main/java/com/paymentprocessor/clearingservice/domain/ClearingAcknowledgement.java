package com.paymentprocessor.clearingservice.domain;

import com.paymentprocessor.clearingservice.domain.enums.AckStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** An inbound acknowledgement (or rejection) received for a submitted batch. */
@Entity
@Table(name = "clearing_acknowledgement")
public class ClearingAcknowledgement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "ack_reference", length = 128)
    private String ackReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private AckStatus status;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(name = "message", length = 1024)
    private String message;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected ClearingAcknowledgement() {
        // for JPA
    }

    public static ClearingAcknowledgement of(UUID batchId, String ackReference, AckStatus status,
                                             String reasonCode, String message, String rawPayload) {
        return builder()
                .batchId(batchId)
                .ackReference(ackReference)
                .status(status)
                .reasonCode(reasonCode)
                .message(message)
                .rawPayload(rawPayload)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link ClearingAcknowledgement}. */
    public static final class Builder {
        private UUID batchId;
        private String ackReference;
        private AckStatus status;
        private String reasonCode;
        private String message;
        private String rawPayload;

        public Builder batchId(UUID v) { this.batchId = v; return this; }
        public Builder ackReference(String v) { this.ackReference = v; return this; }
        public Builder status(AckStatus v) { this.status = v; return this; }
        public Builder reasonCode(String v) { this.reasonCode = v; return this; }
        public Builder message(String v) { this.message = v; return this; }
        public Builder rawPayload(String v) { this.rawPayload = v; return this; }

        public ClearingAcknowledgement build() {
            ClearingAcknowledgement a = new ClearingAcknowledgement();
            a.id = UUID.randomUUID();
            a.batchId = batchId;
            a.ackReference = ackReference;
            a.status = status;
            a.reasonCode = reasonCode;
            a.message = message;
            a.rawPayload = rawPayload;
            return a;
        }
    }

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public String getAckReference() { return ackReference; }
    public AckStatus getStatus() { return status; }
    public String getReasonCode() { return reasonCode; }
    public String getMessage() { return message; }
    public String getRawPayload() { return rawPayload; }
    public Instant getReceivedAt() { return receivedAt; }
}
