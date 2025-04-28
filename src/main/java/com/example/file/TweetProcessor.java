package com.example.file;

import com.example.discord.DiscordNotifier;
import org.apache.commons.io.FileUtils; // Using Apache Commons IO
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class TweetProcessor {

    private final File inputDir;
    private final File processedDir;
    private final File failedDir;
    private final DiscordNotifier discordNotifier;

    public TweetProcessor(DirectoryManager directoryManager, DiscordNotifier discordNotifier) {
        this.inputDir = directoryManager.getInputDir().toFile();
        this.processedDir = directoryManager.getProcessedDir().toFile();
        this.failedDir = directoryManager.getFailedDir().toFile();
        this.discordNotifier = discordNotifier;
    }

    public void processInputFiles() {
        System.out.println("Starting processing of files in: " + inputDir.getAbsolutePath());
        // List files directly in the input directory (not recursively)
        File[] files = inputDir.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No files found in input directory.");
            return;
        }

        System.out.println("Found " + files.length + " files to process.");

        for (File file : files) {
            if (!file.isFile()) continue; // Skip directories

            System.out.println("Processing file: " + file.getName());
            boolean success = discordNotifier.consume(file);

            try {
                if (success) {
                    FileUtils.moveFileToDirectory(file, processedDir, true);
                    System.out.println("Moved " + file.getName() + " to processed directory.");
                } else {
                    System.err.println("Processing failed for " + file.getName());
                    FileUtils.moveFileToDirectory(file, failedDir, true);
                    System.out.println("Moved " + file.getName() + " to failed directory.");
                }
            } catch (IOException e) {
                System.err.println("Failed to move file " + file.getName() + ": " + e.getMessage());
                // Consider leaving the file in input or another strategy
            }
        }
        System.out.println("Finished processing files.");
    }
}