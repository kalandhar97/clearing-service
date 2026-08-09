package com.paymentprocessor.clearingservice.service.transport;

/** Receipt returned by the external clearing application when a file is accepted for processing. */
public record SubmissionReceipt(
        String receiptReference,
        int statusCode,
        String rawResponse
) {
}
