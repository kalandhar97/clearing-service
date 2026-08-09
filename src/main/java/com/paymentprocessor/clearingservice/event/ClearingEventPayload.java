package com.paymentprocessor.clearingservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import java.time.Instant;

/**
 * Serialized payload for clearing domain events. Null fields are omitted, so the
 * same shape carries created/submitted/accepted/rejected events.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClearingEventPayload(
        String eventType,
        String batchId,
        String reference,
        String network,
        String currency,
        String settlementDate,
        String format,
        Integer transactionCount,
        Long totalAmountMinor,
        String status,
        String ackReference,
        String reasonCode,
        Instant occurredAt
) {

    public static ClearingEventPayload of(String eventType, ClearingBatch b) {
        return new ClearingEventPayload(
                eventType,
                b.getId().toString(),
                b.getReference(),
                b.getNetwork().name(),
                b.getCurrency(),
                b.getSettlementDate().toString(),
                b.getFormat().name(),
                b.getTransactionCount(),
                b.getTotalAmountMinor(),
                b.getStatus().name(),
                b.getAckReference(),
                null,
                Instant.now());
    }

    public ClearingEventPayload withReason(String reasonCode) {
        return new ClearingEventPayload(eventType, batchId, reference, network, currency,
                settlementDate, format, transactionCount, totalAmountMinor, status,
                ackReference, reasonCode, occurredAt);
    }
}
