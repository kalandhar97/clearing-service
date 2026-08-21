package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Composite validator that runs every registered validator and returns the
 * aggregated list of violations. New rules can be added without changing the
 * consumers, satisfying the Open/Closed Principle.
 */
@Component
public class ValidationChain {

    private final List<TransactionValidator> validators;

    public ValidationChain(List<TransactionValidator> validators) {
        this.validators = new ArrayList<>(validators);
    }

    public List<String> validate(ClearingTransaction transaction) {
        List<String> all = new ArrayList<>();
        for (TransactionValidator validator : validators) {
            all.addAll(validator.validate(transaction));
        }
        return Collections.unmodifiableList(all);
    }
}
