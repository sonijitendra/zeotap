package com.ims.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found: %s", resourceType, identifier));
    }
}
