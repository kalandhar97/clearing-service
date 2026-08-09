package com.paymentprocessor.clearingservice.domain.enums;

/**
 * Clearing participants / rails supported by the service, each with the
 * clearing file format it expects by default.
 */
public enum Network {
    VISA(ClearingFormat.ISO8583),
    MASTERCARD(ClearingFormat.ISO8583),
    AMEX(ClearingFormat.ISO8583),
    DISCOVER(ClearingFormat.ISO8583),
    UNIONPAY(ClearingFormat.ISO8583),
    ACH(ClearingFormat.NACHA),
    SEPA(ClearingFormat.ISO20022),
    FPS(ClearingFormat.ISO20022),
    SWIFT(ClearingFormat.ISO20022),
    FEDWIRE(ClearingFormat.ISO20022);

    private final ClearingFormat defaultFormat;

    Network(ClearingFormat defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    public ClearingFormat defaultFormat() {
        return defaultFormat;
    }
}
