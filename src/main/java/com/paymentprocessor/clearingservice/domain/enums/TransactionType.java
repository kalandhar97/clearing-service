package com.paymentprocessor.clearingservice.domain.enums;

/** Category of a transaction submitted for clearing. */
public enum TransactionType {
    SALE,
    REFUND,
    CHARGEBACK,
    ADJUSTMENT
}
