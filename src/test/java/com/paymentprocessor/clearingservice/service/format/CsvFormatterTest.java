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

class CsvFormatterTest {

    private final CsvFormatter formatter = new CsvFormatter();

    @Test
    void formatProducesCsvWithHeaderAndTrailer() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.of(2026, 8, 21), ClearingFormat.CSV, null);
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "MERCHANT", Network.VISA,
                TransactionType.SALE, "USD", 1000, null, LocalDate.of(2026, 8, 21), null, null, null, null, null);

        byte[] bytes = formatter.format(batch, List.of(txn));
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("HDR,REF,VISA,USD,2026-08-21");
        assertThat(csv).contains("SRC,MERCHANT,SALE,1000,USD,,,,,2026-08-21");
        assertThat(csv).contains("TRL,1,1000");
    }

    @Test
    void csvValuesAreQuotedWhenNeeded() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.of(2026, 8, 21), ClearingFormat.CSV, null);
        ClearingTransaction txn = ClearingTransaction.ingest("SRC", "A,B", Network.VISA,
                TransactionType.SALE, "USD", 1000, null, LocalDate.of(2026, 8, 21), null, null, null, null, null);

        byte[] bytes = formatter.format(batch, List.of(txn));
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("\"A,B\"");
    }
}
