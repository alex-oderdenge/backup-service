package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.CompressionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of CompressionService that creates ZIP archives.
 * This service handles both files and directories, creating properly structured ZIP files.
 */
@Service
@Slf4j
public class ZipCompressionService implements CompressionService {

    @Override
    public Path compressToZip(Path sourcePath, Path outputDirectory) throws CompressionException {
        validateInputs(sourcePath, outputDirectory);
        
        String zipFileName = generateZipFileName(sourcePath);
        Path zipFilePath = outputDirectory.resolve(zipFileName);
        
        log.info("🗜️ Compressing {} to {}", sourcePath, zipFilePath);
        
        try {
            createZipFile(sourcePath, zipFilePath);
            log.info("✅ Successfully compressed {} to {} (size: {} bytes)", 
                    sourcePath, zipFilePath, Files.size(zipFilePath));
            return zipFilePath;
        } catch (IOException e) {
            log.error("❌ Failed to compress {} to {}: {}", sourcePath, zipFilePath, e.getMessage());
            throw new CompressionException("Failed to compress " + sourcePath + " to " + zipFilePath, e);
        }
    }

    @Override
    public void validateCloudPathForCompression(String cloudPath, boolean isCompressionEnabled) {
        if (cloudPath == null || cloudPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloud path cannot be null or empty");
        }
        
        if (isCompressionEnabled && !cloudPath.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException(
                "Cloud path must end with '.zip' when compression is enabled. Got: " + cloudPath);
        }
        
        if (!isCompressionEnabled && cloudPath.toLowerCase().endsWith(".zip")) {
            log.warn("⚠️ Cloud path ends with '.zip' but compression is disabled for: {}", cloudPath);
        }
    }

    private void validateInputs(Path sourcePath, Path outputDirectory) throws CompressionException {
        if (!Files.exists(sourcePath)) {
            throw new CompressionException("Source path does not exist: " + sourcePath);
        }
        
        if (!Files.exists(outputDirectory)) {
            try {
                Files.createDirectories(outputDirectory);
                log.debug("Created output directory: {}", outputDirectory);
            } catch (IOException e) {
                throw new CompressionException("Failed to create output directory: " + outputDirectory, e);
            }
        }
        
        if (!Files.isDirectory(outputDirectory)) {
            throw new CompressionException("Output directory is not a directory: " + outputDirectory);
        }
    }

    private String generateZipFileName(Path sourcePath) {
        String baseName = sourcePath.getFileName().toString();
        return baseName + ".zip";
    }

    private void createZipFile(Path sourcePath, Path zipFilePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            if (Files.isDirectory(sourcePath)) {
                compressDirectory(sourcePath, zos);
            } else {
                compressSingleFile(sourcePath, zos);
            }
        }
    }

    private void compressDirectory(Path directory, ZipOutputStream zos) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = directory.relativize(file);
                ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace('\\', '/'));
                zos.putNextEntry(zipEntry);
                Files.copy(file, zos);
                zos.closeEntry();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(directory)) {
                    Path relativePath = directory.relativize(dir);
                    ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace('\\', '/') + "/");
                    zos.putNextEntry(zipEntry);
                    zos.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void compressSingleFile(Path file, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(file.getFileName().toString());
        zos.putNextEntry(zipEntry);
        Files.copy(file, zos);
        zos.closeEntry();
    }
}
