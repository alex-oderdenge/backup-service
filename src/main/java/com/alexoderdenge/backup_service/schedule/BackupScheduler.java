package com.alexoderdenge.backup_service.schedule;

import com.alexoderdenge.backup_service.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final BackupService backupService;

    // Run every 24h (midnight) by default
    @Scheduled(cron = "${backup.schedule-cron:0 0 0 * * *}")
    public void runScheduledBackup() {
        log.info("Scheduled backup started.");
        backupService.runBackup();
    }
}