package com.alexoderdenge.backup_service.model;

import lombok.Data;

import java.util.List;

@Data
public class BackupConfig {
    private List<BackupEntry> backupEntries;
    private String scheduleCron;
    private String cloudProvider;

    @Data
    public static class BackupEntry {
        private String localPath;
        private String cloudPath;
        private boolean compress = false; // Default to false for backward compatibility
    }
}
