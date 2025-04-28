package com.example.file;

import com.example.discord.DiscordNotifier;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC; // Import MDC

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles the processing logic for a single tweet file,
 * including checks, consumption, moving, and per-file logging via MDC.
 */
public class SingleTweetFileProcessor {

    // Use the standard logger; SiftingAppender will route based on MDC
    private static final Logger logger = LoggerFactory.getLogger(SingleTweetFileProcessor.class);
    private static final String MDC_KEY = "logFileName"; // Key for MDC

    private final DiscordNotifier discordNotifier;
    private final Path processedDir;
    private final Path failedDir;
    private final Path binDir; // For moving duplicates

    public SingleTweetFileProcessor(DirectoryManager directoryManager, DiscordNotifier discordNotifier) {
        this.discordNotifier = discordNotifier;
        this.processedDir = directoryManager.getProcessedDir();
        this.failedDir = directoryManager.getFailedDir();
        this.binDir = directoryManager.getBinDir(); // Get bin path
    }

    /**
     * Processes a single input file representing a tweet.
     * Sets up MDC for per-file logging.
     *
     * @param inputFile The tweet file from the input directory.
     */
    public void processFile(File inputFile) {
        String inputFileName = inputFile.getName();
        // Create a log file name based on the input file name (e.g., tweet_12345)
        String logFileName = inputFileName.replace(".txt", ""); // Remove extension for log name

        // Set MDC context for this file's processing
        MDC.put(MDC_KEY, logFileName);

        try {
            logger.info("Starting processing for file: {}", inputFileName); // Will go to tweet log

            // --- Check if already processed or failed ---
            Path processedFilePath = processedDir.resolve(inputFileName);
            Path failedFilePath = failedDir.resolve(inputFileName);

            if (Files.exists(processedFilePath)) {
                logger.info("File already exists in processed directory. Skipping.");
                moveDuplicateToBin(inputFile);
                return; // Stop processing this file
            }

            if (Files.exists(failedFilePath)) {
                logger.info("File already exists in failed directory. Skipping.");
                moveDuplicateToBin(inputFile);
                return; // Stop processing this file
            }
            // --- End Check ---

            logger.info("Attempting to send to Discord...");
            boolean success;
            try {
                success = discordNotifier.consume(inputFile); // Attempt to consume
                if (success) {
                    logger.info("Successfully consumed and queued message for Discord.");
                } else {
                    // Consume method logs specific errors
                    logger.warn("Discord consumption step indicated failure.");
                }
            } catch (Exception e) {
                logger.error("Exception during Discord consumption: {}", e.getMessage(), e);
                success = false; // Ensure it's marked as failed on exception
            }

            // --- Move File Based on Success ---
            moveFileAfterProcessing(inputFile, success);

        } catch (Exception e) {
            // Catch any unexpected errors during the processing steps
            logger.error("Unexpected error processing file {}: {}", inputFileName, e.getMessage(), e);
            // Attempt to move to failed even on unexpected error
            try {
                moveFileAfterProcessing(inputFile, false);
            } catch (Exception moveEx) {
                logger.error("Could not move file {} to failed directory after unexpected error: {}", inputFileName, moveEx.getMessage(), moveEx);
            }
        } finally {
            logger.info("Finished processing for file: {}", inputFileName);
            // !!! Crucial: Clear MDC context for this file !!!
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Moves the processed file to either the 'processed' or 'failed' directory.
     *
     * @param inputFile The file to move.
     * @param success   True if processing was successful, false otherwise.
     */
    private void moveFileAfterProcessing(File inputFile, boolean success) {
        String inputFileName = inputFile.getName();
        File targetDir = success ? processedDir.toFile() : failedDir.toFile();
        String targetDirName = success ? "processed" : "failed";

        try {
            FileUtils.moveFileToDirectory(inputFile, targetDir, true);
            logger.info("Moved {} to {} directory: {}", inputFileName, targetDirName, targetDir.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to move file {} to {} directory after processing: {}", inputFileName, targetDirName, e.getMessage(), e);
            // Consider alternative actions if moving fails (e.g., retry, leave in input with warning)
        }
    }

    /**
     * Moves a duplicate file found in the input directory to the bin directory.
     *
     * @param inputFile The duplicate file from the input directory.
     */
    private void moveDuplicateToBin(File inputFile) {
        String inputFileName = inputFile.getName();
        File moveFileDest = binDir.resolve(inputFileName).toFile(); // Use Path.resolve
        logger.debug("Moving duplicate input file {} to bin directory: {}", inputFileName, binDir.toAbsolutePath());
        try {
            // Ensure bin directory exists (should be handled by DirectoryManager, but belt-and-suspenders)
            if (!binDir.toFile().exists()) {
                Files.createDirectories(binDir);
            }
            // Use moveFile which handles cross-filesystem moves better potentially
            FileUtils.moveFile(inputFile, moveFileDest);
            logger.info("Moved duplicate input file {} to bin.", inputFileName);
        } catch (IOException e) {
            String inputFilePath = inputFile.getAbsolutePath(); // Use getAbsolutePath
            logger.error("Failed to move duplicate input file {} to bin ({}). Attempting deletion.", inputFilePath, moveFileDest.getAbsolutePath(), e);
            // Fallback to deletion if move fails
            try {
                FileUtils.delete(inputFile);
                logger.warn("Deleted duplicate input file {} after failing to move it to bin.", inputFilePath);
            } catch (IOException ioe) {
                logger.error("Failed to delete duplicate input file {} after move failed.", inputFilePath, ioe);
            }
        }
    }
}
