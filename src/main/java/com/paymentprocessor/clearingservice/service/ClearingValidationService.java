package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.service.validation.ValidationChain;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business-rule validation for transactions prior to clearing-file generation.
 * Delegates to a chain of focused validators, making individual rules easy to
 * test and extend without modifying this class.
 */
@Service
public class ClearingValidationService {

    private final ValidationChain validationChain;

    public ClearingValidationService(ValidationChain validationChain) {
        this.validationChain = validationChain;
    }

    public List<String> validate(ClearingTransaction t) {
        return validationChain.validate(t);
    }

    public boolean isValid(ClearingTransaction t) {
        return validate(t).isEmpty();
    }
}
