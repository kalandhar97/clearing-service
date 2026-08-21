package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingAcknowledgement;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.dto.AcknowledgementRequest;
import com.paymentprocessor.clearingservice.dto.AcknowledgementResponse;
import com.paymentprocessor.clearingservice.mapper.ClearingMapper;
import com.paymentprocessor.clearingservice.repository.ClearingAcknowledgementRepository;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.service.acknowledgement.AcknowledgementHandlerContext;
import com.paymentprocessor.clearingservice.service.acknowledgement.AcknowledgementHandlerRegistry;
import com.paymentprocessor.clearingservice.event.OutboxService;
import com.paymentprocessor.clearingservice.web.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Processes inbound acknowledgements from clearing participants. */
@Service
public class AcknowledgementService {

    private static final Logger log = LoggerFactory.getLogger(AcknowledgementService.class);
    private static final String ACTOR = "clearing-participant";

    private final ClearingBatchRepository batchRepository;
    private final ClearingAcknowledgementRepository ackRepository;
    private final AcknowledgementHandlerRegistry handlerRegistry;
    private final AcknowledgementHandlerContext handlerContext;

    public AcknowledgementService(ClearingBatchRepository batchRepository,
                                  ClearingAcknowledgementRepository ackRepository,
                                  ClearingTransactionRepository txnRepository,
                                  AuditService auditService,
                                  OutboxService outboxService,
                                  AcknowledgementHandlerRegistry handlerRegistry) {
        this.batchRepository = batchRepository;
        this.ackRepository = ackRepository;
        this.handlerRegistry = handlerRegistry;
        this.handlerContext = new AcknowledgementHandlerContext(
                batchRepository, txnRepository, auditService, outboxService);
    }

    @Transactional
    public AcknowledgementResponse process(UUID batchId, AcknowledgementRequest request) {
        ClearingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));

        ClearingAcknowledgement ack = ClearingAcknowledgement.of(
                batch.getId(),
                request.ackReference(),
                request.status(),
                request.reasonCode(),
                request.message(),
                request.rawPayload());
        ackRepository.save(ack);

        if (request.ackReference() != null && !request.ackReference().isBlank()) {
            batch.setAckReference(request.ackReference());
        }

        auditService().record(AuditEntry.builder("ACK_RECEIVED", ACTOR)
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .reasonCode(request.reasonCode())
                .detail("Acknowledgement " + request.status()
                        + (request.message() != null ? ": " + request.message() : ""))
                .build());

        if (batch.getStatus().isTerminal()) {
            log.info("Acknowledgement {} for already-terminal batch {} ({}); recorded only",
                    request.status(), batch.getReference(), batch.getStatus());
            batch = batchRepository.save(batch);
            return ClearingMapper.toResponse(ack, batch.getStatus());
        }

        handlerRegistry.get(request.status()).handle(batch, request, handlerContext);
        batch = batchRepository.save(batch);

        log.info("Batch {} processed acknowledgement {}", batch.getReference(), request.status());
        return ClearingMapper.toResponse(ack, batch.getStatus());
    }

    private AuditService auditService() {
        return handlerContext.auditService();
    }

    @Transactional(readOnly = true)
    public List<ClearingAcknowledgement> forBatch(UUID batchId) {
        return ackRepository.findByBatchIdOrderByReceivedAtDesc(batchId);
    }
}
