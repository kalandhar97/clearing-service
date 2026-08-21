package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import java.util.List;

/**
 * Single-responsibility validator in the transaction validation chain.
 * Implementations contribute zero or more violation messages for a transaction.
 */
public interface TransactionValidator {

    List<String> validate(ClearingTransaction transaction);
}
