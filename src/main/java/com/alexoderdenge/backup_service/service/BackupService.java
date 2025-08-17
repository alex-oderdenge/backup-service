package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.model.BackupConfig;
import com.alexoderdenge.backup_service.service.exception.RemoteNotConfiguredException;
import com.alexoderdenge.backup_service.service.exception.RcloneException;
import com.alexoderdenge.backup_service.service.exception.RcloneNotInstalledException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final CloudProvider cloudProvider;
    private final BackupConfig config;
    private final RcloneValidator rcloneValidator;

    @Value("${config:classpath:backup-config.json}")
    private String configPath;

    @Value("${rclone.config-path:}")
    private String rcloneConfigPath;

    public void runBackup() {
        log.info("=== Starting Backup Task ===");
        log.info("📁 Backup config file: {}", configPath);
        log.info("🔧 Rclone config file: {}", rcloneConfigPath.isEmpty() ? "default (~/.config/rclone/rclone.conf)" : rcloneConfigPath);
        log.info("📋 Backup entries to process: {}", config.getBackupEntries().size());

        // Check if rclone is installed before processing any backups
        try {
            // Validate rclone installation first
            rcloneValidator.validateRcloneInstallation();
        } catch (RcloneNotInstalledException e) {
            log.error("Rclone is not installed: {}", e.getMessage());
            log.error("Stopping backup task - all backups will fail without rclone installed");
            return; // Exit early - no point in continuing
        } catch (RcloneException e) {
            log.error("Failed to validate rclone installation: {}", e.getMessage());
            return; // Exit early if we can't even validate rclone
        }

        // Process each backup entry
        for (BackupConfig.BackupEntry entry : config.getBackupEntries()) {
            try {
                log.info("🔄 Backing up: {} -> {}", entry.getLocalPath(), entry.getCloudPath());
                cloudProvider.backup(entry.getLocalPath(), entry.getCloudPath());
            } catch (RemoteNotConfiguredException e) {
                log.error("Remote '{}' is not configured: {}", e.getRemoteName(), e.getMessage());
                // Continue with other backups - this is a per-remote issue
            } catch (RcloneException e) {
                log.error("Rclone backup failed for {} → {}: {}", 
                         entry.getLocalPath(), entry.getCloudPath(), e.getMessage());
                // Continue with other backups - this might be a temporary issue
            }
        }

        log.info("=== Backup Task Completed ===");
    }
}