package com.example.file;

import com.example.twitter.TweetData;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class TweetWriter {

    // Initialize Logger
    private static final Logger logger = LoggerFactory.getLogger(TweetWriter.class);
    private final Path inputDirPath;
    public static final String IMAGE_URL_SEPARATOR = ","; // Make public for DiscordNotifier

    public TweetWriter(Path inputDirPath) {
        this.inputDirPath = inputDirPath;
        logger.info("TweetWriter initialized for input directory: {}", inputDirPath);
    }

    public void writeTweetToFile(TweetData tweetData) {
        String fileName = "tweet_" + tweetData.getId() + ".txt";
        File outputFile = inputDirPath.resolve(fileName).toFile();
        logger.debug("Preparing to write tweet {} to file: {}", tweetData.getId(), outputFile.getAbsolutePath());

        String imageUrlsString = tweetData.getImageUrls().isEmpty()
                ? ""
                : tweetData.getImageUrls().stream().collect(Collectors.joining(IMAGE_URL_SEPARATOR));

        String line1 = "Text: " + tweetData.getText();
        String line2 = "URL: " + tweetData.getUrl();
        String line3 = "ImageURLs: " + imageUrlsString;
        String content = String.join("\n", line1, line2, line3);

        try {
            FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8);
            logger.info("Successfully wrote tweet {} to {}", tweetData.getId(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            // Log the error with stack trace
            logger.error("Failed to write tweet {} to file {}: {}", tweetData.getId(), outputFile.getName(), e.getMessage(), e);
        }
    }
}
