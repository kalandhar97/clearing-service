package com.paymentprocessor.clearingservice.audit;

import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.repository.AuditEntryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records and queries the immutable clearing audit trail. */
@Service
public class AuditService {

    private final AuditEntryRepository repository;

    public AuditService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists an audit entry. Participates in the caller's transaction so the
     * audit record and the state change it describes commit atomically.
     */
    @Transactional
    public void record(AuditEntry entry) {
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> forBatch(UUID batchId) {
        return repository.findByBatchIdOrderByCreatedAtAsc(batchId);
    }
}
