package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RequiredFieldValidator implements TransactionValidator {

    @Override
    public List<String> validate(ClearingTransaction transaction) {
        List<String> violations = new ArrayList<>();
        if (isBlank(transaction.getMerchantId())) {
            violations.add("merchantId is required");
        }
        if (transaction.getNetwork() == null) {
            violations.add("network is required");
        }
        if (transaction.getTransactionType() == null) {
            violations.add("transactionType is required");
        }
        if (transaction.getSettlementDate() == null) {
            violations.add("settlementDate is required");
        }
        return violations;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
