package com.paymentprocessor.clearingservice.repository;

import com.paymentprocessor.clearingservice.domain.ClearingFile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClearingFileRepository extends JpaRepository<ClearingFile, UUID> {
    Optional<ClearingFile> findByBatchId(UUID batchId);
}
