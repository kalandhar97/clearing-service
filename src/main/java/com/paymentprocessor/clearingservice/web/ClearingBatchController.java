package com.paymentprocessor.clearingservice.web;

import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.dto.AuditEntryResponse;
import com.paymentprocessor.clearingservice.dto.BatchFormationResponse;
import com.paymentprocessor.clearingservice.dto.ClearingBatchResponse;
import com.paymentprocessor.clearingservice.dto.ClearingFileResponse;
import com.paymentprocessor.clearingservice.dto.ClearingTransactionResponse;
import com.paymentprocessor.clearingservice.dto.PageResponse;
import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.mapper.ClearingMapper;
import com.paymentprocessor.clearingservice.service.BatchingService;
import com.paymentprocessor.clearingservice.service.ClearingBatchService;
import com.paymentprocessor.clearingservice.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clearing/batches")
@Tag(name = "Clearing Batches", description = "Formation, submission and status of clearing batches")
public class ClearingBatchController {

    private static final int MAX_PAGE_SIZE = 200;

    private final ClearingBatchService batchService;
    private final BatchingService batchingService;
    private final SubmissionService submissionService;
    private final AuditService auditService;

    public ClearingBatchController(ClearingBatchService batchService,
                                   BatchingService batchingService,
                                   SubmissionService submissionService,
                                   AuditService auditService) {
        this.batchService = batchService;
        this.batchingService = batchingService;
        this.submissionService = submissionService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List clearing batches, optionally filtered by status or network")
    public PageResponse<ClearingBatchResponse> list(
            @RequestParam(required = false) BatchStatus status,
            @RequestParam(required = false) Network network,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(batchService.list(status, network, pageable), ClearingMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a batch by id")
    public ClearingBatchResponse getById(@PathVariable UUID id) {
        return ClearingMapper.toResponse(batchService.getById(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get a batch by its reference")
    public ClearingBatchResponse getByReference(@PathVariable String reference) {
        return ClearingMapper.toResponse(batchService.getByReference(reference));
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Get the generated clearing file metadata for a batch")
    public ClearingFileResponse getFile(@PathVariable UUID id) {
        return ClearingMapper.toResponse(batchService.getFile(id));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "List the transactions in a batch")
    public List<ClearingTransactionResponse> getTransactions(@PathVariable UUID id) {
        return batchService.getTransactions(id).stream().map(ClearingMapper::toResponse).toList();
    }

    @GetMapping("/{id}/rejections")
    @Operation(summary = "List rejected transactions in a batch")
    public List<ClearingTransactionResponse> getRejections(@PathVariable UUID id) {
        return batchService.getRejections(id).stream().map(ClearingMapper::toResponse).toList();
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Get the audit trail for a batch")
    public List<AuditEntryResponse> getAudit(@PathVariable UUID id) {
        batchService.getById(id); // 404 if the batch does not exist
        return auditService.forBatch(id).stream().map(ClearingMapper::toResponse).toList();
    }

    @PostMapping("/form")
    @Operation(summary = "Trigger formation of clearing batches from pending transactions")
    public BatchFormationResponse form() {
        return batchingService.formBatches();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Manually submit a VALIDATED batch to the external clearing application")
    public ClearingBatchResponse submit(@PathVariable UUID id) {
        return ClearingMapper.toResponse(submissionService.submitNow(id));
    }
}
