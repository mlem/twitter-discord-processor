package com.example.file;

import com.example.twitter.TweetData;
import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature; // For pretty printing, optional
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // For LocalDateTime
// Removed FileUtils import as ObjectMapper writes directly
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
// Removed StandardCharsets import
import java.nio.file.Path;
// Removed Collectors import

public class TweetWriter {

    // IMAGE_URL_SEPARATOR no longer needed for JSON
    // public static final String IMAGE_URL_SEPARATOR = ",";
    private static final Logger logger = LoggerFactory.getLogger(TweetWriter.class);
    private final Path inputDirPath;
    private final ObjectMapper objectMapper; // Jackson ObjectMapper instance

    public TweetWriter(Path inputDirPath) {
        this.inputDirPath = inputDirPath;
        logger.info("TweetWriter initialized for input directory: {}", inputDirPath);

        // Initialize ObjectMapper and configure it
        this.objectMapper = new ObjectMapper();
        // Register module to handle Java 8 Date/Time types like LocalDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps (write as ISO-8601 strings)
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Optional: Enable pretty printing for readability
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void writeTweetToFile(TweetData tweetData) {
        // Change file extension to .json
        String fileName = "tweet_" + tweetData.getId() + ".json";
        File outputFile = inputDirPath.resolve(fileName).toFile();
        logger.debug("Preparing to write tweet {} to JSON file: {}", tweetData.getId(), outputFile.getAbsolutePath());

        try {
            // Serialize the TweetData object directly to the file
            objectMapper.writeValue(outputFile, tweetData);
            logger.info("Successfully wrote tweet {} to JSON file {}", tweetData.getId(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            // Log the error with stack trace
            logger.error("Failed to write tweet {} to JSON file {}: {}", tweetData.getId(), outputFile.getName(), e.getMessage(), e);
        }
    }
}
