package com.example.file;

import com.example.twitter.TweetData;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

        // Define lines including new author info
        // Use empty string if any author field is null to avoid "null" in the file
        String line1 = "Text: " + tweetData.getText().replaceAll("\n", "###n###");
        String line2 = "URL: " + tweetData.getUrl();
        String line3 = "ImageURLs: " + imageUrlsString;
        String line4 = "AuthorName: " + (tweetData.getAuthorName() != null ? tweetData.getAuthorName() : "");
        String line5 = "AuthorProfileURL: " + (tweetData.getAuthorProfileUrl() != null ? tweetData.getAuthorProfileUrl() : "");
        String line6 = "AuthorImageURL: " + (tweetData.getAuthorProfileImageUrl() != null ? tweetData.getAuthorProfileImageUrl() : "");

        // Join all lines
        String content = String.join("\n", line1, line2, line3, line4, line5, line6);

        try {
            FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8);
            logger.info("Successfully wrote tweet {} to {}", tweetData.getId(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write tweet {} to file {}: {}", tweetData.getId(), outputFile.getName(), e.getMessage(), e);
        }
    }
}
