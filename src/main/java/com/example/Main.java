package com.example;

import com.example.config.PropertiesLoader;
import com.example.discord.DiscordNotifier;
import com.example.file.DirectoryManager;
import com.example.file.TweetProcessor;
import com.example.file.TweetWriter;
import com.example.twitter.TweetData;
import com.example.twitter.TwitterService;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Build this with the command in root directory
 * ```
 * mvn clean package
 * ```
 *
 * Run this with
 * ```
 * java -jar target/twitter-discord-processor-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/your/data/directory
 * ```
 */
public class Main {

    /**
     * Need to set environment variables in order to use this
     *
     * TWITTER_BEARER_TOKEN: Your Twitter API Bearer Token.
     * TWITTER_USERNAME: The Twitter username whose timeline you want to fetch.
     * DISCORD_BOT_TOKEN: Your Discord Bot Token.
     *
     * @param args
     */
    public static void main(String[] args) {
        String basePath;

        // Determine base path: use argument or default to ./data relative to JAR
        if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
            basePath = args[0];
            System.out.println("Using provided base path: " + basePath);
        } else {
            try {
                // Find the directory containing the JAR file
                File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path jarDir = jarFile.getParentFile().toPath(); // Directory containing the jar
                Path defaultDataDir = jarDir.resolve("data"); // Default path: <jar_dir>/data
                basePath = defaultDataDir.toAbsolutePath().toString();
                System.out.println("No base path provided. Using default relative to JAR: " + basePath);
            } catch (URISyntaxException | SecurityException e) {
                System.err.println("Error determining JAR file location for default path: " + e.getMessage());
                // Fallback to using 'data' directory in the current working directory
                basePath = Paths.get("data").toAbsolutePath().toString();
                System.err.println("Falling back to default path in current working directory: " + basePath);
                // Optional: Exit if default path determination fails critically
                // System.err.println("Cannot determine default path. Please provide a path as an argument.");
                // return;
            } catch (NullPointerException e) {
                System.err.println("Could not determine code source location (maybe running from IDE?).");
                // Fallback to using 'data' directory in the current working directory
                basePath = Paths.get("data").toAbsolutePath().toString();
                System.err.println("Falling back to default path in current working directory: " + basePath);
            }
        }

        // --- Configuration ---
        PropertiesLoader propsLoader = new PropertiesLoader();
        String twitterBearerToken = System.getenv("TWITTER_BEARER_TOKEN");
        String twitterUsername = System.getenv("TWITTER_USERNAME"); // Or get from props/args
        String discordBotToken = System.getenv("DISCORD_BOT_TOKEN");
        String discordChannelId = propsLoader.getProperty("discord.channel.id");

        // Basic validation
        if (twitterBearerToken == null || twitterUsername == null || discordBotToken == null || discordChannelId == null) {
            System.err.println("Error: Missing required configuration.");
            System.err.println("Ensure TWITTER_BEARER_TOKEN, TWITTER_USERNAME, DISCORD_BOT_TOKEN env vars are set,");
            System.err.println("and discord.channel.id is present in config.properties.");
            return;
        }

        DiscordNotifier discordNotifier = null;
        try {
            // --- Initialization ---
            System.out.println("Initializing with base path: " + basePath);
            DirectoryManager dirManager = new DirectoryManager(basePath); // Use determined basePath
            TwitterService twitterService = new TwitterService(twitterBearerToken, twitterUsername);
            TweetWriter tweetWriter = new TweetWriter(dirManager.getInputDir());
            discordNotifier = new DiscordNotifier(discordBotToken, discordChannelId); // Initialize Discord bot

            // --- Fetch Tweets ---
            System.out.println("Fetching tweets for user: " + twitterUsername);
            List<TweetData> tweets = twitterService.fetchTimelineTweets(10); // Fetch latest 10 tweets

            if (tweets.isEmpty()) {
                System.out.println("No tweets fetched or an error occurred.");
            } else {
                System.out.println("Fetched " + tweets.size() + " tweets.");
                // --- Write Tweets to Files ---
                System.out.println("Writing tweets to input directory: " + dirManager.getInputDir());
                for (TweetData tweet : tweets) {
                    tweetWriter.writeTweetToFile(tweet);
                }

                // --- Process Files and Notify Discord ---
                System.out.println("Processing tweet files and notifying Discord...");
                TweetProcessor tweetProcessor = new TweetProcessor(dirManager, discordNotifier);
                tweetProcessor.processInputFiles();
            }

            System.out.println("Processing complete.");

        } catch (Exception e) {
            System.err.println("An error occurred during execution: " + e.getMessage());
            e.printStackTrace(); // Consider more robust logging
        } finally {
            // --- Shutdown ---
            if (discordNotifier != null) {
                discordNotifier.shutdown(); // Gracefully shut down JDA connection
            }
            System.out.println("Application finished.");
        }
    }
}