package com.paymentprocessor.clearingservice.service.format;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.util.CurrencyUtils;
import com.paymentprocessor.clearingservice.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ISO 20022 pacs.008-style XML clearing file. This renders a valid, well-formed
 * document with a group header (control sum + number of transactions) and one
 * credit-transfer transaction information block per transaction.
 */
@Component
public class Iso20022Formatter implements ClearingMessageFormatter {

    @Override
    public ClearingFormat format() {
        return ClearingFormat.ISO20022;
    }

    @Override
    public String fileExtension() {
        return "xml";
    }

    @Override
    public byte[] format(ClearingBatch batch, List<ClearingTransaction> transactions) {
        long totalMinor = transactions.stream().mapToLong(ClearingTransaction::getAmountMinor).sum();
        String ccy = batch.getCurrency();
        StringBuilder sb = new StringBuilder(512 + transactions.size() * 256);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">\n");
        sb.append("  <FIToFICstmrCdtTrf>\n");
        sb.append("    <GrpHdr>\n");
        sb.append("      <MsgId>").append(StringUtils.escapeXml(batch.getReference())).append("</MsgId>\n");
        sb.append("      <CreDtTm>").append(Instant.now()).append("</CreDtTm>\n");
        sb.append("      <NbOfTxs>").append(transactions.size()).append("</NbOfTxs>\n");
        sb.append("      <CtrlSum>").append(CurrencyUtils.toMajor(totalMinor, ccy)).append("</CtrlSum>\n");
        sb.append("      <SttlmInf><SttlmMtd>CLRG</SttlmMtd></SttlmInf>\n");
        sb.append("    </GrpHdr>\n");
        for (ClearingTransaction t : transactions) {
            sb.append("    <CdtTrfTxInf>\n");
            sb.append("      <PmtId><EndToEndId>").append(StringUtils.escapeXml(t.getSourceTransactionId()))
                    .append("</EndToEndId>");
            if (t.getArn() != null) {
                sb.append("<TxId>").append(StringUtils.escapeXml(t.getArn())).append("</TxId>");
            }
            sb.append("</PmtId>\n");
            sb.append("      <IntrBkSttlmAmt Ccy=\"").append(ccy).append("\">")
                    .append(CurrencyUtils.toMajor(t.getAmountMinor(), ccy)).append("</IntrBkSttlmAmt>\n");
            sb.append("      <IntrBkSttlmDt>").append(t.getSettlementDate()).append("</IntrBkSttlmDt>\n");
            sb.append("      <ChrgBr>SLEV</ChrgBr>\n");
            sb.append("      <Cdtr><Id>").append(StringUtils.escapeXml(t.getMerchantId())).append("</Id></Cdtr>\n");
            sb.append("      <Purp><Cd>").append(t.getTransactionType()).append("</Cd></Purp>\n");
            sb.append("    </CdtTrfTxInf>\n");
        }
        sb.append("  </FIToFICstmrCdtTrf>\n");
        sb.append("</Document>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
