package com.paymentprocessor.clearingservice.service.storage;

import com.paymentprocessor.clearingservice.config.ClearingProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Filesystem-backed {@link ClearingFileStorage}. */
@Component
public class LocalFileStorage implements ClearingFileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path root;

    public LocalFileStorage(ClearingProperties properties) {
        this.root = Paths.get(properties.storage().directory()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
            log.info("Clearing file storage directory: {}", root);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create clearing storage directory " + root, e);
        }
    }

    @Override
    public String store(String relativeName, byte[] content) {
        String safeName = Paths.get(relativeName).getFileName().toString();
        Path target = root.resolve(safeName);
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store clearing file " + target, e);
        }
        return target.toUri().toString();
    }

    @Override
    public byte[] read(String storageUri) {
        try {
            Path path = Paths.get(java.net.URI.create(storageUri));
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read clearing file " + storageUri, e);
        }
    }
}
