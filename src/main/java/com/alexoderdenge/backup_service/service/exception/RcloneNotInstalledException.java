package com.alexoderdenge.backup_service.service.exception;

public class RcloneNotInstalledException extends RcloneException {
    
    public RcloneNotInstalledException() {
        super("Rclone is not installed on this system. Please install rclone first:\n" +
              "1. Visit https://rclone.org/downloads/\n" +
              "2. Download and install rclone for your operating system\n" +
              "3. Ensure rclone is available in your system PATH");
    }
}
