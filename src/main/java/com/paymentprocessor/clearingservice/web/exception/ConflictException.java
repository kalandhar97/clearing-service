package com.paymentprocessor.clearingservice.web.exception;

/** Thrown when an operation conflicts with the current state of a resource. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
