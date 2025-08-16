package com.alexoderdenge.backup_service.util;

import java.io.File;

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
}
