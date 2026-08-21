package com.paymentprocessor.clearingservice.repository;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClearingBatchRepositoryTest {

    @Autowired
    private ClearingBatchRepository repository;

    @Test
    void saveAndFindByReference() {
        ClearingBatch batch = ClearingBatch.create("REF-001", Network.VISA, "USD", null,
                LocalDate.now(), ClearingFormat.ISO8583, null);
        repository.save(batch);

        Optional<ClearingBatch> found = repository.findByReference("REF-001");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(BatchStatus.CREATED);
    }
}
