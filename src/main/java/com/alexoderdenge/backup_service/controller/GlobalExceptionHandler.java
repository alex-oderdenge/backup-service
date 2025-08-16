package com.alexoderdenge.backup_service.controller;

import com.alexoderdenge.backup_service.service.exception.RemoteNotConfiguredException;
import com.alexoderdenge.backup_service.service.exception.RcloneException;
import com.alexoderdenge.backup_service.service.exception.RcloneNotInstalledException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RcloneNotInstalledException.class)
    public ResponseEntity<Map<String, Object>> handleRcloneNotInstalled(RcloneNotInstalledException e) {
        log.error("Rclone not installed: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Rclone not installed");
        response.put("message", e.getMessage());
        response.put("status", "error");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(RemoteNotConfiguredException.class)
    public ResponseEntity<Map<String, Object>> handleRemoteNotConfigured(RemoteNotConfiguredException e) {
        log.error("Remote not configured: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Remote not configured");
        response.put("remoteName", e.getRemoteName());
        response.put("message", e.getMessage());
        response.put("status", "error");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RcloneException.class)
    public ResponseEntity<Map<String, Object>> handleRcloneException(RcloneException e) {
        log.error("Rclone error: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Rclone operation failed");
        response.put("message", e.getMessage());
        response.put("status", "error");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Invalid argument: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid argument");
        response.put("message", e.getMessage());
        response.put("status", "error");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
