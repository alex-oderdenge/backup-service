package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.model.BackupConfig;
import com.alexoderdenge.backup_service.service.exception.CompressionException;
import com.alexoderdenge.backup_service.service.exception.RemoteNotConfiguredException;
import com.alexoderdenge.backup_service.service.exception.RcloneException;
import com.alexoderdenge.backup_service.service.exception.RcloneNotInstalledException;
import com.alexoderdenge.backup_service.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final CloudProvider cloudProvider;
    private final BackupConfig config;
    private final RcloneValidator rcloneValidator;
    private final CompressionService compressionService;

    @Value("${config:classpath:backup-config.json}")
    private String configPath;

    @Value("${rclone.config-path:}")
    private String rcloneConfigPath;

    public void runBackup() {
        logBackupStart();

        if (!validateRcloneInstallation()) {
            return; // Exit early if rclone is not properly installed
        }

        processBackupEntries();

        log.info("=== Backup Task Completed ===");
    }

    private void logBackupStart() {
        log.info("=== Starting Backup Task ===");
        log.info("📁 Backup config file: {}", configPath);
        log.info("🔧 Rclone config file: {}", rcloneConfigPath.isEmpty() ? "default (~/.config/rclone/rclone.conf)" : rcloneConfigPath);
        log.info("📋 Backup entries to process: {}", config.getBackupEntries().size());
    }

    private boolean validateRcloneInstallation() {
        try {
            rcloneValidator.validateRcloneInstallation();
            return true;
        } catch (RcloneNotInstalledException e) {
            log.error("Rclone is not installed: {}", e.getMessage());
            log.error("Stopping backup task - all backups will fail without rclone installed");
            return false;
        } catch (RcloneException e) {
            log.error("Failed to validate rclone installation: {}", e.getMessage());
            return false;
        }
    }

    private void processBackupEntries() {
        for (BackupConfig.BackupEntry entry : config.getBackupEntries()) {
            processIndividualBackupEntry(entry);
        }
    }

    private void processIndividualBackupEntry(BackupConfig.BackupEntry entry) {
        try {
            log.info("🔍 Processing backup entry: {} -> {} (compress: {})",
                    entry.getLocalPath(), entry.getCloudPath(), entry.isCompress());

            if (!validateBackupEntry(entry)) {
                return; // Skip this entry if validation fails
            }

            String sourceToBackup = handleCompressionIfEnabled(entry);
            if (sourceToBackup == null) {
                return; // Skip this entry if compression fails
            }
            boolean isFile = pathIsFile(sourceToBackup);

            performBackup(sourceToBackup, entry.getCloudPath(), isFile);

        } catch (RemoteNotConfiguredException e) {
            log.error("Remote '{}' is not configured: {}", e.getRemoteName(), e.getMessage());
            // Continue with other backups - this is a per-remote issue
        } catch (RcloneException e) {
            log.error("Rclone backup failed for {} → {}: {}",
                    entry.getLocalPath(), entry.getCloudPath(), e.getMessage());
            // Continue with other backups - this might be a temporary issue
        } catch (Exception e) {
            log.error("Unexpected error during backup of {} → {}: {}",
                    entry.getLocalPath(), entry.getCloudPath(), e.getMessage());
            // Continue with other backups - log and move on
        }
    }

    private boolean validateBackupEntry(BackupConfig.BackupEntry entry) {
        try {
            // Validate cloud path for compression requirements
            compressionService.validateCloudPathForCompression(entry.getCloudPath(), entry.isCompress());

            // Validate local path
            if (entry.getLocalPath() == null || entry.getLocalPath().isEmpty()) {
                throw new IllegalArgumentException("Local path cannot be null or empty");
            }

            // Validate source path exists, permissions, and is accessible
            String validatedSourcePath = FileUtils.validateSourcePath(entry.getLocalPath());
            entry.setLocalPath(validatedSourcePath);

            return true;

        } catch (IllegalArgumentException e) {
            log.error("Invalid backup entry configuration for {}: {}", entry.getLocalPath(), e.getMessage());
            log.debug("Stack trace: ", e);
            return false;
        }
    }

    private String handleCompressionIfEnabled(BackupConfig.BackupEntry entry) {
        if (!entry.isCompress()) {
            return entry.getLocalPath(); // No compression needed
        }

        return performCompression(entry.getLocalPath());
    }

    private String performCompression(String localPath) {
        Path tempDirectory = null;
        try {
            log.info("🗜️ Compression enabled for: {}", localPath);

            // Create temporary directory for compressed files
            tempDirectory = FileUtils.createTempDirectory("backup-compression-");

            // Compress the source
            Path compressedFile = compressionService.compressToZip(
                    Paths.get(localPath), tempDirectory);

            log.info("✅ Compressed {} to {}", localPath, compressedFile);
            return compressedFile.toString();

        } catch (CompressionException | IOException e) {
            log.error("❌ Compression failed for {}: {}", localPath, e.getMessage());
            cleanupTempDirectory(tempDirectory);
            return null; // Indicate compression failure
        }
    }

    private void performBackup(String sourceToBackup, String cloudPath, boolean isFile) throws RcloneException {
        Path tempDirectory = null;

        // Extract temp directory from source path if it's a compressed file
        if (sourceToBackup.contains("backup-compression-")) {
            tempDirectory = Paths.get(sourceToBackup).getParent();
        }

        try {
            log.info("🔄 Backing up: {} -> {}", sourceToBackup, cloudPath);
            cloudProvider.backup(sourceToBackup, cloudPath, isFile);
            log.info("✅ Successfully backed up: {} -> {}", sourceToBackup, cloudPath);

        } finally {
            cleanupTempDirectory(tempDirectory);
        }
    }

    private void cleanupTempDirectory(Path tempDirectory) {
        if (tempDirectory != null) {
            try {
                FileUtils.deleteDirectoryRecursively(tempDirectory);
                log.debug("🧹 Cleaned up temporary directory: {}", tempDirectory);
            } catch (IOException e) {
                log.warn("Failed to cleanup temporary directory {}: {}",
                        tempDirectory, e.getMessage());
            }
        }
    }

    private boolean pathIsFile(String path) {
        return path != null && !path.isEmpty() && !path.endsWith("/");
    }
}
