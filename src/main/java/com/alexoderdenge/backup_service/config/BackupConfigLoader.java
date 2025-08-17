package com.alexoderdenge.backup_service.config;

import com.alexoderdenge.backup_service.model.BackupConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Slf4j
public class BackupConfigLoader {

    private static final String DEFAULT_CONFIG_PATH = "classpath:backup-config.json";

    @Value("${config:" + DEFAULT_CONFIG_PATH + "}")
    private String configPath;

    @Bean
    public BackupConfig backupConfig() {
        try {
            log.info("Attempting to load backup config from: {}", configPath);
            
            BackupConfig config;

            if (isClasspathConfig(configPath)) {
                config = loadFromClasspath(getClasspathResource(configPath));
                log.info("Loaded backup config from classpath: {}", configPath);
            } else {
                Path resolvedPath = resolvePath(configPath);
                log.info("Resolved config path to: {}", resolvedPath.toAbsolutePath());
                config = loadFromFileSystem(resolvedPath);
                log.info("Loaded backup config from file system: {}", resolvedPath);
            }

            return config;

        } catch (Exception e) {
            log.error("Error loading backup config from '{}'", configPath, e);
            log.error("Please ensure the backup configuration file exists at: {}", configPath);
            log.error("You can create it manually or use the setup instructions in the README");
            throw new IllegalStateException("Failed to load backup config from: " + configPath, e);
        }
    }

    private Path resolvePath(String pathString) {
        // Handle environment variables and user.home
        String resolvedPath = pathString;
        
        // Replace ${HOME} with actual home directory
        if (resolvedPath.contains("${HOME}")) {
            String homeDir = System.getenv("HOME");
            if (homeDir == null) {
                homeDir = System.getProperty("user.home");
            }
            resolvedPath = resolvedPath.replace("${HOME}", homeDir);
        }
        
        // Replace ${user.home} with actual home directory
        if (resolvedPath.contains("${user.home}")) {
            String userHome = System.getProperty("user.home");
            resolvedPath = resolvedPath.replace("${user.home}", userHome);
        }
        
        log.debug("Resolved path '{}' to '{}'", pathString, resolvedPath);
        return Path.of(resolvedPath);
    }

    private boolean isClasspathConfig(String path) {
        return path.startsWith("classpath:");
    }

    private String getClasspathResource(String path) {
        return path.replaceFirst("classpath:", "").trim();
    }

    private BackupConfig loadFromClasspath(String resourceName) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + resourceName);
            }
            return new ObjectMapper().readValue(inputStream, BackupConfig.class);
        }
    }

    private BackupConfig loadFromFileSystem(Path path) throws Exception {
        if (!Files.exists(path)) {
            String homeDir = System.getProperty("user.home");
            String envHome = System.getenv("HOME");
            
            throw new IllegalArgumentException("Backup configuration file not found: " + path.toAbsolutePath() + 
                "\nPlease create the configuration file or check the 'config' property in application.properties" +
                "\nCurrent user.home: " + homeDir +
                "\nCurrent HOME env: " + envHome);
        }
        return new ObjectMapper().readValue(Files.newBufferedReader(path), BackupConfig.class);
    }
}