package com.alexoderdenge.backup_service.controller;

import com.alexoderdenge.backup_service.service.BackupService;
import com.alexoderdenge.backup_service.service.RcloneValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final BackupService backupService;
    private final RcloneValidator rcloneValidator;

    @Value("${config:classpath:backup-config.json}")
    private String configPath;

    @Value("${rclone.config-path:}")
    private String rcloneConfigPath;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runBackupNow() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("🚀 Manual backup triggered via API at: {}", timestamp);
        log.info("📁 Using backup config: {}", configPath);
        log.info("🔧 Using rclone config: {}", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Backup started successfully");
        response.put("timestamp", timestamp);
        response.put("configFile", configPath);
        response.put("rcloneConfig", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
        
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
        log.info("🔍 Rclone validation requested via API");
        log.info("📁 Using backup config: {}", configPath);
        log.info("🔧 Using rclone config: {}", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            rcloneValidator.validateRcloneInstallation();
            response.put("rcloneInstalled", true);
            response.put("message", "Rclone is properly installed");
            response.put("configFile", configPath);
            response.put("rcloneConfig", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
        } catch (Exception e) {
            response.put("rcloneInstalled", false);
            response.put("message", e.getMessage());
            response.put("configFile", configPath);
            response.put("rcloneConfig", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
            return ResponseEntity.status(503).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/remote/{remoteName}")
    public ResponseEntity<Map<String, Object>> validateRemote(@PathVariable String remoteName) {
        log.info("🔍 Remote validation requested for: {}", remoteName);
        log.info("📁 Using backup config: {}", configPath);
        log.info("🔧 Using rclone config: {}", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            rcloneValidator.validateRemoteConfiguration(remoteName);
            response.put("remoteConfigured", true);
            response.put("remoteName", remoteName);
            response.put("message", "Remote is properly configured");
            response.put("configFile", configPath);
            response.put("rcloneConfig", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
        } catch (Exception e) {
            response.put("remoteConfigured", false);
            response.put("remoteName", remoteName);
            response.put("message", e.getMessage());
            response.put("configFile", configPath);
            response.put("rcloneConfig", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}