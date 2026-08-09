package com.paymentprocessor.clearingservice.service.storage;

/**
 * Abstraction over where generated clearing files are persisted. The default
 * implementation writes to the local filesystem; a production deployment can
 * swap in an S3/GCS-backed implementation without touching callers.
 */
public interface ClearingFileStorage {

    /**
     * Persists file content and returns a stable storage URI.
     *
     * @param relativeName file name (no directory separators expected)
     * @param content      raw file bytes
     * @return storage URI that can later be passed to {@link #read(String)}
     */
    String store(String relativeName, byte[] content);

    /** Reads previously stored content by its storage URI. */
    byte[] read(String storageUri);
}
