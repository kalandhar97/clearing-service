package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CurrencyValidator implements TransactionValidator {

    @Override
    public List<String> validate(ClearingTransaction transaction) {
        List<String> violations = new ArrayList<>();
        if (!CurrencyUtils.isValidCurrencyCode(transaction.getCurrency())) {
            violations.add("currency must be a valid ISO 4217 code: " + transaction.getCurrency());
        }
        return violations;
    }
}
