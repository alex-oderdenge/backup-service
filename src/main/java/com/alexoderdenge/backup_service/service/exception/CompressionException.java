package com.alexoderdenge.backup_service.service.exception;

/**
 * Exception thrown when compression operations fail.
 */
public class CompressionException extends Exception {
    
    public CompressionException(String message) {
        super(message);
    }
    
    public CompressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
