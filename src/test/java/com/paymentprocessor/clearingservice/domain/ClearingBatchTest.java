package com.paymentprocessor.clearingservice.domain;

import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClearingBatchTest {

    @Test
    void batchIsCreatedWithPendingStatus() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.now(), ClearingFormat.ISO8583, null);
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.CREATED);
    }

    @Test
    void happyPathTransitionSetsTimestamps() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.now(), ClearingFormat.ISO8583, null);
        batch.transitionTo(BatchStatus.VALIDATED);
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.VALIDATED);
        assertThat(batch.getValidatedAt()).isNotNull();
    }

    @Test
    void illegalTransitionThrows() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.now(), ClearingFormat.ISO8583, null);
        assertThatThrownBy(() -> batch.transitionTo(BatchStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void terminalStatesCannotTransition() {
        ClearingBatch batch = ClearingBatch.create("REF", Network.VISA, "USD", null,
                LocalDate.now(), ClearingFormat.ISO8583, null);
        batch.transitionTo(BatchStatus.VALIDATED);
        batch.transitionTo(BatchStatus.SENT);
        batch.transitionTo(BatchStatus.ACKNOWLEDGED);
        batch.transitionTo(BatchStatus.COMPLETED);
        assertThat(batch.getStatus().isTerminal()).isTrue();
        assertThatThrownBy(() -> batch.transitionTo(BatchStatus.FAILED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void builderProducesEquivalentBatch() {
        ClearingBatch batch = ClearingBatch.builder()
                .reference("REF-1")
                .network(Network.ACH)
                .currency("USD")
                .settlementDate(LocalDate.now())
                .format(ClearingFormat.NACHA)
                .build();
        assertThat(batch.getReference()).isEqualTo("REF-1");
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.CREATED);
    }
}
