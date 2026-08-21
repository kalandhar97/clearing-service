package com.paymentprocessor.clearingservice.service.format;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Iso8583FormatterTest {

    private final Iso8583Formatter formatter = new Iso8583Formatter();

    @Test
    void formatProducesIso8583File() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.of(2026, 8, 21), ClearingFormat.ISO8583, null);
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 1000, null, LocalDate.of(2026, 8, 21),
                "1234", "AUTH", "ARN", "PAN", null);

        byte[] bytes = formatter.format(batch, List.of(txn));
        String output = new String(bytes, StandardCharsets.US_ASCII);

        assertThat(output).contains("FHDR|VISA|USD|2026-08-21|REF");
        assertThat(output).contains("1240");
        assertThat(output).contains("|DE2=PAN");
        assertThat(output).contains("|DE3=000000");
        assertThat(output).contains("|DE4=000000001000");
        assertThat(output).contains("FTRL|1|000000001000");
    }

    @Test
    void refundUsesProcessingCode200000() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.of(2026, 8, 21), ClearingFormat.ISO8583, null);
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA,
                TransactionType.REFUND, "USD", 1000, null, LocalDate.of(2026, 8, 21),
                null, null, null, null, null);

        String output = new String(formatter.format(batch, List.of(txn)), StandardCharsets.US_ASCII);
        assertThat(output).contains("|DE3=200000");
    }
}
