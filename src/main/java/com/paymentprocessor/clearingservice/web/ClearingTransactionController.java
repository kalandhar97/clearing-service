package com.paymentprocessor.clearingservice.web;

import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.dto.ClearingTransactionResponse;
import com.paymentprocessor.clearingservice.dto.IngestTransactionRequest;
import com.paymentprocessor.clearingservice.dto.PageResponse;
import com.paymentprocessor.clearingservice.mapper.ClearingMapper;
import com.paymentprocessor.clearingservice.service.ClearingTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clearing/transactions")
@Tag(name = "Clearing Transactions", description = "Ingestion and querying of transactions eligible for clearing")
public class ClearingTransactionController {

    private static final int MAX_PAGE_SIZE = 200;

    private final ClearingTransactionService service;

    public ClearingTransactionController(ClearingTransactionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Ingest a transaction for clearing (idempotent on sourceTransactionId)")
    public ResponseEntity<ClearingTransactionResponse> ingest(@Valid @RequestBody IngestTransactionRequest request) {
        ClearingTransactionResponse body = ClearingMapper.toResponse(service.ingest(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction by id")
    public ClearingTransactionResponse getById(@PathVariable UUID id) {
        return ClearingMapper.toResponse(service.getById(id));
    }

    @GetMapping("/by-source/{sourceTransactionId}")
    @Operation(summary = "Get a transaction by its source transaction id")
    public ClearingTransactionResponse getBySource(@PathVariable String sourceTransactionId) {
        return ClearingMapper.toResponse(service.getBySourceId(sourceTransactionId));
    }

    @GetMapping
    @Operation(summary = "List transactions, optionally filtered by status")
    public PageResponse<ClearingTransactionResponse> list(
            @RequestParam(required = false) ClearingTransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(service.list(status, pageable), ClearingMapper::toResponse);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a transaction that has not yet been cleared")
    public ClearingTransactionResponse cancel(@PathVariable UUID id) {
        return ClearingMapper.toResponse(service.cancel(id));
    }
}
