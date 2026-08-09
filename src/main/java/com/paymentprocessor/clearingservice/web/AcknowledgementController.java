package com.paymentprocessor.clearingservice.web;

import com.paymentprocessor.clearingservice.dto.AcknowledgementRequest;
import com.paymentprocessor.clearingservice.dto.AcknowledgementResponse;
import com.paymentprocessor.clearingservice.mapper.ClearingMapper;
import com.paymentprocessor.clearingservice.service.AcknowledgementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clearing/batches/{batchId}/acknowledgements")
@Tag(name = "Clearing Acknowledgements", description = "Inbound acknowledgements from clearing participants")
public class AcknowledgementController {

    private final AcknowledgementService service;

    public AcknowledgementController(AcknowledgementService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record an acknowledgement (ACK/ACCEPT/REJECT/PARTIAL) for a submitted batch")
    public AcknowledgementResponse process(@PathVariable UUID batchId,
                                           @Valid @RequestBody AcknowledgementRequest request) {
        return service.process(batchId, request);
    }

    @GetMapping
    @Operation(summary = "List acknowledgements received for a batch")
    public List<AcknowledgementResponse> list(@PathVariable UUID batchId) {
        return service.forBatch(batchId).stream()
                .map(a -> ClearingMapper.toResponse(a, null))
                .toList();
    }
}
