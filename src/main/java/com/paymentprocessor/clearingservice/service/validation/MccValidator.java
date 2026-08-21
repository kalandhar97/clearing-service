package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MccValidator implements TransactionValidator {

    @Override
    public List<String> validate(ClearingTransaction transaction) {
        List<String> violations = new ArrayList<>();
        String mcc = transaction.getMcc();
        if (mcc != null && !mcc.isBlank() && !mcc.matches("\\d{3,4}")) {
            violations.add("mcc must be 3-4 digits: " + mcc);
        }
        return violations;
    }
}
