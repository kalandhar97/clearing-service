package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.service.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.paymentprocessor.clearingservice.service.TestFixtures.sampleTransaction;
import static org.assertj.core.api.Assertions.assertThat;

class ClearingValidationServiceTest {

    private ClearingValidationService service;

    @BeforeEach
    void setUp() {
        service = new ClearingValidationService(new ValidationChain(List.of(
                new RequiredFieldValidator(),
                new AmountValidator(),
                new CurrencyValidator(),
                new MccValidator())));
    }

    @Test
    void validTransactionReturnsEmptyList() {
        assertThat(service.validate(sampleTransaction())).isEmpty();
    }

    @Test
    void isValidReturnsTrueForValidTransaction() {
        assertThat(service.isValid(sampleTransaction())).isTrue();
    }

    @Test
    void invalidTransactionReturnsViolations() {
        ClearingTransaction txn = ClearingTransaction.builder()
                .sourceTransactionId("SRC")
                .merchantId("")
                .build();
        assertThat(service.validate(txn)).isNotEmpty();
        assertThat(service.isValid(txn)).isFalse();
    }
}
