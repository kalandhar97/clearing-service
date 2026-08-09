package com.paymentprocessor.clearingservice.service.transport;

/**
 * Signals a failure while transmitting a clearing file. {@code retryable}
 * distinguishes transient faults (timeouts, 5xx, connection resets) from
 * permanent ones (4xx, malformed request) that must not be retried as-is.
 */
public class TransportException extends RuntimeException {

    private final boolean retryable;

    public TransportException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public TransportException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
