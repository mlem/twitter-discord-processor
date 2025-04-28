package com.example.file;

import com.example.twitter.TweetData;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class TweetWriter {

    private final Path inputDirPath;
    public static final String IMAGE_URL_SEPARATOR = ","; // Separator for image URLs in the file

    public TweetWriter(Path inputDirPath) {
        this.inputDirPath = inputDirPath;
    }

    public void writeTweetToFile(TweetData tweetData) {
        String fileName = "tweet_" + tweetData.getId() + ".txt";
        File outputFile = inputDirPath.resolve(fileName).toFile();

        // Join image URLs with a comma, handle empty list
        String imageUrlsString = tweetData.getImageUrls().isEmpty()
                ? ""
                : tweetData.getImageUrls().stream().collect(Collectors.joining(IMAGE_URL_SEPARATOR));

        // Define lines for clarity
        String line1 = "Text: " + tweetData.getText();
        String line2 = "URL: " + tweetData.getUrl();
        String line3 = "ImageURLs: " + imageUrlsString; // Store comma-separated URLs

        String content = String.join("\n", line1, line2, line3);

        try {
            FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8);
            System.out.println("Successfully wrote tweet " + tweetData.getId() + " to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write tweet " + tweetData.getId() + " to file: " + e.getMessage());
            // Handle error appropriately
        }
    }
}