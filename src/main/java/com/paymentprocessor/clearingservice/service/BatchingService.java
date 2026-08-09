package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.dto.BatchFormationResponse;
import com.paymentprocessor.clearingservice.mapper.ClearingMapper;
import com.paymentprocessor.clearingservice.repository.BatchGroupKey;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates batch formation across all pending transaction groups. Each
 * group is prepared in its own transaction via {@link BatchPreparationService},
 * so a failure in one group is isolated from the rest.
 */
@Service
public class BatchingService {

    private static final Logger log = LoggerFactory.getLogger(BatchingService.class);

    private final ClearingTransactionRepository txnRepository;
    private final BatchPreparationService batchPreparationService;

    public BatchingService(ClearingTransactionRepository txnRepository,
                           BatchPreparationService batchPreparationService) {
        this.txnRepository = txnRepository;
        this.batchPreparationService = batchPreparationService;
    }

    /** Reads pending groups in a short read-only transaction. */
    @Transactional(readOnly = true)
    public List<BatchGroupKey> pendingGroups() {
        return txnRepository.findPendingGroups();
    }

    public BatchFormationResponse formBatches() {
        List<BatchGroupKey> groups = pendingGroups();
        List<ClearingBatch> created = new ArrayList<>();
        int transactionsBatched = 0;

        for (BatchGroupKey g : groups) {
            try {
                var result = batchPreparationService.prepareBatch(
                        g.getNetwork(), g.getCurrency(), g.getSettlementDate());
                if (result.isPresent()) {
                    ClearingBatch batch = result.get();
                    created.add(batch);
                    transactionsBatched += batch.getTransactionCount();
                }
            } catch (RuntimeException e) {
                log.error("Failed to prepare batch for group network={} currency={} date={}",
                        g.getNetwork(), g.getCurrency(), g.getSettlementDate(), e);
            }
        }

        log.info("Batch formation complete: {} batches created, {} transactions batched",
                created.size(), transactionsBatched);
        return new BatchFormationResponse(
                created.size(),
                transactionsBatched,
                created.stream().map(ClearingMapper::toResponse).toList());
    }
}
