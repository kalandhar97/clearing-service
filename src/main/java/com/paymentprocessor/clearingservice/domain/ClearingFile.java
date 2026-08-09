package com.paymentprocessor.clearingservice.domain;

import com.paymentprocessor.clearingservice.domain.enums.ClearingFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Metadata for the generated clearing file belonging to a batch (one-to-one). */
@Entity
@Table(name = "clearing_file")
public class ClearingFile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 16)
    private ClearingFormat format;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "record_count", nullable = false)
    private int recordCount;

    @Column(name = "control_total_minor", nullable = false)
    private long controlTotalMinor;

    @Column(name = "storage_uri", nullable = false, length = 512)
    private String storageUri;

    @Column(name = "signature", length = 512)
    private String signature;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    protected ClearingFile() {
        // for JPA
    }

    public static ClearingFile of(UUID batchId, ClearingFormat format, String fileName,
                                  String contentHash, long sizeBytes, int recordCount,
                                  long controlTotalMinor, String storageUri, String signature) {
        ClearingFile f = new ClearingFile();
        f.id = UUID.randomUUID();
        f.batchId = batchId;
        f.format = format;
        f.fileName = fileName;
        f.contentHash = contentHash;
        f.sizeBytes = sizeBytes;
        f.recordCount = recordCount;
        f.controlTotalMinor = controlTotalMinor;
        f.storageUri = storageUri;
        f.signature = signature;
        return f;
    }

    @PrePersist
    void onCreate() {
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public ClearingFormat getFormat() { return format; }
    public String getFileName() { return fileName; }
    public String getContentHash() { return contentHash; }
    public long getSizeBytes() { return sizeBytes; }
    public int getRecordCount() { return recordCount; }
    public long getControlTotalMinor() { return controlTotalMinor; }
    public String getStorageUri() { return storageUri; }
    public String getSignature() { return signature; }
    public Instant getGeneratedAt() { return generatedAt; }
}
