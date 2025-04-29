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
import java.util.ArrayList; // Import ArrayList
import java.util.List; // Import List
import java.util.stream.Collectors;

public class TweetWriter {

    public static final String IMAGE_URL_SEPARATOR = ",";
    private static final Logger logger = LoggerFactory.getLogger(TweetWriter.class);
    private final Path inputDirPath;

    public TweetWriter(Path inputDirPath) {
        this.inputDirPath = inputDirPath;
        logger.info("TweetWriter initialized for input directory: {}", inputDirPath);
    }

    // Helper to handle nulls gracefully for writing
    private String formatValue(String prefix, Object value) {
        String stringValue = (value == null) ? "" : value.toString();
        // Escape newlines within the value itself if necessary, using the same marker
        stringValue = stringValue.replaceAll("\n", "###n###");
        return prefix + stringValue;
    }

    public void writeTweetToFile(TweetData tweetData) {
        String fileName = "tweet_" + tweetData.getId() + ".txt";
        File outputFile = inputDirPath.resolve(fileName).toFile();
        logger.debug("Preparing to write tweet {} to file: {}", tweetData.getId(), outputFile.getAbsolutePath());


        // Format known complex values or values needing specific formatting
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
        }

        // Build list of lines to write
        List<String> lines = new ArrayList<>();
        lines.add(formatValue("Text: ", tweetData.getText())); // Apply ###n### replacement here too
        lines.add(formatValue("URL: ", tweetData.getUrl()));
        lines.add("ImageURLs: " + imageUrlsString); // Already formatted
        lines.add("CreatedAt: " + createdAtString); // Already formatted
        // Twitter Author
        lines.add(formatValue("AuthorName: ", tweetData.getAuthorName()));
        lines.add(formatValue("AuthorProfileURL: ", tweetData.getAuthorProfileUrl()));
        lines.add(formatValue("AuthorImageURL: ", tweetData.getAuthorProfileImageUrl()));
        // Twitch Context
        lines.add(formatValue("TwitchUsername: ", tweetData.getTwitchUsername()));
        lines.add(formatValue("TwitchImageURL: ", tweetData.getTwitchProfileImageUrl()));
        lines.add(formatValue("TwitchChannelURL: ", tweetData.getTwitchChannelUrl()));
        // Additional Raw Tweet Fields
        lines.add(formatValue("TweetAuthorId: ", tweetData.getTweetAuthorId()));
        lines.add(formatValue("TweetConversationId: ", tweetData.getTweetConversationId()));
        lines.add(formatValue("TweetLang: ", tweetData.getTweetLang()));
        lines.add(formatValue("TweetSource: ", tweetData.getTweetSource()));
        lines.add(formatValue("TweetReplySettings: ", tweetData.getTweetReplySettings()));
        lines.add(formatValue("TweetInReplyToUserId: ", tweetData.getTweetInReplyToUserId()));
        lines.add(formatValue("TweetEntities: ", tweetData.getTweetEntitiesStr()));
        lines.add(formatValue("TweetAttachments: ", tweetData.getTweetAttachmentsStr()));
        lines.add(formatValue("TweetGeo: ", tweetData.getTweetGeoStr()));


        // Join all lines
        String content = String.join("\n", lines);

        try {
            FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8);
            logger.info("Successfully wrote tweet {} to {}", tweetData.getId(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write tweet {} to file {}: {}", tweetData.getId(), outputFile.getName(), e.getMessage(), e);
        }
    }
}
