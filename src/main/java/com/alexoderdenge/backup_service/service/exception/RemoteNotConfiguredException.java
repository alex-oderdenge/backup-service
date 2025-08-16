package com.alexoderdenge.backup_service.service.exception;

public class RemoteNotConfiguredException extends RcloneException {
    
    private final String remoteName;
    
    public RemoteNotConfiguredException(String remoteName) {
        super(String.format("Remote '%s' is not configured in rclone. Please configure it first:\n" +
                           "1. Run: rclone config\n" +
                           "2. Choose 'n' for new remote\n" +
                           "3. Enter a name for your remote (e.g., '%s')\n" +
                           "4. Select your cloud storage provider\n" +
                           "5. Follow the configuration steps for your provider", 
                           remoteName, remoteName));
        this.remoteName = remoteName;
    }
    
    public String getRemoteName() {
        return remoteName;
    }
}
