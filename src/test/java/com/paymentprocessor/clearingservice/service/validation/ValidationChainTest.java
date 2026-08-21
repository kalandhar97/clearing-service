package com.paymentprocessor.clearingservice.service.validation;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationChainTest {

    private ValidationChain chain;

    @BeforeEach
    void setUp() {
        chain = new ValidationChain(List.of(
                new RequiredFieldValidator(),
                new AmountValidator(),
                new CurrencyValidator(),
                new MccValidator()));
    }

    @Test
    void validTransactionHasNoViolations() {
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 100, null, LocalDate.now(), null, null, null, null, null);
        assertThat(chain.validate(txn)).isEmpty();
    }

    @Test
    void invalidTransactionAggregatesViolations() {
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "", null, null,
                "XYZ", 0, null, null, "ABC", null, null, null, null);
        List<String> violations = chain.validate(txn);
        assertThat(violations).contains(
                "merchantId is required",
                "network is required",
                "transactionType is required",
                "settlementDate is required",
                "amountMinor must be positive",
                "currency must be a valid ISO 4217 code: XYZ");
    }

    @Test
    void invalidMccIsReported() {
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 100, null, LocalDate.now(), "AB", null, null, null, null);
        assertThat(chain.validate(txn)).containsExactly("mcc must be 3-4 digits: AB");
    }
}
