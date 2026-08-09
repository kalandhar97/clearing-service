package com.paymentprocessor.clearingservice.service.transport;

/** JSON body posted to the external clearing application. */
public record RestSubmissionPayload(
        String batchReference,
        String network,
        String currency,
        String settlementDate,
        String format,
        int recordCount,
        long controlTotalMinor,
        String contentHash,
        String fileName,
        String contentBase64
) {
}
