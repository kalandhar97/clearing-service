package com.paymentprocessor.clearingservice.web.exception;

/** Thrown when creating a resource that already exists (idempotency conflict). */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
