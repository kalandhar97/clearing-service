package com.paymentprocessor.clearingservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void nullToEmptyReturnsEmptyForNull() {
        assertThat(StringUtils.nullToEmpty(null)).isEmpty();
    }

    @Test
    void nullToEmptyReturnsValueWhenPresent() {
        assertThat(StringUtils.nullToEmpty("foo")).isEqualTo("foo");
    }

    @Test
    void truncateReturnsNullForNull() {
        assertThat(StringUtils.truncate(null, 10)).isNull();
    }

    @Test
    void truncateLeavesShortStringsIntact() {
        assertThat(StringUtils.truncate("short", 10)).isEqualTo("short");
    }

    @Test
    void truncateCutsLongStrings() {
        String value = "a".repeat(20);
        assertThat(StringUtils.truncate(value, 10)).isEqualTo("a".repeat(10));
    }

    @Test
    void escapeCsvReturnsEmptyForNull() {
        assertThat(StringUtils.escapeCsv(null)).isEmpty();
    }

    @Test
    void escapeCsvLeavesPlainValueAlone() {
        assertThat(StringUtils.escapeCsv("plain")).isEqualTo("plain");
    }

    @Test
    void escapeCsvQuotesCommaContainingValue() {
        assertThat(StringUtils.escapeCsv("a,b")).isEqualTo("\"a,b\"");
    }

    @Test
    void escapeCsvDoublesQuotes() {
        assertThat(StringUtils.escapeCsv("a\"b")).isEqualTo("\"a\"\"b\"");
    }

    @Test
    void escapeXmlEscapesSpecialCharacters() {
        assertThat(StringUtils.escapeXml("a < b & c > d")).isEqualTo("a &lt; b &amp; c &gt; d");
    }
}
