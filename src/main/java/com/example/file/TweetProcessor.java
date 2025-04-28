package com.example.file;

import com.example.discord.DiscordNotifier;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import java.io.File;
import java.io.IOException;

public class TweetProcessor {

    // Initialize Logger
    private static final Logger logger = LoggerFactory.getLogger(TweetProcessor.class);

    private final File inputDir;
    private final File processedDir;
    private final File failedDir;
    private final DiscordNotifier discordNotifier;

    public TweetProcessor(DirectoryManager directoryManager, DiscordNotifier discordNotifier) {
        this.inputDir = directoryManager.getInputDir().toFile();
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

        for (File file : files) {
            if (!file.isFile()) {
                logger.trace("Skipping non-file item: {}", file.getName());
                continue; // Skip directories
            }

            logger.info("Processing file: {}", file.getName());
            boolean success = false;
            try {
                success = discordNotifier.consume(file); // Attempt to consume
            } catch (Exception e) {
                logger.error("Exception during consumption of file {}: {}", file.getName(), e.getMessage(), e);
                success = false; // Ensure it's marked as failed on exception
            }


            try {
                if (success) {
                    FileUtils.moveFileToDirectory(file, processedDir, true);
                    logger.info("Moved {} to processed directory.", file.getName());
                } else {
                    logger.warn("Processing failed for {}. Moving to failed directory.", file.getName());
                    FileUtils.moveFileToDirectory(file, failedDir, true);
                    logger.info("Moved {} to failed directory.", file.getName());
                }
            } catch (IOException e) {
                logger.error("Failed to move file {} after processing: {}", file.getName(), e.getMessage(), e);
                // Consider leaving the file in input or another strategy if moving fails
            }
        }
        logger.info("Finished processing batch of files.");
    }
}
