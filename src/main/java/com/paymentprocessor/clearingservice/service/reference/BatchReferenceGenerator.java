package com.paymentprocessor.clearingservice.service.reference;

import com.paymentprocessor.clearingservice.domain.enums.Network;
import java.time.LocalDate;

/** Generates a unique reference for a clearing batch. */
public interface BatchReferenceGenerator {

    String generate(Network network, String currency, LocalDate settlementDate);
}
