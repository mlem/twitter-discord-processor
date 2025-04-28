package com.example.file;

import com.example.discord.DiscordNotifier;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files; // Import Files for exists check
import java.nio.file.Path;   // Import Path

public class TweetProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TweetProcessor.class);

    private final File inputDir;
    private final File processedDir;
    private final File failedDir;
    private final DiscordNotifier discordNotifier;

    public TweetProcessor(DirectoryManager directoryManager, DiscordNotifier discordNotifier) {
        this.inputDir = directoryManager.getInputDir().toFile();
        // Get the correct paths from DirectoryManager
        this.processedDir = directoryManager.getProcessedDir().toFile();
        this.failedDir = directoryManager.getFailedDir().toFile();
        this.discordNotifier = discordNotifier;
        logger.info("TweetProcessor initialized. Input: {}, Processed: {}, Failed: {}",
                inputDir.getAbsolutePath(), processedDir.getAbsolutePath(), failedDir.getAbsolutePath());
    }

    public void processInputFiles() {
        logger.info("Starting processing of files in: {}", inputDir.getAbsolutePath());
        File[] files = inputDir.listFiles();

        if (files == null) {
            logger.error("Could not list files in input directory: {}. Check permissions.", inputDir.getAbsolutePath());
            return;
        }

        if (files.length == 0) {
            logger.info("No files found in input directory.");
            return;
        }

        logger.info("Found {} files to process.", files.length);

        for (File inputFile : files) {
            if (!inputFile.isFile()) {
                logger.trace("Skipping non-file item: {}", inputFile.getName());
                continue; // Skip directories
            }

            String inputFileName = inputFile.getName();
            logger.debug("Checking file: {}", inputFileName);

            // --- Check if already processed or failed ---
            Path processedFilePath = processedDir.toPath().resolve(inputFileName);
            Path failedFilePath = failedDir.toPath().resolve(inputFileName);

            if (Files.exists(processedFilePath)) {
                logger.info("Skipping already processed file: {}", inputFileName);
                // Optionally delete the duplicate from input:
                // try { FileUtils.delete(inputFile); logger.debug("Deleted duplicate input file: {}", inputFileName); } catch (IOException e) { logger.error("Failed to delete duplicate input file {}", inputFileName, e); }
                continue; // Skip to the next file
            }

            if (Files.exists(failedFilePath)) {
                logger.info("Skipping file that previously failed: {}", inputFileName);
                // Optionally delete the duplicate from input:
                // try { FileUtils.delete(inputFile); logger.debug("Deleted duplicate input file: {}", inputFileName); } catch (IOException e) { logger.error("Failed to delete duplicate input file {}", inputFileName, e); }
                continue; // Skip to the next file
            }
            // --- End Check ---


            logger.info("Processing file: {}", inputFileName);
            boolean success = false;
            try {
                success = discordNotifier.consume(inputFile); // Attempt to consume
            } catch (Exception e) {
                logger.error("Exception during consumption of file {}: {}", inputFileName, e.getMessage(), e);
                success = false; // Ensure it's marked as failed on exception
            }


            try {
                if (success) {
                    // Move to the processed directory (inside bin)
                    FileUtils.moveFileToDirectory(inputFile, processedDir, true);
                    logger.info("Moved {} to processed directory: {}", inputFileName, processedDir.getAbsolutePath());
                } else {
                    logger.warn("Processing failed for {}. Moving to failed directory.", inputFileName);
                    // Move to the failed directory (inside bin)
                    FileUtils.moveFileToDirectory(inputFile, failedDir, true);
                    logger.info("Moved {} to failed directory: {}", inputFileName, failedDir.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("Failed to move file {} after processing: {}", inputFileName, e.getMessage(), e);
                // Consider leaving the file in input or another strategy if moving fails
            }
        }
        logger.info("Finished processing batch of files.");
    }
}
