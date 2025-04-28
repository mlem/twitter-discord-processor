package com.example.file;

import com.example.discord.DiscordNotifier; // Keep for context if needed, but not used directly
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Scans the input directory and delegates the processing of each file
 * to a SingleTweetFileProcessor instance.
 */
public class TweetProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TweetProcessor.class);

    private final File inputDir;
    private final SingleTweetFileProcessor singleFileProcessor; // Use the new processor

    // Constructor now takes SingleTweetFileProcessor
    public TweetProcessor(DirectoryManager directoryManager, SingleTweetFileProcessor singleFileProcessor) {
        this.inputDir = directoryManager.getInputDir().toFile();
        this.singleFileProcessor = singleFileProcessor; // Store the injected processor
        logger.info("TweetProcessor initialized for input directory: {}", inputDir.getAbsolutePath());
    }

    /**
     * Processes all valid files found in the input directory.
     */
    public void processInputFiles() {
        logger.info("Starting scan of input directory: {}", inputDir.getAbsolutePath());
        File[] files = inputDir.listFiles();

        if (files == null) {
            logger.error("Could not list files in input directory: {}. Check permissions.", inputDir.getAbsolutePath());
            return;
        }

        if (files.length == 0) {
            logger.info("No files found in input directory during this scan.");
            return;
        }

        logger.info("Found {} items in input directory. Processing files...", files.length);

        int fileCount = 0;
        for (File inputFile : files) {
            // Only process actual files
            if (inputFile.isFile()) {
                fileCount++;
                // Delegate processing of this single file
                singleFileProcessor.processFile(inputFile);
            } else {
                logger.trace("Skipping non-file item: {}", inputFile.getName());
            }
        }

        if (fileCount == 0) {
            logger.info("No files (only directories or other items) found in input directory to process.");
        } else {
            logger.info("Finished processing batch. {} files were evaluated.", fileCount);
        }
    }

    // Removed the moveOrDeleteFile method - its logic is now in SingleTweetFileProcessor
}
