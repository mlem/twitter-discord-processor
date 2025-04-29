package com.example.file;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Manages reading and writing the ID of the last processed tweet
 * to the LAST_TWEET_ID.txt file.
 */
public class LastTweetIdManager {

    private static final Logger logger = LoggerFactory.getLogger(LastTweetIdManager.class);
    private final Path lastTweetIdFilePath;

    /**
     * Constructor requires the DirectoryManager to get the file path.
     * @param directoryManager The directory manager instance.
     */
    public LastTweetIdManager(DirectoryManager directoryManager) {
        this.lastTweetIdFilePath = directoryManager.getLastTweetIdFile();
        logger.info("LastTweetIdManager initialized for file: {}", lastTweetIdFilePath.toAbsolutePath());
    }

    /**
     * Reads the last processed tweet ID from the configured file.
     * @return Optional containing the ID string if found and valid, otherwise Optional.empty().
     */
    public Optional<String> readLastTweetId() {
        logger.debug("Attempting to read last tweet ID from: {}", lastTweetIdFilePath.toAbsolutePath());
        if (Files.exists(lastTweetIdFilePath) && Files.isReadable(lastTweetIdFilePath)) {
            try {
                String content = FileUtils.readFileToString(lastTweetIdFilePath.toFile(), StandardCharsets.UTF_8);
                if (content != null && !content.trim().isEmpty()) {
                    String trimmedId = content.trim();
                    // Basic validation: check if it looks like a number string
                    if (trimmedId.matches("\\d+")) {
                        logger.info("Successfully read last tweet ID: {}", trimmedId);
                        return Optional.of(trimmedId);
                    } else {
                         logger.warn("Content of {} ({}) is not a valid tweet ID format.", lastTweetIdFilePath.getFileName(), trimmedId);
                    }
                } else {
                     logger.info("Last tweet ID file {} is empty.", lastTweetIdFilePath.getFileName());
                }
            } catch (IOException e) {
                logger.error("Could not read last tweet ID file: {}", lastTweetIdFilePath.toAbsolutePath(), e);
            }
        } else {
             logger.info("Last tweet ID file not found or not readable: {}", lastTweetIdFilePath.toAbsolutePath());
        }
        return Optional.empty();
    }

    /**
     * Writes the given tweet ID to the configured file, overwriting previous content.
     * @param tweetId The ID string to write.
     */
    public void writeLastTweetId(String tweetId) {
         if (tweetId == null || !tweetId.matches("\\d+")) {
              logger.error("Attempted to write invalid tweet ID: {}. Aborting write.", tweetId);
              return;
         }
        logger.debug("Attempting to write last tweet ID {} to: {}", tweetId, lastTweetIdFilePath.toAbsolutePath());
        try {
            // Use false for the append parameter to overwrite the file
            FileUtils.writeStringToFile(lastTweetIdFilePath.toFile(), tweetId, StandardCharsets.UTF_8, false);
            logger.info("Successfully updated {} to: {}", lastTweetIdFilePath.getFileName(), tweetId);
        } catch (IOException e) {
            logger.error("Could not write last tweet ID file: {}", lastTweetIdFilePath.toAbsolutePath(), e);
        }
    }
}
