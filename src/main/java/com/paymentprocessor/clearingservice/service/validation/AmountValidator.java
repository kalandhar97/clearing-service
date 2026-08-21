package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AmountValidator implements TransactionValidator {

    @Override
    public List<String> validate(ClearingTransaction transaction) {
        List<String> violations = new ArrayList<>();
        if (transaction.getAmountMinor() <= 0) {
            violations.add("amountMinor must be positive");
        }
        return violations;
    }
}
