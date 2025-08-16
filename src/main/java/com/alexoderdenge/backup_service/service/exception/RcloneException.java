package com.alexoderdenge.backup_service.service.exception;

public class RcloneException extends RuntimeException {
    
    public RcloneException(String message) {
        super(message);
    }
    
    public RcloneException(String message, Throwable cause) {
        super(message, cause);
    }
}
