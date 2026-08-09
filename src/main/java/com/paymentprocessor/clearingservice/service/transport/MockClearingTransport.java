package com.paymentprocessor.clearingservice.service.transport;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op transport that simulates a successful hand-off to the external clearing
 * application. Enabled by default ({@code clearing.transport.mock=true}) so the
 * service runs end-to-end without a live downstream endpoint.
 */
@Component
@ConditionalOnProperty(name = "clearing.transport.mock", havingValue = "true", matchIfMissing = true)
public class MockClearingTransport implements ClearingTransport {

    private static final Logger log = LoggerFactory.getLogger(MockClearingTransport.class);

    @Override
    public SubmissionReceipt submit(ClearingBatch batch, ClearingFile file, byte[] content) {
        String receipt = "MOCK-" + UUID.randomUUID();
        log.info("[MOCK] Accepted batch {} file {} ({} bytes) -> receipt {}",
                batch.getReference(), file.getFileName(), content.length, receipt);
        return new SubmissionReceipt(receipt, 202, "{\"status\":\"accepted\",\"receiptReference\":\"" + receipt + "\"}");
    }
}
