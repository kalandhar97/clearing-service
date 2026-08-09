package com.paymentprocessor.clearingservice.repository;

import com.paymentprocessor.clearingservice.domain.AuditEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {
    List<AuditEntry> findByBatchIdOrderByCreatedAtAsc(UUID batchId);
}
