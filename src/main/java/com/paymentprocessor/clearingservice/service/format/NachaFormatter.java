package com.paymentprocessor.clearingservice.service.format;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * NACHA-style ACH clearing file. Produces the canonical record-type sequence
 * (1 file header, 5 batch header, 6 entry detail, 8 batch control, 9 file
 * control) with entry counts and control totals.
 */
@Component
public class NachaFormatter implements ClearingMessageFormatter {

    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    @Override
    public ClearingFormat format() {
        return ClearingFormat.NACHA;
    }

    @Override
    public String fileExtension() {
        return "ach";
    }

    @Override
    public byte[] format(ClearingBatch batch, List<ClearingTransaction> transactions) {
        StringBuilder sb = new StringBuilder(256 + transactions.size() * 96);
        String date = batch.getSettlementDate().format(YYMMDD);

        // 1 - File Header Record
        sb.append("1|").append(batch.getReference()).append('|').append(date)
                .append('|').append(batch.getCurrency()).append('\n');
        // 5 - Batch Header Record
        sb.append("5|").append(batch.getNetwork()).append('|').append(date).append('\n');

        long total = 0L;
        int seq = 0;
        for (ClearingTransaction t : transactions) {
            total += t.getAmountMinor();
            seq++;
            // 6 - Entry Detail Record
            sb.append("6|").append(transactionCode(t))
                    .append('|').append(nullToEmpty(t.getMerchantId()))
                    .append('|').append(t.getAmountMinor())
                    .append('|').append(t.getSourceTransactionId())
                    .append('|').append(nullToEmpty(t.getArn()))
                    .append('|').append(pad(seq))
                    .append('\n');
        }
        // 8 - Batch Control Record (entry count + control total)
        sb.append("8|").append(transactions.size()).append('|').append(total).append('\n');
        // 9 - File Control Record
        sb.append("9|").append(transactions.size()).append('|').append(total).append('\n');
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String transactionCode(ClearingTransaction t) {
        // 22 = credit to checking, 27 = debit to checking (simplified mapping)
        return switch (t.getTransactionType()) {
            case SALE, ADJUSTMENT -> "27";
            case REFUND, CHARGEBACK -> "22";
        };
    }

    private static String pad(int seq) {
        String s = Integer.toString(seq);
        return "0000000".substring(Math.min(s.length(), 7)) + s;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
