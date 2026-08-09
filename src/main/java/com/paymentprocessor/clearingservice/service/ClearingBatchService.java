package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.domain.enums.BatchStatus;
import com.paymentprocessor.clearingservice.domain.enums.ClearingTransactionStatus;
import com.paymentprocessor.clearingservice.domain.enums.Network;
import com.paymentprocessor.clearingservice.repository.ClearingBatchRepository;
import com.paymentprocessor.clearingservice.repository.ClearingFileRepository;
import com.paymentprocessor.clearingservice.repository.ClearingTransactionRepository;
import com.paymentprocessor.clearingservice.web.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side queries over clearing batches, files and related transactions. */
@Service
@Transactional(readOnly = true)
public class ClearingBatchService {

    private final ClearingBatchRepository batchRepository;
    private final ClearingFileRepository fileRepository;
    private final ClearingTransactionRepository txnRepository;

    public ClearingBatchService(ClearingBatchRepository batchRepository,
                                ClearingFileRepository fileRepository,
                                ClearingTransactionRepository txnRepository) {
        this.batchRepository = batchRepository;
        this.fileRepository = fileRepository;
        this.txnRepository = txnRepository;
    }

    public ClearingBatch getById(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + id));
    }

    public ClearingBatch getByReference(String reference) {
        return batchRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + reference));
    }

    public Page<ClearingBatch> list(BatchStatus status, Network network, Pageable pageable) {
        if (status != null) {
            return batchRepository.findByStatus(status, pageable);
        }
        if (network != null) {
            return batchRepository.findByNetwork(network, pageable);
        }
        return batchRepository.findAll(pageable);
    }

    public ClearingFile getFile(UUID batchId) {
        getById(batchId); // ensure batch exists
        return fileRepository.findByBatchId(batchId)
                .orElseThrow(() -> new NotFoundException("No file generated for batch: " + batchId));
    }

    public List<ClearingTransaction> getTransactions(UUID batchId) {
        getById(batchId);
        return txnRepository.findByBatchId(batchId);
    }

    public List<ClearingTransaction> getRejections(UUID batchId) {
        getById(batchId);
        return txnRepository.findByBatchIdAndStatus(batchId, ClearingTransactionStatus.REJECTED);
    }
}
