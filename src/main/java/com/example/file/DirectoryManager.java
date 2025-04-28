package com.example.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryManager {

    private final Path baseDir;
    private final Path inputDir;
    private final Path processedDir;
    private final Path failedDir;
    private final Path logsDir; // Added logs directory path

    public DirectoryManager(String basePath) throws IOException {
        this.baseDir = Paths.get(basePath);
        this.inputDir = baseDir.resolve("input");
        this.processedDir = baseDir.resolve("processed");
        this.failedDir = baseDir.resolve("failed");
        this.logsDir = baseDir.resolve("logs"); // Resolve logs directory

        // Create all directories
        Files.createDirectories(inputDir);
        Files.createDirectories(processedDir);
        Files.createDirectories(failedDir);
        Files.createDirectories(logsDir); // Create logs directory
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getInputDir() {
        return inputDir;
    }

    public Path getProcessedDir() {
        return processedDir;
    }

    public Path getFailedDir() {
        return failedDir;
    }

    public Path getLogsDir() { // Getter for logs directory
        return logsDir;
    }
}
