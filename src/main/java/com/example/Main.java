package com.example;

import com.example.config.PropertiesLoader;
import com.example.discord.DiscordNotifier;
import com.example.file.DirectoryManager;
import com.example.file.SingleTweetFileProcessor; // Import the new class
import com.example.file.TweetProcessor;
import com.example.file.TweetWriter;
import com.example.twitter.TweetData;
import com.example.twitter.TwitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Application starting...");
        String basePath;

        // Determine base path (same as before)
        if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
            basePath = args[0];
            logger.info("Using provided base path: {}", basePath);
        } else {
            try {
                File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path jarDir = jarFile.getParentFile().toPath();
                Path defaultDataDir = jarDir.resolve("data");
                basePath = defaultDataDir.toAbsolutePath().toString();
                logger.info("No base path provided. Using default relative to JAR: {}", basePath);
            } catch (URISyntaxException | SecurityException | NullPointerException e) {
                logger.warn("Error determining JAR file location for default path: {}. Falling back to current working directory.", e.getMessage());
                basePath = Paths.get("data").toAbsolutePath().toString();
                logger.warn("Using default path in current working directory: {}", basePath);
            }
        }

        DirectoryManager dirManager = null;
        DiscordNotifier discordNotifier = null;

        try {
            // --- Setup Directories and Logging Path ---
            dirManager = new DirectoryManager(basePath);
            System.setProperty("LOG_DIR", dirManager.getLogsDir().toAbsolutePath().toString());
            logger.info("Log directory set to: {}", dirManager.getLogsDir().toAbsolutePath());

            // --- Configuration Loading ---
            logger.info("Loading configuration...");
            PropertiesLoader propsLoader = new PropertiesLoader();
            String twitterBearerToken = System.getenv("TWITTER_BEARER_TOKEN");
            String twitterUsername = System.getenv("TWITTER_USERNAME");
            String discordBotToken = System.getenv("DISCORD_BOT_TOKEN");
            String discordChannelId = propsLoader.getProperty("discord.channel.id");

            if (isNullOrEmpty(twitterBearerToken) || isNullOrEmpty(twitterUsername) || isNullOrEmpty(discordBotToken) || isNullOrEmpty(discordChannelId)) {
                logger.error("Missing required configuration. Ensure TWITTER_BEARER_TOKEN, TWITTER_USERNAME, DISCORD_BOT_TOKEN env vars are set, and discord.channel.id is in config.properties.");
                System.exit(1);
            }
            logger.info("Configuration loaded successfully.");

            // --- Service Initialization ---
            logger.info("Initializing services...");
            TwitterService twitterService = new TwitterService(twitterBearerToken, twitterUsername);
            TweetWriter tweetWriter = new TweetWriter(dirManager.getInputDir());
            discordNotifier = new DiscordNotifier(discordBotToken, discordChannelId);

            // Instantiate the new single file processor
            SingleTweetFileProcessor singleFileProcessor = new SingleTweetFileProcessor(dirManager, discordNotifier);

            // Instantiate the main processor, injecting the single file processor
            TweetProcessor tweetProcessor = new TweetProcessor(dirManager, singleFileProcessor);

            // --- Core Logic ---
            logger.info("Fetching tweets for user: {}", twitterUsername);
            List<TweetData> tweets = twitterService.fetchTimelineTweets(10);

            if (tweets.isEmpty()) {
                logger.info("No new tweets fetched or an error occurred during fetch.");
            } else {
                logger.info("Fetched {} tweets.", tweets.size());
                logger.info("Writing tweets to input directory: {}", dirManager.getInputDir());
                for (TweetData tweet : tweets) {
                    tweetWriter.writeTweetToFile(tweet);
                }

                // Call the refactored processor
                logger.info("Processing tweet files and notifying Discord...");
                tweetProcessor.processInputFiles();
            }

            logger.info("Main processing complete.");

        } catch (Exception e) {
            logger.error("An unhandled error occurred during execution: {}", e.getMessage(), e);
            System.exit(1);
        } finally {
            logger.info("Initiating shutdown sequence...");
            if (discordNotifier != null) {
                discordNotifier.shutdown();
            }
            logger.info("Application finished.");
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
