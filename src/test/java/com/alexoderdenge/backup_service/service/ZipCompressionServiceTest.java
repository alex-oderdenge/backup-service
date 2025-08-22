package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.CompressionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ZipCompressionServiceTest {

    private final ZipCompressionService compressionService = new ZipCompressionService();

    @TempDir
    Path tempDir;

    @Test
    void testCompressDirectory() throws IOException, CompressionException {
        // Create test directory structure
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        
        Path subDir = sourceDir.resolve("subdir");
        Files.createDirectories(subDir);
        
        Path file1 = sourceDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");
        
        Files.writeString(file1, "Content of file 1");
        Files.writeString(file2, "Content of file 2");
        
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Compress
        Path zipFile = compressionService.compressToZip(sourceDir, outputDir);
        
        assertNotNull(zipFile);
        assertTrue(Files.exists(zipFile));
        assertTrue(zipFile.getFileName().toString().endsWith(".zip"));
        assertTrue(Files.size(zipFile) > 0);
        
        // Verify ZIP contents
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            assertNotNull(zip.getEntry("file1.txt"));
            assertNotNull(zip.getEntry("subdir/"));
            assertNotNull(zip.getEntry("subdir/file2.txt"));
        }
    }

    @Test
    void testCompressSingleFile() throws IOException, CompressionException {
        // Create test file
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "Test file content");
        
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Compress
        Path zipFile = compressionService.compressToZip(sourceFile, outputDir);
        
        assertNotNull(zipFile);
        assertTrue(Files.exists(zipFile));
        assertTrue(zipFile.getFileName().toString().endsWith(".zip"));
        assertTrue(Files.size(zipFile) > 0);
        
        // Verify ZIP contents
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            assertNotNull(zip.getEntry("test.txt"));
        }
    }

    @Test
    void testCompressNonExistentSource() {
        Path nonExistent = tempDir.resolve("non-existent");
        Path outputDir = tempDir.resolve("output");
        
        assertThrows(CompressionException.class, () -> {
            compressionService.compressToZip(nonExistent, outputDir);
        });
    }

    @Test
    void testValidateCloudPathForCompression_CompressionEnabled() {
        // Should pass - compression enabled and path ends with .zip
        assertDoesNotThrow(() -> {
            compressionService.validateCloudPathForCompression("gdrive:/backup/test.zip", true);
        });
        
        // Should fail - compression enabled but path doesn't end with .zip
        assertThrows(IllegalArgumentException.class, () -> {
            compressionService.validateCloudPathForCompression("gdrive:/backup/test/", true);
        });
    }

    @Test
    void testValidateCloudPathForCompression_CompressionDisabled() {
        // Should pass - compression disabled
        assertDoesNotThrow(() -> {
            compressionService.validateCloudPathForCompression("gdrive:/backup/test/", false);
        });
        
        // Should pass with warning - compression disabled but path ends with .zip
        assertDoesNotThrow(() -> {
            compressionService.validateCloudPathForCompression("gdrive:/backup/test.zip", false);
        });
    }

    @Test
    void testValidateCloudPathForCompression_NullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            compressionService.validateCloudPathForCompression(null, true);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            compressionService.validateCloudPathForCompression("", true);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            compressionService.validateCloudPathForCompression("   ", true);
        });
    }

    @Test
    void testValidateCloudPathForCompression_CaseInsensitive() {
        // Should pass - case insensitive check
        assertDoesNotThrow(() -> {
            compressionService.validateCloudPathForCompression("gdrive:/backup/test.ZIP", true);
        });
        
        assertDoesNotThrow(() -> {
            compressionService.validateCloudPathForCompression("gdrive:/backup/test.Zip", true);
        });
    }
}
