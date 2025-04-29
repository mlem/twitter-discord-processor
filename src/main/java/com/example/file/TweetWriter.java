package com.example.file;

import com.example.twitter.TweetData;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class TweetWriter {

    public static final String IMAGE_URL_SEPARATOR = ",";
    private static final Logger logger = LoggerFactory.getLogger(TweetWriter.class);
    private final Path inputDirPath;

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

        String createdAtString = "";
        if (tweetData.getCreatedAt() != null) {
            try {
                createdAtString = tweetData.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                logger.error("Error formatting createdAt timestamp for tweet {}: {}", tweetData.getId(), e.getMessage());
            }
        } else {
            logger.warn("Tweet {} has null createdAt timestamp.", tweetData.getId());
        }

        // Define lines including all context
        String line1 = "Text: " + tweetData.getText().replaceAll("\n", "###n###");
        String line2 = "URL: " + tweetData.getUrl();
        String line3 = "ImageURLs: " + imageUrlsString;
        String line4 = "CreatedAt: " + createdAtString;
        // Twitter Author
        String line5 = "AuthorName: " + (tweetData.getAuthorName() != null ? tweetData.getAuthorName() : "");
        String line6 = "AuthorProfileURL: " + (tweetData.getAuthorProfileUrl() != null ? tweetData.getAuthorProfileUrl() : "");
        String line7 = "AuthorImageURL: " + (tweetData.getAuthorProfileImageUrl() != null ? tweetData.getAuthorProfileImageUrl() : "");
        // Twitch Context
        String line8 = "TwitchUsername: " + (tweetData.getTwitchUsername() != null ? tweetData.getTwitchUsername() : "");
        String line9 = "TwitchImageURL: " + (tweetData.getTwitchProfileImageUrl() != null ? tweetData.getTwitchProfileImageUrl() : "");
        String line10= "TwitchChannelURL: " + (tweetData.getTwitchChannelUrl() != null ? tweetData.getTwitchChannelUrl() : "");


        // Join all lines
        String content = String.join("\n", line1, line2, line3, line4, line5, line6, line7, line8, line9, line10);

        try {
            FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8);
            logger.info("Successfully wrote tweet {} to {}", tweetData.getId(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write tweet {} to file {}: {}", tweetData.getId(), outputFile.getName(), e.getMessage(), e);
        }
    }
}
