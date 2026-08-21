package com.paymentprocessor.clearingservice.service.format;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/** Simple, portable CSV clearing file for API-based partners. */
@Component
public class CsvFormatter implements ClearingMessageFormatter {

    @Override
    public ClearingFormat format() {
        return ClearingFormat.CSV;
    }

    @Override
    public String fileExtension() {
        return "csv";
    }

    @Override
    public byte[] format(ClearingBatch batch, List<ClearingTransaction> transactions) {
        StringBuilder sb = new StringBuilder(256 + transactions.size() * 96);
        // Header
        sb.append("HDR,").append(batch.getReference()).append(',')
                .append(batch.getNetwork()).append(',')
                .append(batch.getCurrency()).append(',')
                .append(batch.getSettlementDate()).append('\n');
        // Column header
        sb.append("source_transaction_id,merchant_id,transaction_type,amount_minor,"
                + "currency,mcc,auth_code,arn,pan_token,settlement_date\n");
        long total = 0L;
        for (ClearingTransaction t : transactions) {
            total += t.getAmountMinor();
            sb.append(StringUtils.escapeCsv(t.getSourceTransactionId())).append(',')
                    .append(StringUtils.escapeCsv(t.getMerchantId())).append(',')
                    .append(t.getTransactionType()).append(',')
                    .append(t.getAmountMinor()).append(',')
                    .append(t.getCurrency()).append(',')
                    .append(StringUtils.escapeCsv(t.getMcc())).append(',')
                    .append(StringUtils.escapeCsv(t.getAuthCode())).append(',')
                    .append(StringUtils.escapeCsv(t.getArn())).append(',')
                    .append(StringUtils.escapeCsv(t.getPanToken())).append(',')
                    .append(t.getSettlementDate()).append('\n');
        }
        // Trailer: record count and control total
        sb.append("TRL,").append(transactions.size()).append(',').append(total).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
