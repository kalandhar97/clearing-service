package com.paymentprocessor.clearingservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/** Helpers for ISO 4217 currency validation and minor/major-unit conversion. */
public final class CurrencyUtils {

    private CurrencyUtils() {
    }

    public static boolean isValidCurrencyCode(String code) {
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

    public static int fractionDigits(String currencyCode) {
        try {
            int fraction = Currency.getInstance(currencyCode).getDefaultFractionDigits();
            return Math.max(fraction, 0);
        } catch (IllegalArgumentException e) {
            return 2;
        }
    }

    public static String toMajor(long amountMinor, String currencyCode) {
        int fraction = fractionDigits(currencyCode);
        return BigDecimal.valueOf(amountMinor, fraction)
                .setScale(fraction, RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
