package com.example.file;

import com.example.twitter.TweetData;
import org.apache.commons.io.FileUtils; // Using Apache Commons IO

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class TweetWriter {

    private final Path inputDirPath;

    public TweetWriter(Path inputDirPath) {
        this.inputDirPath = inputDirPath;
    }

    public void writeTweetToFile(TweetData tweetData) {
        String fileName = "tweet_" + tweetData.id() + ".txt";
        File outputFile = inputDirPath.resolve(fileName).toFile();
        String content = "Text: " + tweetData.text() + "\nURL: " + tweetData.url();

        try {
            FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8);
            System.out.println("Successfully wrote tweet " + tweetData.id() + " to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write tweet " + tweetData.id() + " to file: " + e.getMessage());
            // Handle error appropriately (e.g., log, throw exception)
        }
    }
}