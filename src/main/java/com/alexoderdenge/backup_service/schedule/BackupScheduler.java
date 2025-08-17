package com.alexoderdenge.backup_service.schedule;

import com.alexoderdenge.backup_service.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final BackupService backupService;

    @Value("${config:classpath:backup-config.json}")
    private String configPath;

    @Value("${rclone.config-path:}")
    private String rcloneConfigPath;

    // Run every 24h (midnight) by default
    @Scheduled(cron = "${backup.schedule-cron:0 0 0 * * *}")
    public void runScheduledBackup() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("🕒 Scheduled backup triggered at: {}", timestamp);
        log.info("📁 Using backup config: {}", configPath);
        log.info("🔧 Using rclone config: {}", rcloneConfigPath.isEmpty() ? "default" : rcloneConfigPath);
        
        backupService.runBackup();
    }
}