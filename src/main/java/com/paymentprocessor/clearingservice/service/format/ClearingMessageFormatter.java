package com.paymentprocessor.clearingservice.service.format;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import java.util.List;

/**
 * Strategy that renders a batch of transactions into a network-specific
 * clearing file, complete with header, detail records and a control trailer.
 *
 * <p>The encodings produced here are structured and self-consistent
 * (header / detail / trailer with record counts and control totals) but are
 * intentionally simplified representations of the full network specifications
 * (ISO 8583, NACHA, ISO 20022). Exact, certified wire encoding is owned by the
 * external clearing application, per the service's architectural boundary.
 */
public interface ClearingMessageFormatter {

    ClearingFormat format();

    String fileExtension();

    byte[] format(ClearingBatch batch, List<ClearingTransaction> transactions);
}
