package com.alexoderdenge.backup_service.controller;

import com.alexoderdenge.backup_service.service.BackupService;
import com.alexoderdenge.backup_service.service.RcloneValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final BackupService backupService;
    private final RcloneValidator rcloneValidator;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runBackupNow() {
        log.info("Manual backup triggered via API.");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Backup started successfully");
        
        try {
            backupService.runBackup();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Backup failed", e);
            response.put("status", "error");
            response.put("message", "Backup failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateRcloneSetup() {
        log.info("Rclone validation requested via API.");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            rcloneValidator.validateRcloneInstallation();
            response.put("rcloneInstalled", true);
            response.put("message", "Rclone is properly installed");
        } catch (Exception e) {
            response.put("rcloneInstalled", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/remote/{remoteName}")
    public ResponseEntity<Map<String, Object>> validateRemote(@PathVariable String remoteName) {
        log.info("Remote validation requested for: {}", remoteName);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            rcloneValidator.validateRemoteConfiguration(remoteName);
            response.put("remoteConfigured", true);
            response.put("remoteName", remoteName);
            response.put("message", "Remote is properly configured");
        } catch (Exception e) {
            response.put("remoteConfigured", false);
            response.put("remoteName", remoteName);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}