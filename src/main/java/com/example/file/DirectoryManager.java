package com.example.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryManager {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryManager.class);

    private final Path baseDir;
    private final Path inputDir;
    private final Path logsDir;
    private final Path binDir; // Keep bin alongside others
    private final Path processedDir; // Alongside others
    private final Path failedDir;    // Alongside others


    public DirectoryManager(String basePath) throws IOException {
        this.baseDir = Paths.get(basePath);
        // Define all directories directly under baseDir
        this.inputDir = baseDir.resolve("input");
        this.logsDir = baseDir.resolve("logs");
        this.binDir = baseDir.resolve("bin"); // Still creating bin, but alongside
        this.processedDir = baseDir.resolve("processed"); // Now alongside
        this.failedDir = baseDir.resolve("failed");       // Now alongside

        logger.info("Ensuring directory structure exists under base path: {}", baseDir.toAbsolutePath());

        // Create all directories directly under baseDir
        Files.createDirectories(inputDir);
        logger.debug("Ensured directory exists: {}", inputDir.toAbsolutePath());
        Files.createDirectories(logsDir);
        logger.debug("Ensured directory exists: {}", logsDir.toAbsolutePath());
        Files.createDirectories(binDir);
        logger.debug("Ensured directory exists: {}", binDir.toAbsolutePath());
        Files.createDirectories(processedDir);
        logger.debug("Ensured directory exists: {}", processedDir.toAbsolutePath());
        Files.createDirectories(failedDir);
        logger.debug("Ensured directory exists: {}", failedDir.toAbsolutePath());
    }

    // Getters remain the same, returning the correct Path objects
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

    public Path getLogsDir() {
        return logsDir;
    }

    public Path getBinDir() {
        return binDir;
    }
}
