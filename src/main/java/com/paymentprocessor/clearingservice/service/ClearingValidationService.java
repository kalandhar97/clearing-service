package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business-rule validation for transactions prior to clearing-file generation.
 * Returns the list of violations; an empty list means the transaction is
 * clearable.
 */
@Service
public class ClearingValidationService {

    public List<String> validate(ClearingTransaction t) {
        List<String> violations = new ArrayList<>();

        if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
            violations.add("merchantId is required");
        }
        if (t.getNetwork() == null) {
            violations.add("network is required");
        }
        if (t.getTransactionType() == null) {
            violations.add("transactionType is required");
        }
        if (t.getSettlementDate() == null) {
            violations.add("settlementDate is required");
        }
        if (t.getAmountMinor() <= 0) {
            violations.add("amountMinor must be positive");
        }
        if (!isValidCurrency(t.getCurrency())) {
            violations.add("currency must be a valid ISO 4217 code: " + t.getCurrency());
        }
        if (t.getMcc() != null && !t.getMcc().isBlank() && !t.getMcc().matches("\\d{3,4}")) {
            violations.add("mcc must be 3-4 digits: " + t.getMcc());
        }
        return violations;
    }

    public boolean isValid(ClearingTransaction t) {
        return validate(t).isEmpty();
    }

    private boolean isValidCurrency(String code) {
        if (code == null || code.length() != 3) {
            return false;
        }
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
