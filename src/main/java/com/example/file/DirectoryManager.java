package com.example.file;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryManager {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryManager.class);
    public static final String LAST_TWEET_ID_FILENAME = "LAST_TWEET_ID.txt"; // Define filename

    private final Path baseDir;
    private final Path inputDir;
    private final Path logsDir;
    private final Path binDir;
    private final Path processedDir;
    private final Path failedDir;
    private final Path lastTweetIdFile; // Path to the ID file


    public DirectoryManager(String basePath) throws IOException {
        this.baseDir = Paths.get(basePath);
        // Define all directories directly under baseDir
        this.inputDir = baseDir.resolve("input");
        this.logsDir = baseDir.resolve("logs");
        this.binDir = baseDir.resolve("bin");
        this.processedDir = baseDir.resolve("processed");
        this.failedDir = baseDir.resolve("failed");
        // Define the path for the last tweet ID file directly under baseDir
        this.lastTweetIdFile = baseDir.resolve(LAST_TWEET_ID_FILENAME);

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

        // Don't create the ID file itself here, just ensure the base directory exists
        logger.debug("Path for last tweet ID file: {}", lastTweetIdFile.toAbsolutePath());
    }

    @NotNull
    public static String basePathRelativeToJar() {
        // (Implementation remains the same as user provided)
        try {
            File jarFile = new File(DirectoryManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path jarDir = jarFile.getParentFile().toPath();
            Path defaultDataDir = jarDir.resolve("data");
            return defaultDataDir.toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            logger.error("Couldn't create an Url from the file location of that jar", e);
            // Return path string, not URI string
            return Paths.get("data").toAbsolutePath().toString();
        } catch (SecurityException | NullPointerException e) {
            logger.error("Security or NullPointer exception getting code source location", e);
            return Paths.get("data").toAbsolutePath().toString();
        }
    }

    // --- Getters ---
    public Path getBaseDir() { return baseDir; }
    public Path getInputDir() { return inputDir; }
    public Path getProcessedDir() { return processedDir; }
    public Path getFailedDir() { return failedDir; }
    public Path getLogsDir() { return logsDir; }
    public Path getBinDir() { return binDir; }

    /**
     * Gets the Path object for the file storing the last processed tweet ID.
     * @return Path to the LAST_TWEET_ID file.
     */
    public Path getLastTweetIdFile() { // Getter for the ID file path
        return lastTweetIdFile;
    }
}
