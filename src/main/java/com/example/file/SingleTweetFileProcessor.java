package com.example.file;

import com.example.discord.DiscordNotifier;
import com.example.twitch.TwitchUserInfo; // Import Twitch info holder
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional; // Import Optional

/**
 * Handles the processing logic for a single tweet file,
 * including checks, consumption, moving, and per-file logging via MDC.
 */
public class SingleTweetFileProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SingleTweetFileProcessor.class);
    private static final String MDC_KEY = "logFileName";

    private final DiscordNotifier discordNotifier;
    private final Path processedDir;
    private final Path failedDir;
    private final Path binDir;
    private final Optional<TwitchUserInfo> twitchInfo; // Store Twitch info

    // Updated constructor
    public SingleTweetFileProcessor(DirectoryManager directoryManager,
                                    DiscordNotifier discordNotifier,
                                    Optional<TwitchUserInfo> twitchInfo) { // Accept Twitch info
        this.discordNotifier = discordNotifier;
        this.processedDir = directoryManager.getProcessedDir();
        this.failedDir = directoryManager.getFailedDir();
        this.binDir = directoryManager.getBinDir();
        this.twitchInfo = twitchInfo; // Store it
    }

    /**
     * Processes a single input file representing a tweet.
     * Sets up MDC for per-file logging.
     *
     * @param inputFile The tweet file from the input directory.
     */
    public void processFile(File inputFile) {
        String inputFileName = inputFile.getName();
        String logFileName = inputFileName.replace(".txt", "");

        MDC.put(MDC_KEY, logFileName);

        try {
            logger.info("Starting processing for file: {}", inputFileName);

            // --- Check if already processed or failed ---
            Path processedFilePath = processedDir.resolve(inputFileName);
            Path failedFilePath = failedDir.resolve(inputFileName);

            if (Files.exists(processedFilePath)) {
                logger.info("File already exists in processed directory. Skipping.");
                moveDuplicateToBin(inputFile);
                return;
            }

            if (Files.exists(failedFilePath)) {
                logger.info("File already exists in failed directory. Skipping.");
                moveDuplicateToBin(inputFile);
                return;
            }
            // --- End Check ---

            logger.info("Attempting to send to Discord...");
            boolean success;
            try {
                // Pass twitchInfo to consume method
                success = discordNotifier.consume(inputFile, this.twitchInfo);
                if (success) {
                    logger.info("Successfully consumed and queued message for Discord.");
                } else {
                    logger.warn("Discord consumption step indicated failure.");
                }
            } catch (Exception e) {
                logger.error("Exception during Discord consumption: {}", e.getMessage(), e);
                success = false;
            }

            // --- Move File Based on Success ---
            moveFileAfterProcessing(inputFile, success);

        } catch (Exception e) {
            logger.error("Unexpected error processing file {}: {}", inputFileName, e.getMessage(), e);
            try {
                moveFileAfterProcessing(inputFile, false);
            } catch (Exception moveEx) {
                logger.error("Could not move file {} to failed directory after unexpected error: {}", inputFileName, moveEx.getMessage(), moveEx);
            }
        } finally {
            logger.info("Finished processing for file: {}", inputFileName);
            MDC.remove(MDC_KEY); // Clear MDC
        }
    }

    // moveFileAfterProcessing remains the same...
    private void moveFileAfterProcessing(File inputFile, boolean success) {
        String inputFileName = inputFile.getName();
        File targetDir = success ? processedDir.toFile() : failedDir.toFile();
        String targetDirName = success ? "processed" : "failed";
        try {
            FileUtils.moveFileToDirectory(inputFile, targetDir, true);
            logger.info("Moved {} to {} directory: {}", inputFileName, targetDirName, targetDir.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to move file {} to {} directory after processing: {}", inputFileName, targetDirName, e.getMessage(), e);
        }
    }


    // moveDuplicateToBin remains the same...
    private void moveDuplicateToBin(File inputFile) {
        String inputFileName = inputFile.getName();
        File moveFileDest = binDir.resolve(inputFileName).toFile();
        logger.debug("Moving duplicate input file {} to bin directory: {}", inputFileName, binDir.toAbsolutePath());
        try {
            if (!binDir.toFile().exists()) {
                Files.createDirectories(binDir);
            }
            FileUtils.moveFile(inputFile, moveFileDest);
            logger.info("Moved duplicate input file {} to bin.", inputFileName);
        } catch (IOException e) {
            String inputFilePath = inputFile.getAbsolutePath();
            logger.error("Failed to move duplicate input file {} to bin ({}). Attempting deletion.", inputFilePath, moveFileDest.getAbsolutePath(), e);
            try {
                FileUtils.delete(inputFile);
                logger.warn("Deleted duplicate input file {} after failing to move it to bin.", inputFilePath);
            } catch (IOException ioe) {
                logger.error("Failed to delete duplicate input file {} after move failed.", inputFilePath, ioe);
            }
        }
    }
}
