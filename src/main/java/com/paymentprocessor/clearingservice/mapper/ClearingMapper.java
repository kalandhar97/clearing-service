package com.paymentprocessor.clearingservice.mapper;

import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingAcknowledgement;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.dto.AcknowledgementResponse;
import com.paymentprocessor.clearingservice.dto.AuditEntryResponse;
import com.paymentprocessor.clearingservice.dto.ClearingBatchResponse;
import com.paymentprocessor.clearingservice.dto.ClearingFileResponse;
import com.paymentprocessor.clearingservice.dto.ClearingTransactionResponse;

/** Pure, stateless mapping between domain entities and response DTOs. */
public final class ClearingMapper {

    private ClearingMapper() {
    }

    public static ClearingTransactionResponse toResponse(ClearingTransaction t) {
        return new ClearingTransactionResponse(
                t.getId(),
                t.getSourceTransactionId(),
                t.getMerchantId(),
                t.getNetwork(),
                t.getTransactionType(),
                t.getCurrency(),
                t.getAmountMinor(),
                t.getRegion(),
                t.getSettlementDate(),
                t.getMcc(),
                t.getAuthCode(),
                t.getArn(),
                t.getPanToken(),
                t.getCapturedAt(),
                t.getStatus(),
                t.getRejectionReason(),
                t.getBatchId(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    public static ClearingBatchResponse toResponse(ClearingBatch b) {
        return new ClearingBatchResponse(
                b.getId(),
                b.getReference(),
                b.getNetwork(),
                b.getCurrency(),
                b.getRegion(),
                b.getSettlementDate(),
                b.getFormat(),
                b.getStatus(),
                b.getTransactionCount(),
                b.getTotalAmountMinor(),
                b.getCutoffAt(),
                b.getSubmissionAttempts(),
                b.getNextAttemptAt(),
                b.getAckReference(),
                b.getLastError(),
                b.getCreatedAt(),
                b.getValidatedAt(),
                b.getSentAt(),
                b.getAcknowledgedAt(),
                b.getCompletedAt(),
                b.getFailedAt(),
                b.getUpdatedAt()
        );
    }

    public static ClearingFileResponse toResponse(ClearingFile f) {
        return new ClearingFileResponse(
                f.getId(),
                f.getBatchId(),
                f.getFormat(),
                f.getFileName(),
                f.getContentHash(),
                f.getSizeBytes(),
                f.getRecordCount(),
                f.getControlTotalMinor(),
                f.getStorageUri(),
                f.getSignature(),
                f.getGeneratedAt()
        );
    }

    public static AuditEntryResponse toResponse(AuditEntry a) {
        return new AuditEntryResponse(
                a.getId(),
                a.getBatchId(),
                a.getTransactionId(),
                a.getAction(),
                a.getActor(),
                a.getParticipantId(),
                a.getFileName(),
                a.getFileHash(),
                a.getReasonCode(),
                a.getBeforeState(),
                a.getAfterState(),
                a.getDetail(),
                a.getCreatedAt()
        );
    }

    public static AcknowledgementResponse toResponse(ClearingAcknowledgement a, BatchStatus resultingStatus) {
        return new AcknowledgementResponse(
                a.getId(),
                a.getBatchId(),
                a.getAckReference(),
                a.getStatus(),
                a.getReasonCode(),
                a.getMessage(),
                a.getReceivedAt(),
                resultingStatus
        );
    }
}
