package com.paymentprocessor.clearingservice.service.reference;

import com.paymentprocessor.clearingservice.domain.enums.Network;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Default reference generator producing references of the form
 * {@code CLR-yyyyMMdd-NETWORK-CURRENCY-XXXXXXXX}.
 */
@Component
public class DefaultBatchReferenceGenerator implements BatchReferenceGenerator {

    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String generate(Network network, String currency, LocalDate settlementDate) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CLR-" + settlementDate.format(REF_DATE) + "-" + network.name()
                + "-" + currency + "-" + suffix;
    }
}
