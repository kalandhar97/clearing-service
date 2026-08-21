package com.paymentprocessor.clearingservice.service;

import com.paymentprocessor.clearingservice.audit.AuditService;
import com.paymentprocessor.clearingservice.domain.AuditEntry;
import com.paymentprocessor.clearingservice.domain.ClearingBatch;
import com.paymentprocessor.clearingservice.domain.ClearingFile;
import com.paymentprocessor.clearingservice.domain.ClearingTransaction;
import com.paymentprocessor.clearingservice.repository.ClearingFileRepository;
import com.paymentprocessor.clearingservice.service.format.ClearingMessageFormatter;
import com.paymentprocessor.clearingservice.service.format.FormatterRegistry;
import com.paymentprocessor.clearingservice.service.storage.ClearingFileStorage;
import com.paymentprocessor.clearingservice.util.HashUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Generates, hashes, stores and records the clearing file for a batch. */
@Service
public class FileGenerationService {

    private static final Logger log = LoggerFactory.getLogger(FileGenerationService.class);

    private final FormatterRegistry formatterRegistry;
    private final ClearingFileStorage storage;
    private final ClearingFileRepository fileRepository;
    private final AuditService auditService;

    public FileGenerationService(FormatterRegistry formatterRegistry,
                                 ClearingFileStorage storage,
                                 ClearingFileRepository fileRepository,
                                 AuditService auditService) {
        this.formatterRegistry = formatterRegistry;
        this.storage = storage;
        this.fileRepository = fileRepository;
        this.auditService = auditService;
    }

    public ClearingFile generate(ClearingBatch batch, List<ClearingTransaction> transactions) {
        ClearingMessageFormatter formatter = formatterRegistry.get(batch.getFormat());
        byte[] content = formatter.format(batch, transactions);
        String hash = HashUtils.sha256Hex(content);
        long controlTotal = transactions.stream().mapToLong(ClearingTransaction::getAmountMinor).sum();
        String fileName = batch.getReference() + "." + formatter.fileExtension();
        String storageUri = storage.store(fileName, content);
        String signature = "SHA256:" + hash;

        ClearingFile file = ClearingFile.of(
                batch.getId(),
                batch.getFormat(),
                fileName,
                hash,
                content.length,
                transactions.size(),
                controlTotal,
                storageUri,
                signature);
        file = fileRepository.save(file);

        auditService.record(AuditEntry.builder("GENERATED", "clearing-service")
                .batchId(batch.getId())
                .participantId(batch.getNetwork().name())
                .fileName(fileName)
                .fileHash(hash)
                .detail("Generated " + batch.getFormat() + " file with " + transactions.size()
                        + " records, control total " + controlTotal + " minor units")
                .build());

        log.info("Generated clearing file {} ({} bytes, hash={}) for batch {}",
                fileName, content.length, hash, batch.getReference());
        return file;
    }
}
