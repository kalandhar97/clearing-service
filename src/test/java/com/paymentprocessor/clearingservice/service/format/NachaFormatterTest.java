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

class NachaFormatterTest {

    private final NachaFormatter formatter = new NachaFormatter();

    @Test
    void formatProducesNachaRecords() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.ACH, "USD", null,
                LocalDate.of(2026, 8, 21), ClearingFormat.NACHA, null);
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.ACH,
                TransactionType.SALE, "USD", 1000, null, LocalDate.of(2026, 8, 21),
                null, null, null, null, null);

        String output = new String(formatter.format(batch, List.of(txn)), StandardCharsets.US_ASCII);

        assertThat(output).contains("1|REF|260821|USD");
        assertThat(output).contains("5|ACH|260821");
        assertThat(output).contains("6|27|MERCHANT|1000|SRC|");
        assertThat(output).contains("8|1|1000");
        assertThat(output).contains("9|1|1000");
    }

    @Test
    void refundUsesCreditCode22() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.ACH, "USD", null,
                LocalDate.of(2026, 8, 21), ClearingFormat.NACHA, null);
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.ACH,
                TransactionType.REFUND, "USD", 1000, null, LocalDate.of(2026, 8, 21),
                null, null, null, null, null);

        String output = new String(formatter.format(batch, List.of(txn)), StandardCharsets.US_ASCII);
        assertThat(output).contains("6|22|");
    }
}
