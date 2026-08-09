package com.paymentprocessor.clearingservice.service.format;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ISO 8583-style clearing presentment file for card networks. Each transaction
 * is rendered as an MTI 1240 (clearing/advice) message with key data elements;
 * a header and control trailer bookend the detail records.
 */
@Component
public class Iso8583Formatter implements ClearingMessageFormatter {

    @Override
    public ClearingFormat format() {
        return ClearingFormat.ISO8583;
    }

    @Override
    public String fileExtension() {
        return "8583";
    }

    @Override
    public byte[] format(ClearingBatch batch, List<ClearingTransaction> transactions) {
        StringBuilder sb = new StringBuilder(256 + transactions.size() * 128);
        // File header (network, currency, business/settlement date, reference)
        sb.append("FHDR|").append(batch.getNetwork())
                .append('|').append(batch.getCurrency())
                .append('|').append(batch.getSettlementDate())
                .append('|').append(batch.getReference())
                .append('\n');
        long total = 0L;
        for (ClearingTransaction t : transactions) {
            total += t.getAmountMinor();
            // MTI 1240 clearing message with selected data elements.
            sb.append("1240")
                    .append("|DE2=").append(nullToEmpty(t.getPanToken()))
                    .append("|DE3=").append(processingCode(t))          // processing code
                    .append("|DE4=").append(pad12(t.getAmountMinor()))  // amount, minor units
                    .append("|DE37=").append(nullToEmpty(t.getArn()))   // retrieval reference / ARN
                    .append("|DE38=").append(nullToEmpty(t.getAuthCode()))
                    .append("|DE41=").append(nullToEmpty(t.getMerchantId()))
                    .append("|DE49=").append(t.getCurrency())
                    .append("|DE63=").append(t.getSourceTransactionId())
                    .append("|MCC=").append(nullToEmpty(t.getMcc()))
                    .append('\n');
        }
        // File trailer: record count + control total (minor units)
        sb.append("FTRL|").append(transactions.size()).append('|').append(pad12(total)).append('\n');
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String processingCode(ClearingTransaction t) {
        return switch (t.getTransactionType()) {
            case SALE -> "000000";
            case REFUND -> "200000";
            case CHARGEBACK -> "280000";
            case ADJUSTMENT -> "020000";
        };
    }

    private static String pad12(long amountMinor) {
        String s = Long.toString(Math.abs(amountMinor));
        return "000000000000".substring(Math.min(s.length(), 12)) + s;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
