package com.example.file;

import com.example.discord.DiscordNotifier;
// import com.example.twitch.TwitchUserInfo; // No longer needed
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC; // Kept MDC as per user's logback config

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
// import java.util.Optional; // No longer needed

/**
 * Handles the processing logic for a single tweet JSON file,
 * including checks, consumption, moving, and per-file logging via MDC.
 * Assumes all necessary context is within the file itself.
 */
public class SingleTweetFileProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SingleTweetFileProcessor.class);
    private static final String MDC_KEY = "logFileName"; // Kept MDC key based on logback.xml

    private final DiscordNotifier discordNotifier;
    private final Path processedDir;
    private final Path failedDir;
    private final Path binDir;
    // private final Optional<TwitchUserInfo> twitchInfo; // Removed

    // Updated constructor - removed twitchInfo parameter
    public SingleTweetFileProcessor(DirectoryManager directoryManager,
                                    DiscordNotifier discordNotifier) {
        this.discordNotifier = discordNotifier;
        this.processedDir = directoryManager.getProcessedDir();
        this.failedDir = directoryManager.getFailedDir();
        this.binDir = directoryManager.getBinDir();
    }

    /**
     * Processes a single input JSON file representing a tweet.
     * Sets up MDC for per-file logging.
     *
     * @param inputFile The tweet JSON file from the input directory.
     */
    public void processFile(File inputFile) {
        String inputFileName = inputFile.getName();
        // Ensure we only process .json files (basic check)
        if (!inputFileName.toLowerCase().endsWith(".json")) {
            logger.trace("Skipping non-JSON file: {}", inputFileName);
            return;
        }

        // Create log file name based on JSON file name (e.g., tweet_12345)
        String logFileName = inputFileName.replace(".json", ""); // Use .json extension

        MDC.put(MDC_KEY, logFileName); // Set MDC for SiftingAppender

        try {
            logger.info("Starting processing for JSON file: {}", inputFileName); // Goes to specific tweet log

            // --- Check if already processed or failed ---
            // Note: This check might be redundant if also performed in TweetProcessor,
            // but kept here for robustness based on previous state.
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

            logger.info("Attempting to send JSON file {} to Discord...", inputFileName); // Goes to specific tweet log
            boolean success;
            try {
                // Call consume without twitchInfo, reads context from file
                success = discordNotifier.consume(inputFile);
                if (success) {
                    logger.info("Successfully consumed and queued message for Discord from file {}.", inputFileName); // Goes to specific tweet log
                } else {
                    logger.warn("Discord consumption step indicated failure for file {}.", inputFileName); // Goes to specific tweet log
                }
            } catch (Exception e) {
                logger.error("Exception during Discord consumption for file {}: {}", inputFileName, e.getMessage(), e); // Goes to specific tweet log
                success = false;
            }

            // --- Move File Based on Success ---
            moveFileAfterProcessing(inputFile, success);

        } catch (Exception e) {
            logger.error("Unexpected error processing JSON file {}: {}", inputFileName, e.getMessage(), e); // Goes to specific tweet log
            try {
                moveFileAfterProcessing(inputFile, false);
            } catch (Exception moveEx) {
                logger.error("Could not move file {} to failed directory after unexpected error: {}", inputFileName, moveEx.getMessage(), moveEx); // Goes to specific tweet log
            }
        } finally {
            logger.info("Finished processing attempt for JSON file: {}", inputFileName); // Goes to specific tweet log
            MDC.remove(MDC_KEY); // Clear MDC
        }
    }

    // moveFileAfterProcessing uses the main logger (application.log and console)
    private void moveFileAfterProcessing(File inputFile, boolean success) {
        String inputFileName = inputFile.getName();
        File targetDir = success ? processedDir.toFile() : failedDir.toFile();
        String targetDirName = success ? "processed" : "failed";
        try {
            FileUtils.moveFileToDirectory(inputFile, targetDir, true);
            // Use the main logger for file operations summary
            LoggerFactory.getLogger(TweetProcessor.class).info("Moved {} to {} directory: {}", inputFileName, targetDirName, targetDir.getAbsolutePath());
        } catch (IOException e) {
            LoggerFactory.getLogger(TweetProcessor.class).error("Failed to move file {} to {} directory after processing: {}", inputFileName, targetDirName, e.getMessage(), e);
        }
    }


    // moveDuplicateToBin uses the main logger (application.log and console)
    private void moveDuplicateToBin(File inputFile) {
        String inputFileName = inputFile.getName();
        File moveFileDest = binDir.resolve(inputFileName).toFile();
        LoggerFactory.getLogger(TweetProcessor.class).debug("Moving duplicate input file {} to bin directory: {}", inputFileName, binDir.toAbsolutePath());
        try {
            if (!binDir.toFile().exists()) {
                Files.createDirectories(binDir);
            }
            FileUtils.moveFile(inputFile, moveFileDest);
            LoggerFactory.getLogger(TweetProcessor.class).info("Moved duplicate input file {} to bin.", inputFileName);
        } catch (IOException e) {
            String inputFilePath = inputFile.getAbsolutePath();
            LoggerFactory.getLogger(TweetProcessor.class).error("Failed to move duplicate input file {} to bin ({}). Attempting deletion.", inputFilePath, moveFileDest.getAbsolutePath(), e);
            try {
                FileUtils.delete(inputFile);
                LoggerFactory.getLogger(TweetProcessor.class).warn("Deleted duplicate input file {} after failing to move it to bin.", inputFilePath);
            } catch (IOException ioe) {
                LoggerFactory.getLogger(TweetProcessor.class).error("Failed to delete duplicate input file {} after move failed.", inputFilePath, ioe);
            }
        }
    }
}
