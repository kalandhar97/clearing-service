package com.paymentprocessor.clearingservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyUtilsTest {

    @Test
    void validCurrencyCodeIsRecognized() {
        assertThat(CurrencyUtils.isValidCurrencyCode("USD")).isTrue();
        assertThat(CurrencyUtils.isValidCurrencyCode("EUR")).isTrue();
    }

    @Test
    void invalidCurrencyCodeIsRejected() {
        assertThat(CurrencyUtils.isValidCurrencyCode(null)).isFalse();
        assertThat(CurrencyUtils.isValidCurrencyCode("US")).isFalse();
        assertThat(CurrencyUtils.isValidCurrencyCode("INVALID")).isFalse();
    }

    @Test
    void toMajorConvertsMinorUnits() {
        assertThat(CurrencyUtils.toMajor(12345, "USD")).isEqualTo("123.45");
        assertThat(CurrencyUtils.toMajor(12345, "JPY")).isEqualTo("12345");
    }

    @Test
    void toMajorFallsBackToTwoDecimalsForUnknownCurrency() {
        assertThat(CurrencyUtils.toMajor(12345, "XYZ")).isEqualTo("123.45");
    }
}
