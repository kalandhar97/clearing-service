package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable context passed through a validation chain so validators can append
 * violation messages without each knowing about the others.
 */
public final class ValidationContext {

    private final ClearingTransaction transaction;
    private final List<String> violations = new ArrayList<>();

    public ValidationContext(ClearingTransaction transaction) {
        this.transaction = transaction;
    }

    public ClearingTransaction getTransaction() {
        return transaction;
    }

    public void addViolation(String message) {
        violations.add(message);
    }

    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }
}
