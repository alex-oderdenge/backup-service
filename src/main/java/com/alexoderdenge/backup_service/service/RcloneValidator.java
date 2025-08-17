package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.RcloneException;
import com.alexoderdenge.backup_service.service.exception.RemoteNotConfiguredException;
import com.alexoderdenge.backup_service.service.exception.RcloneNotInstalledException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class RcloneValidator {

    @Value("${rclone.config-path:}")
    private String rcloneConfigPath;

    /**
     * Validates that rclone is installed and accessible
     */
    public void validateRcloneInstallation() {
        try {
            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("version");

            if (!rcloneConfigPath.isBlank()) {
                command.add("--config");
                command.add(rcloneConfigPath);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RcloneNotInstalledException();
            }

            log.debug("Rclone installation validated successfully");
        } catch (IOException e) {
            log.error("Failed to execute rclone command", e);
            throw new RcloneNotInstalledException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RcloneException("Rclone validation was interrupted", e);
        }
    }

    /**
     * Validates that a remote is configured in rclone
     * @param remoteName the name of the remote to validate
     */
    public void validateRemoteConfiguration(String remoteName) {
        try {
            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("listremotes");

            if (!rcloneConfigPath.isBlank()) {
                command.add("--config");
                command.add(rcloneConfigPath);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean remoteFound = false;
                
                while ((line = reader.readLine()) != null) {
                    // rclone listremotes outputs remotes in format "remoteName:"
                    if (line.trim().equals(remoteName + ":")) {
                        remoteFound = true;
                        break;
                    }
                }
                
                int exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    log.error("Failed to list rclone remotes, exit code: {}", exitCode);
                    throw new RcloneException("Failed to validate remote configuration");
                }
                
                if (!remoteFound) {
                    throw new RemoteNotConfiguredException(remoteName);
                }
            }

            log.debug("Remote '{}' configuration validated successfully", remoteName);
        } catch (IOException e) {
            log.error("Failed to execute rclone listremotes command", e);
            throw new RcloneException("Failed to validate remote configuration", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RcloneException("Remote validation was interrupted", e);
        }
    }

    /**
     * Extracts remote name from a destination path
     * @param destination the destination path (e.g., "remoteName:path/to/destination")
     * @return the remote name
     */
    public String extractRemoteName(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination cannot be null or empty");
        }
        
        // Pattern to match remote name in format "remoteName:path"
        Pattern pattern = Pattern.compile("^([^:]+):(.+)$");
        Matcher matcher = pattern.matcher(destination.trim());
        
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid destination format. Expected format: 'remoteName:path'");
        }
        
        return matcher.group(1);
    }
}
