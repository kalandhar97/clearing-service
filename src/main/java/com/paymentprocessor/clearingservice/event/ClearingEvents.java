package com.paymentprocessor.clearingservice.event;

/** Canonical domain-event and aggregate-type names published by the service. */
public final class ClearingEvents {

    private ClearingEvents() {
    }

    public static final String AGGREGATE_BATCH = "ClearingBatch";

    public static final String CLEARING_BATCH_CREATED = "ClearingBatchCreated";
    public static final String CLEARING_SUBMITTED = "ClearingSubmitted";
    public static final String CLEARING_ACCEPTED = "ClearingAccepted";
    public static final String CLEARING_REJECTED = "ClearingRejected";
}
