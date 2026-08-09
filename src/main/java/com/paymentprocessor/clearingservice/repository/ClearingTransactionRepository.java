package com.paymentprocessor.clearingservice.repository;

import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClearingTransactionRepository extends JpaRepository<ClearingTransaction, UUID> {

    boolean existsBySourceTransactionId(String sourceTransactionId);

    Optional<ClearingTransaction> findBySourceTransactionId(String sourceTransactionId);

    Page<ClearingTransaction> findByStatus(ClearingTransactionStatus status, Pageable pageable);

    List<ClearingTransaction> findByBatchId(UUID batchId);

    List<ClearingTransaction> findByBatchIdAndStatus(UUID batchId, ClearingTransactionStatus status);

    /** Distinct (network, currency, settlementDate) groups among PENDING transactions. */
    @Query("""
            select t.network as network, t.currency as currency, t.settlementDate as settlementDate
            from ClearingTransaction t
            where t.status = com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus.PENDING
            group by t.network, t.currency, t.settlementDate
            """)
    List<BatchGroupKey> findPendingGroups();

    @Query("""
            select t from ClearingTransaction t
            where t.status = com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus.PENDING
              and t.network = :network
              and t.currency = :currency
              and t.settlementDate = :settlementDate
            order by t.createdAt asc
            """)
    List<ClearingTransaction> findPendingForGroup(@Param("network") Network network,
                                                  @Param("currency") String currency,
                                                  @Param("settlementDate") LocalDate settlementDate,
                                                  Pageable pageable);
}
