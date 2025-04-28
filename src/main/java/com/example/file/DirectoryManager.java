package com.example.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryManager {

    private final Path inputDir;
    private final Path processedDir;
    private final Path failedDir;

    public DirectoryManager(String basePath) throws IOException {
        Path base = Paths.get(basePath);
        inputDir = base.resolve("input");
        processedDir = base.resolve("processed");
        failedDir = base.resolve("failed");

        Files.createDirectories(inputDir);
        Files.createDirectories(processedDir);
        Files.createDirectories(failedDir);
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
}