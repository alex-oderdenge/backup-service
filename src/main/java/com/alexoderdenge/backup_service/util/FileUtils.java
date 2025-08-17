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
        File file = new File(path);
        return file.exists() && file.isFile();
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

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Source path is not a directory: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("No read permission for source path: " + path);
        }

        // Check if directory is accessible by trying to list contents
        try (Stream<Path> ignored = Files.list(path)) {
            // no-op; just verifying access
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot access directory contents: " + path + " - " + e.getMessage());
        }

        log.info("✅ Source path validation passed: {}", path);
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
}
