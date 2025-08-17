package com.alexoderdenge.backup_service.service;

import com.alexoderdenge.backup_service.service.exception.RcloneException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RcloneCloudProvider implements CloudProvider {

    private final RcloneValidator rcloneValidator;

    @Value("${rclone.config-path:}") // Empty by default
    private String rcloneConfigPath;

    @Override
    public void backup(String source, String destination) throws RcloneException {
        // Extract and validate remote configuration
        String remoteName = rcloneValidator.extractRemoteName(destination);
        rcloneValidator.validateRemoteConfiguration(remoteName);

        List<String> command = new ArrayList<>();
        command.add("rclone");
        command.add("copy");
        command.add(source);
        command.add(destination);

        if (!rcloneConfigPath.isBlank()) {
            command.add("--config");
            command.add(rcloneConfigPath);
            log.debug("Using custom rclone config: {}", rcloneConfigPath);
        } else {
            log.debug("Using default rclone config location");
        }

        command.add("--verbose");

        log.info("Running Rclone command: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.inheritIO().start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Rclone backup failed from {} to {} with exit code {}", source, destination, exitCode);
                throw new RcloneException("Rclone backup failed with exit code " + exitCode);
            }

            log.info("✅ Successfully backed up from {} to {}", source, destination);
        } catch (IOException e) {
            log.error("Failed to execute rclone backup command", e);
            throw new RcloneException("Failed to execute rclone backup command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RcloneException("Rclone backup was interrupted", e);
        }
    }
}