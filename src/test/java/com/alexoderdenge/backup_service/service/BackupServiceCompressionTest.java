package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.model.BackupConfig;
import com.alexoderdenge.backup_service.service.exception.CompressionException;
import com.alexoderdenge.backup_service.service.exception.RcloneException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceCompressionTest {

    @Mock
    private CloudProvider cloudProvider;

    @Mock
    private RcloneValidator rcloneValidator;

    @Mock
    private CompressionService compressionService;

    private BackupService backupService;
    private BackupConfig config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = new BackupConfig();
        backupService = new BackupService(cloudProvider, config, rcloneValidator, compressionService);
    }

    @Test
    void testRunBackup_WithCompression() throws Exception {
        // Create test source directory
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("test.txt"), "test content");

        // Create test compressed file
        Path tempCompressDir = tempDir.resolve("temp-compress");
        Files.createDirectories(tempCompressDir);
        Path compressedFile = tempCompressDir.resolve("source_123.zip");
        Files.writeString(compressedFile, "compressed content");

        // Setup backup entry with compression
        BackupConfig.BackupEntry entry = new BackupConfig.BackupEntry();
        entry.setLocalPath(sourceDir.toString());
        entry.setCloudPath("gdrive:/backup/test.zip");
        entry.setCompress(true);

        config.setBackupEntries(Arrays.asList(entry));

        // Mock behaviors
        doNothing().when(rcloneValidator).validateRcloneInstallation();
        doNothing().when(compressionService).validateCloudPathForCompression(anyString(), anyBoolean());
        when(compressionService.compressToZip(any(), any())).thenReturn(compressedFile);
        doNothing().when(cloudProvider).backup(anyString(), anyString());

        // Execute
        backupService.runBackup();

        // Verify compression service was called
        verify(compressionService).validateCloudPathForCompression("gdrive:/backup/test.zip", true);
        verify(compressionService).compressToZip(any(), any());
        verify(cloudProvider).backup(eq(compressedFile.toString()), eq("gdrive:/backup/test.zip"));
    }

    @Test
    void testRunBackup_WithoutCompression() throws Exception {
        // Create test source directory
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("test.txt"), "test content");

        // Setup backup entry without compression
        BackupConfig.BackupEntry entry = new BackupConfig.BackupEntry();
        entry.setLocalPath(sourceDir.toString());
        entry.setCloudPath("gdrive:/backup/test/");
        entry.setCompress(false);

        config.setBackupEntries(Arrays.asList(entry));

        // Mock behaviors
        doNothing().when(rcloneValidator).validateRcloneInstallation();
        doNothing().when(compressionService).validateCloudPathForCompression(anyString(), anyBoolean());
        doNothing().when(cloudProvider).backup(anyString(), anyString());

        // Execute
        backupService.runBackup();

        // Verify compression service validation was called but not compression
        verify(compressionService).validateCloudPathForCompression("gdrive:/backup/test/", false);
        verify(compressionService, never()).compressToZip(any(), any());
        verify(cloudProvider).backup(eq(sourceDir.toString()), eq("gdrive:/backup/test/"));
    }

    @Test
    void testRunBackup_CompressionFailure() throws Exception {
        // Create test source directory
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("test.txt"), "test content");

        // Setup backup entry with compression
        BackupConfig.BackupEntry entry = new BackupConfig.BackupEntry();
        entry.setLocalPath(sourceDir.toString());
        entry.setCloudPath("gdrive:/backup/test.zip");
        entry.setCompress(true);

        config.setBackupEntries(Arrays.asList(entry));

        // Mock behaviors
        doNothing().when(rcloneValidator).validateRcloneInstallation();
        doNothing().when(compressionService).validateCloudPathForCompression(anyString(), anyBoolean());
        when(compressionService.compressToZip(any(), any()))
                .thenThrow(new CompressionException("Compression failed"));

        // Execute
        backupService.runBackup();

        // Verify compression was attempted but backup was not called due to failure
        verify(compressionService).compressToZip(any(), any());
        verify(cloudProvider, never()).backup(anyString(), anyString());
    }

    @Test
    void testRunBackup_InvalidCloudPath() throws Exception {
        // Create test source directory
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        // Setup backup entry with invalid cloud path for compression
        BackupConfig.BackupEntry entry = new BackupConfig.BackupEntry();
        entry.setLocalPath(sourceDir.toString());
        entry.setCloudPath("gdrive:/backup/test/"); // Should end with .zip
        entry.setCompress(true);

        config.setBackupEntries(Arrays.asList(entry));

        // Mock behaviors
        doNothing().when(rcloneValidator).validateRcloneInstallation();
        doThrow(new IllegalArgumentException("Cloud path must end with '.zip'"))
                .when(compressionService).validateCloudPathForCompression(anyString(), eq(true));

        // Execute
        backupService.runBackup();

        // Verify validation was called but no backup occurred
        verify(compressionService).validateCloudPathForCompression("gdrive:/backup/test/", true);
        verify(compressionService, never()).compressToZip(any(), any());
        verify(cloudProvider, never()).backup(anyString(), anyString());
    }
}
