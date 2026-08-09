package com.paymentprocessor.clearingservice.web.exception;

import java.util.List;

/** Thrown when a request violates a domain/business rule. */
public class BusinessValidationException extends RuntimeException {

    private final List<String> violations;

    public BusinessValidationException(String message) {
        super(message);
        this.violations = List.of(message);
    }

    public BusinessValidationException(String message, List<String> violations) {
        super(message);
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
