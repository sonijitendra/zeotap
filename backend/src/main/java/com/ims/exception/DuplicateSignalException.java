package com.ims.exception;

/**
 * Thrown when a duplicate signal is detected (idempotency check).
 */
public class DuplicateSignalException extends RuntimeException {

    public DuplicateSignalException(String signalId) {
        super(String.format("Duplicate signal detected: %s", signalId));
    }
}
