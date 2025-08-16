# 🗄️ Backup Service

A Spring Boot application for scheduled file backups to the cloud using [Rclone](https://rclone.org/), supporting configurable backup paths and cloud destinations like Google Drive.

---

## 🚀 Features

- 🔁 **Automatic scheduled backups**
- 🛠️ **Customizable source and destination paths**
- ☁️ **Supports any Rclone-compatible cloud (GDrive, Dropbox, S3, etc.)**
- 🔒 **Configurable via CLI or bundled config file**
- 🧩 **Extensible cloud provider interface**
- ⚙️ **Optional support for custom `rclone.conf`**
- ✅ **Automatic validation of rclone installation and remote configuration**
- 🛡️ **Comprehensive error handling with helpful error messages**

---

## 📁 Project Structure

```
src/main/java/com/example/backup/
├── BackupServiceApplication.java
├── config/              # Config loading from JSON or CLI
├── controller/          # REST API for triggering backups
├── domain/              # (Reserved for business logic objects)
├── model/               # Configuration models
├── schedule/            # Scheduled backup task
├── service/             # Backup + cloud provider logic
│   ├── exception/       # Custom exceptions for error handling
│   └── RcloneValidator.java # Validation logic
├── util/                # File utilities
```

---

## ⚙️ Configuration

The application loads configuration from a `JSON` file specifying:

- Paths to back up from the local machine
- Destination paths in the cloud (via Rclone)
- Cron expression for scheduling
- Cloud provider name (only `rclone` supported initially)

### 🧾 Sample `backup-config.json`

```json
{
  "backupEntries": [
    {
      "localPath": "/home/user/documents/",
      "cloudPath": "gdrive:/backup/documents/"
    },
    {
      "localPath": "/home/user/photos/",
      "cloudPath": "dropbox:/backup/photos/"
    }
  ],
  "scheduleCron": "0 0 0 * * *",
  "cloudProvider": "rclone"
}
```

---

## 🏃 Running the App

### 1. **Build the JAR with Maven**
```bash
mvn clean package
```

### 2. **Run with default config**
```bash
java -jar target/backup-service.jar
```

### 3. **Run with external config file**
```bash
java -jar target/backup-service.jar --config=/absolute/path/to/backup-config.json
```

You can also use an environment variable:
```bash
CONFIG=/absolute/path/backup-config.json java -jar target/backup-service.jar
```

### 4. **Optional: Custom `rclone.conf`**
If you're not using the default `~/.config/rclone/rclone.conf`, you can set a custom path:

```bash
java -jar target/backup-service.jar \
  --config=/home/user/backup-config.json \
  --rclone.config-path=/home/user/custom-rclone.conf
```

If not provided, it will fallback to Rclone's default location automatically.

**Default Configuration Path**: The application looks for `rclone.conf` at `$HOME/.config/backup-service/rclone.conf` by default, as specified in `application.properties`.

---

## 🕒 Scheduling

Backup jobs are run using the cron expression from the config file:
```json
"scheduleCron": "0 0 0 * * *" // Every day at midnight
```

You can override it in `application.yml` as a fallback:
```yaml
backup:
  schedule-cron: "0 0 3 * * *"
```

---

## 🌐 REST API

### Manual Backup Trigger
```http
POST /api/backup/run
```

### Rclone Installation Validation
```http
GET /api/backup/validate
```

### Remote Configuration Validation
```http
GET /api/backup/validate/remote/{remoteName}
```

#### Example with curl:
```bash
# Trigger backup
curl -X POST http://localhost:8082/api/backup/run

# Validate rclone installation
curl -X GET http://localhost:8082/api/backup/validate

# Validate specific remote
curl -X GET http://localhost:8082/api/backup/validate/remote/gdrive
```

---

## ✅ Validation & Error Handling

The application now includes comprehensive validation and error handling:

### Rclone Installation Validation
- Automatically checks if rclone is installed and accessible
- Provides helpful installation instructions if not found
- Validates during backup execution and via API endpoints

### Remote Configuration Validation
- Validates that specified remotes exist in rclone configuration
- Extracts remote names from destination paths (e.g., `gdrive:/path/` → `gdrive`)
- Provides step-by-step configuration instructions for missing remotes

### Error Messages
The application provides clear, actionable error messages:

**Rclone Not Installed:**
```
Rclone is not installed on this system. Please install rclone first:
1. Visit https://rclone.org/downloads/
2. Download and install rclone for your operating system
3. Ensure rclone is available in your system PATH
```

**Remote Not Configured:**
```
Remote 'myRemote' is not configured in rclone. Please configure it first:
1. Run: rclone config
2. Choose 'n' for new remote
3. Enter a name for your remote (e.g., 'myRemote')
4. Select your cloud storage provider
5. Follow the configuration steps for your provider
```

### Smart Error Handling
The application intelligently handles different types of errors:

- **Rclone Not Installed**: Stops all backups immediately since all will fail
- **Remote Not Configured**: Continues with other backups (per-remote issue)
- **Individual Backup Failures**: Continues with remaining backups (temporary issues)

---

## ☁️ Cloud Provider Setup

This app uses [Rclone](https://rclone.org/downloads/) under the hood:

1. Install Rclone
2. Run `rclone config` to set up your cloud remotes (e.g., `gdrive`, `dropbox`, `onedrive`, etc.)
3. Use those names in your `cloudPath` like `gdrive:/folder/` or `dropbox:/path/`
4. Rclone will use the default config in `~/.config/rclone/rclone.conf` unless overridden

You can define multiple clouds in a single `rclone.conf`, for example:

```ini
[gdrive]
type = drive
token = ...

[dropbox]
type = dropbox
token = ...
```

---

## 📁 Manual Setup Required

**Important**: The backup-service configuration folder and files are NOT created automatically. You must create them manually.

### 1. **Create the backup-service configuration directory**
```bash
mkdir -p $HOME/.config/backup-service
```

### 2. **Create the backup configuration file**
```bash
cat > $HOME/.config/backup-service/backup-config.json << 'EOF'
{
  "backupEntries": [
    {
      "localPath": "$HOME/test/",
      "cloudPath": "gdrive:test/"
    }
  ],
  "scheduleCron": "0 0 0 * * *",
  "cloudProvider": "rclone"
}
EOF
```

### 3. **Create a test file and directory**
```bash
# Create test directory
mkdir -p $HOME/test

# Create a test file
echo "This is a test file for backup service" > $HOME/test/test.txt

# Verify the file was created
ls -la $HOME/test/
```

### 4. **Complete setup example**
Here's a complete setup script that creates everything needed for testing:

```bash
#!/bin/bash

# Create backup-service config directory
mkdir -p $HOME/.config/backup-service

# Create test directory and file
mkdir -p $HOME/test
echo "This is a test file for backup service - created at $(date)" > $HOME/test/test.txt

# Create backup configuration
cat > $HOME/.config/backup-service/backup-config.json << 'EOF'
{
  "backupEntries": [
    {
      "localPath": "$HOME/test/",
      "cloudPath": "gdrive:test/"
    }
  ],
  "scheduleCron": "0 0 0 * * *",
  "cloudProvider": "rclone"
}
EOF

# Verify setup
echo "✅ Setup complete!"
echo "📁 Test directory: $HOME/test/"
echo "📄 Test file: $HOME/test/test.txt"
echo "⚙️  Config file: $HOME/.config/backup-service/backup-config.json"
echo ""
echo "🔧 Next steps:"
echo "1. Configure rclone: rclone config"
echo "2. Create a 'gdrive' remote"
echo "3. Run the backup service"
```

### 5. **Run the application with your config**
```bash
java -jar target/backup-service.jar --config=$HOME/.config/backup-service/backup-config.json
```

**Note**: Make sure you have already configured rclone with a `gdrive` remote before running the backup service. The application will validate this and provide helpful error messages if the remote is not configured.

---

## 🧪 Testing locally

You can manually run a one-off backup during development:
```java
@Component
public class StartupRunner implements CommandLineRunner {
    @Autowired
    BackupService backupService;

    public void run(String... args) {
        backupService.runBackup();
    }
}
```

---

## 🔐 Security

No authentication is added yet. To protect the HTTP endpoint:
- Restrict to localhost only
- Or add Spring Security with a password/token

---

## 📦 TODO / Improvements

- Add support for S3, WebDAV, etc.
- Watch local folders for real-time backup (optional)
- Upload summary logs to cloud
- Add web UI dashboard for status/configuration
- ~~Validate `cloudPath` remotes exist in `rclone.conf`~~ ✅ **COMPLETED**

---

## 🧑‍💻 Maintainer

**Alex Oderdenge**  
Feel free to fork, extend, or request enhancements.

---

## 📄 License

MIT – do what you want, but backup responsibly.
