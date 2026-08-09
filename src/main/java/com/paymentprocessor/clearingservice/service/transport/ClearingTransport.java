package com.paymentprocessor.clearingservice.service.transport;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;

/**
 * Transmits a generated clearing file to the external clearing application and
 * returns its receipt. Implementations must throw {@link TransportException}
 * (with an appropriate {@code retryable} flag) on failure.
 */
public interface ClearingTransport {

    SubmissionReceipt submit(ClearingBatch batch, ClearingFile file, byte[] content);
}
