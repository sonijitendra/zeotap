package com.ims.exception;

/**
 * Thrown when attempting to close an incident without a completed RCA.
 */
public class RcaRequiredException extends RuntimeException {

    public RcaRequiredException(String incidentId) {
        super(String.format("Root Cause Analysis is required before closing incident %s", incidentId));
    }
}
