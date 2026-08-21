package com.paymentprocessor.clearingservice.domain;

import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        assertThat(BatchStatus.CREATED.canTransitionTo(BatchStatus.VALIDATED)).isTrue();
        assertThat(BatchStatus.VALIDATED.canTransitionTo(BatchStatus.SENT)).isTrue();
        assertThat(BatchStatus.SENT.canTransitionTo(BatchStatus.ACKNOWLEDGED)).isTrue();
        assertThat(BatchStatus.ACKNOWLEDGED.canTransitionTo(BatchStatus.COMPLETED)).isTrue();
    }

    @Test
    void anyNonTerminalCanTransitionToFailed() {
        assertThat(BatchStatus.CREATED.canTransitionTo(BatchStatus.FAILED)).isTrue();
        assertThat(BatchStatus.VALIDATED.canTransitionTo(BatchStatus.FAILED)).isTrue();
        assertThat(BatchStatus.SENT.canTransitionTo(BatchStatus.FAILED)).isTrue();
    }

    @Test
    void terminalStatesRejectAllTransitions() {
        assertThat(BatchStatus.COMPLETED.canTransitionTo(BatchStatus.FAILED)).isFalse();
        assertThat(BatchStatus.FAILED.canTransitionTo(BatchStatus.CREATED)).isFalse();
    }

    @Test
    void sameStateTransitionIsRejected() {
        assertThat(BatchStatus.VALIDATED.canTransitionTo(BatchStatus.VALIDATED)).isFalse();
    }
}
