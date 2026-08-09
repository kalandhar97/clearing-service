package com.paymentprocessor.clearingservice.repository;

import com.paymentprocessor.clearingservice.domain.enums.Network;
import java.time.LocalDate;

/** Projection of the distinct dimensions used to group pending transactions into batches. */
public interface BatchGroupKey {
    Network getNetwork();
    String getCurrency();
    LocalDate getSettlementDate();
}
