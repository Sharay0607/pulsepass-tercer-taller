package com.pulsepass.exception;

/**
 * Conflicto de unicidad. Ejemplo: "Username already exists: andrea".
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
