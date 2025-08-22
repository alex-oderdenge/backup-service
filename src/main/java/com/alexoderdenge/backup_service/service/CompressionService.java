package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.CompressionException;

import java.nio.file.Path;

/**
 * Service responsible for compressing files and directories into ZIP archives.
 * Following clean architecture principles, this interface defines the contract
 * for compression operations without being tied to any specific implementation.
 */
public interface CompressionService {
    
    /**
     * Compresses a file or directory into a ZIP archive.
     * 
     * @param sourcePath the path to the file or directory to compress
     * @param outputDirectory the directory where the ZIP file should be created
     * @return the path to the created ZIP file
     * @throws CompressionException if compression fails
     */
    Path compressToZip(Path sourcePath, Path outputDirectory) throws CompressionException;
    
    /**
     * Validates that the target cloud path ends with .zip if compression is enabled.
     * 
     * @param cloudPath the cloud destination path
     * @param isCompressionEnabled whether compression is enabled for this backup entry
     * @throws IllegalArgumentException if validation fails
     */
    void validateCloudPathForCompression(String cloudPath, boolean isCompressionEnabled);
}
