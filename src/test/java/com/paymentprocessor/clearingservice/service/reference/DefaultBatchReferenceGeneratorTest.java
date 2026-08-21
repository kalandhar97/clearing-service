package com.paymentprocessor.clearingservice.service.reference;

import com.paymentprocessor.clearingservice.domain.enums.Network;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBatchReferenceGeneratorTest {

    private final DefaultBatchReferenceGenerator generator = new DefaultBatchReferenceGenerator();

    @Test
    void referenceContainsExpectedComponents() {
        String reference = generator.generate(Network.VISA, "USD", LocalDate.of(2026, 8, 21));
        assertThat(reference).startsWith("CLR-20260821-VISA-USD-");
        assertThat(reference).hasSizeGreaterThan("CLR-20260821-VISA-USD-".length());
    }
}
