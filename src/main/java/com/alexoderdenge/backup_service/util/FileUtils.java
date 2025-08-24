package com.alexoderdenge.backup_service.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
public class FileUtils {

    public static boolean pathExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static boolean isDirectory(String path) {
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }

    public static boolean isFile(String path) {
        return Files.isRegularFile(Paths.get(path));
    }


    /**
     * Validates that the source path exists and is accessible
     */
    public static String validateSourcePath(String sourcePath) throws IllegalArgumentException {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Source path is null or empty");
        }

        // Warn if the path contains env-like placeholders
        if (containsAny(sourcePath, "$HOME", "${HOME}", "$USER", "${USER}", "${user.home}", "${user.name}", "~")) {
            log.warn("Source path contains placeholders (e.g., $HOME, ${HOME}, ${user.home}, ~). " +
                    "They will be expanded before validation.");
        }

        // Expand placeholders BEFORE building the Path
        String expanded = expandPlaceholders(sourcePath);

        Path path = Paths.get(expanded);

        // Normalize to absolute
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath();
            log.debug("Converted relative path to absolute: {}", path);
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Source path does not exist: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("No read permission for source path: " + path);
        }

        // For directories, check if we can list contents
        if (Files.isDirectory(path)) {
            try (Stream<Path> ignored = Files.list(path)) {
                // no-op; just verifying access
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot access directory contents: " + path + " - " + e.getMessage());
            }
        }

        log.info("✅ Source path validation passed: {} ({})", path, 
                Files.isDirectory(path) ? "directory" : "file");
        return path.toString();
    }

    /**
     * Expand common placeholders:
     * - $HOME, ${HOME}, ~ -> System.getProperty("user.home")
     * - $USER, ${USER}    -> System.getProperty("user.name")
     * - ${user.home}      -> System.getProperty("user.home")
     * - ${user.name}      -> System.getProperty("user.name")
     */
    private static String expandPlaceholders(String input) {
        String userHome = System.getProperty("user.home");
        String userName = System.getProperty("user.name");

        String out = input;

        // Leading tilde (~ or ~/something) -> user home
        if (out.startsWith("~")) {
            if (out.equals("~")) {
                out = userHome;
            } else if (out.startsWith("~/") || out.startsWith("~\\")) {
                out = userHome + out.substring(1);
            }
        }

        // Java system property style
        out = out.replace("${user.home}", userHome)
                .replace("${user.name}", userName);

        // Env-style (Unix-like)
        out = out.replace("$HOME", userHome)
                .replace("${HOME}", userHome)
                .replace("$USER", userName)
                .replace("${USER}", userName);

        return out;
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) {
            if (s.contains(n)) return true;
        }
        return false;
    }

    /**
     * Creates a temporary directory for compression operations.
     * The directory will be created in the system temp directory.
     * 
     * @param prefix the prefix for the temporary directory name
     * @return the path to the created temporary directory
     * @throws IOException if the directory cannot be created
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        Path tempDir = Files.createTempDirectory(prefix);
        log.debug("Created temporary directory: {}", tempDir);
        return tempDir;
    }

    /**
     * Recursively deletes a directory and all its contents.
     * Use with caution - this operation is irreversible.
     * 
     * @param directory the directory to delete
     * @throws IOException if deletion fails
     */
    public static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((path1, path2) -> -path1.compareTo(path2)) // Reverse order for deletion
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         log.warn("Failed to delete {}: {}", path, e.getMessage());
                     }
                 });
        }
        
        log.debug("Deleted temporary directory: {}", directory);
    }

    /**
     * Validates that the source path exists and is accessible.
     * Updated to work with both files and directories for compression support.
     */
    public static String validateSourcePathForBackup(String sourcePath) throws IllegalArgumentException {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Source path is null or empty");
        }

        // Warn if the path contains env-like placeholders
        if (containsAny(sourcePath, "$HOME", "${HOME}", "$USER", "${USER}", "${user.home}", "${user.name}", "~")) {
            log.warn("Source path contains placeholders (e.g., $HOME, ${HOME}, ${user.home}, ~). " +
                    "They will be expanded before validation.");
        }

        // Expand placeholders BEFORE building the Path
        String expanded = expandPlaceholders(sourcePath);

        Path path = Paths.get(expanded);

        // Normalize to absolute
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath();
            log.debug("Converted relative path to absolute: {}", path);
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Source path does not exist: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("No read permission for source path: " + path);
        }

        // For directories, check accessibility
        if (Files.isDirectory(path)) {
            try (Stream<Path> ignored = Files.list(path)) {
                // no-op; just verifying access
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot access directory contents: " + path + " - " + e.getMessage());
            }
        }

        log.info("✅ Source path validation passed: {} ({})", path, 
                Files.isDirectory(path) ? "directory" : "file");
        return path.toString();
    }
}
