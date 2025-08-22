package com.alexoderdenge.backup_service.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsCompressionTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateZipArchiveFromDirectory() throws IOException {
        // Create a test directory with some files
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.write(sourceDir.resolve("file1.txt"), "Content of file 1".getBytes());
        Files.write(sourceDir.resolve("file2.txt"), "Content of file 2".getBytes());
        
        Path subDir = sourceDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.write(subDir.resolve("file3.txt"), "Content of file 3".getBytes());

        // Create ZIP archive
        Path zipPath = tempDir.resolve("test.zip");
        FileUtils.createZipArchive(sourceDir.toString(), zipPath.toString());

        // Verify ZIP file was created and contains expected entries
        assertTrue(Files.exists(zipPath));
        assertTrue(Files.size(zipPath) > 0);

        // Verify ZIP contents
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                assertTrue(entry.getName().endsWith(".txt"));
            }
            assertEquals(3, entryCount, "ZIP should contain 3 files");
        }
    }

    @Test
    void testCreateZipArchiveFromFile() throws IOException {
        // Create a single test file
        Path sourceFile = tempDir.resolve("test.txt");
        Files.write(sourceFile, "Test file content".getBytes());

        // Create ZIP archive
        Path zipPath = tempDir.resolve("test.zip");
        FileUtils.createZipArchive(sourceFile.toString(), zipPath.toString());

        // Verify ZIP file was created
        assertTrue(Files.exists(zipPath));
        assertTrue(Files.size(zipPath) > 0);

        // Verify ZIP contains the file
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("test.txt", entry.getName());
            
            // Should be only one entry
            assertNull(zis.getNextEntry());
        }
    }

    @Test
    void testGenerateTempZipPath() {
        String sourcePath = "/home/user/documents";
        String tempZipPath = FileUtils.generateTempZipPath(sourcePath);
        
        assertTrue(tempZipPath.contains("backup-documents-"));
        assertTrue(tempZipPath.endsWith(".zip"));
        assertTrue(tempZipPath.contains(System.getProperty("java.io.tmpdir")));
    }

    @Test
    void testValidateCloudPathForCompression() {
        // Should pass - compression enabled with .zip extension
        assertDoesNotThrow(() -> 
            FileUtils.validateCloudPathForCompression("gdrive:/backup/test.zip", true));
        
        // Should pass - compression disabled without .zip extension
        assertDoesNotThrow(() -> 
            FileUtils.validateCloudPathForCompression("gdrive:/backup/test/", false));
        
        // Should pass - compression disabled with .zip extension (allowed but not required)
        assertDoesNotThrow(() -> 
            FileUtils.validateCloudPathForCompression("gdrive:/backup/test.zip", false));
        
        // Should fail - compression enabled without .zip extension
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            FileUtils.validateCloudPathForCompression("gdrive:/backup/test/", true));
        assertTrue(exception.getMessage().contains("must end with '.zip'"));
        
        // Should fail - compression enabled with null cloud path
        assertThrows(IllegalArgumentException.class, () ->
            FileUtils.validateCloudPathForCompression(null, true));
    }
}
