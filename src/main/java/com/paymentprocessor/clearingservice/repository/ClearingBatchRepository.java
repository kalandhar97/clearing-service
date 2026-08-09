package com.paymentprocessor.clearingservice.repository;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClearingBatchRepository extends JpaRepository<ClearingBatch, UUID> {

    Optional<ClearingBatch> findByReference(String reference);

    Page<ClearingBatch> findByStatus(BatchStatus status, Pageable pageable);

    Page<ClearingBatch> findByNetwork(Network network, Pageable pageable);

    long countByStatus(BatchStatus status);

    /** Validated batches that are due for a (first or retry) submission attempt. */
    @Query("""
            select b from ClearingBatch b
            where b.status = com.paymentprocessor.clearingservice.domain.enums.BatchStatus.VALIDATED
              and (b.nextAttemptAt is null or b.nextAttemptAt <= :now)
            order by b.createdAt asc
            """)
    List<ClearingBatch> findSubmittable(@Param("now") Instant now, Pageable pageable);
}
