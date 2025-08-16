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
            BackupConfig config;

            if (isClasspathConfig(configPath)) {
                config = loadFromClasspath(getClasspathResource(configPath));
                log.info("Loaded backup config from classpath: {}", configPath);
            } else {
                config = loadFromFileSystem(Path.of(configPath));
                log.info("Loaded backup config from file system: {}", configPath);
            }

            return config;

        } catch (Exception e) {
            log.error("Error loading backup config from '{}'", configPath, e);
            throw new IllegalStateException("Failed to load backup config", e);
        }
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
            throw new IllegalArgumentException("File not found: " + path.toAbsolutePath());
        }
        return new ObjectMapper().readValue(Files.newBufferedReader(path), BackupConfig.class);
    }
}