package com.example;

import com.example.config.PropertiesLoader;
import com.example.discord.DiscordNotifier;
import com.example.file.*;
import com.example.log.LogsDirLogBackPropertyDefiner;
import com.example.twitch.TwitchService;
import com.example.twitch.TwitchUserInfo;
import com.example.twitter.TweetData;
import com.example.twitter.TwitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_MAX_TWEETS = 10;

    public static void main(String[] args) {
        logger.info("Application starting...");
        String basePath;
        int maxTweetsToFetch = DEFAULT_MAX_TWEETS;

        // --- Argument Parsing (remains the same) ---
        if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
            basePath = args[0];
            logger.info("Using provided base path: {}", basePath);
        } else {
            try {
                // Use static method from DirectoryManager as per user's version
                basePath = DirectoryManager.basePathRelativeToJar();
                logger.info("No base path provided. Using default relative to JAR: {}", basePath);
            } catch (SecurityException | NullPointerException e) { // Catch exceptions it might throw
                logger.warn("Error determining JAR file location for default path: {}. Falling back to current working directory.", e.getMessage());
                basePath = Paths.get("data").toAbsolutePath().toString();
                logger.warn("Using default path in current working directory: {}", basePath);
            }
        }
        if (args.length >= 2 && args[1] != null && !args[1].trim().isEmpty()) {
            try {
                maxTweetsToFetch = Integer.parseInt(args[1]);
                if (maxTweetsToFetch <= 0) {
                    logger.warn("Invalid number provided for max tweets ({}). Must be positive. Using default: {}", args[1], DEFAULT_MAX_TWEETS);
                    maxTweetsToFetch = DEFAULT_MAX_TWEETS;
                } else {
                    if (maxTweetsToFetch > 100) {
                        logger.warn("Requested max tweets ({}) > API limit (100). Fetching 100.", maxTweetsToFetch);
                        maxTweetsToFetch = 100;
                    }
                    logger.info("Using provided max tweets to fetch: {}", maxTweetsToFetch);
                }
            } catch (NumberFormatException e) {
                logger.warn("Could not parse max tweets argument '{}'. Using default: {}", args[1], DEFAULT_MAX_TWEETS, e);
                maxTweetsToFetch = DEFAULT_MAX_TWEETS;
            }
        } else {
            logger.info("No max tweets argument provided. Using default: {}", DEFAULT_MAX_TWEETS);
        }
        // --- End Argument Parsing ---


        DirectoryManager dirManager = null;
        DiscordNotifier discordNotifier = null;
        TwitchService twitchService = null;
        TweetProcessor tweetProcessor = null;
        LastTweetIdManager lastTweetIdManager = null; // Declare the new manager

        try {
            // --- Setup Directories and Logging Path ---
            dirManager = new DirectoryManager(basePath); // Create dirs first
            // Use the LogBack Property Definer provided by user
            LogsDirLogBackPropertyDefiner.setDirectoryManager(dirManager);
            // Set system property as well (might be redundant depending on logback.xml)
            System.setProperty("LOG_DIR", dirManager.getLogsDir().toAbsolutePath().toString());
            logger.info("Log directory set to: {}", dirManager.getLogsDir().toAbsolutePath());

            // --- Initialize Last Tweet ID Manager ---
            lastTweetIdManager = new LastTweetIdManager(dirManager); // Instantiate it
            Optional<String> sinceId = lastTweetIdManager.readLastTweetId(); // Use instance method
            // Logging moved inside the manager's method
            // --- End Initialize Last Tweet ID Manager ---

            // --- Configuration Loading ---
            logger.info("Loading configuration...");
            PropertiesLoader propsLoader = new PropertiesLoader();
            String twitterBearerToken = System.getenv("TWITTER_BEARER_TOKEN");
            String twitterUsernameEnv = System.getenv("TWITTER_USERNAME");
            String discordBotToken = System.getenv("DISCORD_BOT_TOKEN");
            String discordChannelId = propsLoader.getProperty("discord.channel.id");
            String twitchClientId = System.getenv("TWITCH_CLIENT_ID");
            String twitchClientSecret = System.getenv("TWITCH_CLIENT_SECRET");
            String twitchUsername = propsLoader.getProperty("twitch.username"); // Configured Twitch username

            // Determine Twitter Username
            String twitterUsername;
            String usernameSource;
            if (isNullOrEmpty(twitterUsernameEnv)) {
                twitterUsername = propsLoader.getProperty("twitter.username");
                usernameSource = "config.properties";
            } else {
                twitterUsername = twitterUsernameEnv;
                usernameSource = "environment variable";
            }
            logger.info("Twitter username set to '{}' (from {})", twitterUsername, usernameSource);

            // Validation (Add Twitch checks)
            if (isNullOrEmpty(twitterBearerToken) || isNullOrEmpty(twitterUsername) ||
                    isNullOrEmpty(discordBotToken) || isNullOrEmpty(discordChannelId) ||
                    isNullOrEmpty(twitchClientId) || isNullOrEmpty(twitchClientSecret) || // Check Twitch env vars
                    isNullOrEmpty(twitchUsername)) { // Check Twitch username from props
                logger.error("Missing required configuration. Exiting.");
                // ... (error messages remain the same) ...
                System.exit(1); // Exit if config is missing
            }
            logger.info("Configuration loaded successfully.");

            // --- Service Initialization ---
            // Initialize services needed for both fetching and processing
            logger.info("Initializing core services...");
            TweetWriter tweetWriter = new TweetWriter(dirManager.getInputDir());
            discordNotifier = new DiscordNotifier(discordBotToken, discordChannelId);
            twitchService = new TwitchService(twitchClientId, twitchClientSecret); // Initialize TwitchService early
            TwitterService twitterService = new TwitterService(twitterBearerToken, twitterUsername); // Initialize TwitterService early

            // --- Fetch Twitch Info Once ---
            // This still needs to happen before processing, but can tolerate failure
            Optional<TwitchUserInfo> twitchInfoOpt = Optional.empty(); // Default to empty
            try {
                logger.info("Fetching Twitch user info for configured user: {}", twitchUsername);
                twitchInfoOpt = twitchService.fetchUserInfo(twitchUsername);
                if (!twitchInfoOpt.isPresent()) {
                    logger.warn("Could not fetch Twitch user info for {}. Embeds will not include Twitch thumbnail.", twitchUsername);
                }
            } catch (Exception e) {
                logger.error("Error fetching Twitch user info for {}: {}", twitchUsername, e.getMessage(), e);
                // Continue without Twitch info
            }
            // Extract info or use null if not present
            String twitchLogoUrl = twitchInfoOpt.map(TwitchUserInfo::profileImageUrl).orElse(null);
            String twitchChanUrl = twitchInfoOpt.map(TwitchUserInfo::channelUrl).orElse(null);
            // --- End Fetch Twitch Info ---


            // --- Attempt to Fetch and Write Tweets ---
            // This block can fail without stopping the subsequent processing step
            List<TweetData> fetchedTweets = Collections.emptyList(); // Initialize empty list
            String highestFetchedId = null; // Track the highest ID fetched in this batch
            try {
                logger.info("Attempting to fetch up to {} tweets for user {} since ID {}", maxTweetsToFetch, twitterUsername, sinceId.orElse("None"));

                // Pass sinceId to the fetch method
                fetchedTweets = twitterService.fetchTimelineTweets(
                        maxTweetsToFetch,
                        sinceId, // Pass the Optional<String>
                        twitchUsername,
                        twitchLogoUrl,
                        twitchChanUrl
                );

                if (fetchedTweets.isEmpty()) {
                    logger.info("No new tweets fetched since ID {} or an error occurred.", sinceId.orElse("start"));
                } else {
                    logger.info("Fetched {} new tweets.", fetchedTweets.size());
                    logger.info("Writing tweets to input directory: {}", dirManager.getInputDir());

                    // Find the highest ID among the fetched tweets
                    highestFetchedId = fetchedTweets.stream()
                            .map(TweetData::getId)
                            .max(Comparator.naturalOrder()) // IDs are usually numeric strings
                            .orElse(null);

                    for (TweetData tweet : fetchedTweets) {
                        // TweetData object now contains all necessary context
                        tweetWriter.writeTweetToFile(tweet);
                    }
                    // --- Write the new highest ID if found ---
                    if (highestFetchedId != null) {
                        // Use the manager instance to write the ID
                        lastTweetIdManager.writeLastTweetId(highestFetchedId);
                    } else {
                        logger.warn("Could not determine the highest ID from the fetched tweets.");
                    }
                    // --- End Write Highest ID ---
                }
            } catch (Exception e) {
                // Log the error but allow the program to continue to the processing step
                logger.error("Error occurred during tweet fetching or writing: {}. Proceeding to process existing files.", e.getMessage(), e);
            }
            // --- End Fetch and Write Tweets ---


            // --- Process Input Files (Always Run) ---
            // Instantiate processors right before processing
            logger.info("Initializing file processors...");
            // Pass only needed dependencies (DiscordNotifier)
            SingleTweetFileProcessor singleFileProcessor = new SingleTweetFileProcessor(dirManager, discordNotifier);
            tweetProcessor = new TweetProcessor(dirManager, singleFileProcessor); // Instantiate the main processor

            logger.info("Processing existing files in input directory and notifying Discord...");
            tweetProcessor.processInputFiles(); // This call now happens regardless of tweet fetch success
            // --- End Process Input Files ---


            logger.info("Main processing cycle complete.");

        } catch (Exception e) {
            // Catch initialization or other critical errors
            logger.error("A critical unhandled error occurred during execution: {}", e.getMessage(), e);
            System.exit(1); // Exit on critical failure
        } finally {
            // --- Shutdown ---
            logger.info("Initiating shutdown sequence...");
            if (discordNotifier != null) {
                discordNotifier.shutdown(); // Gracefully shut down JDA connection
            }
            // Add shutdown for other services if necessary
            logger.info("Application finished.");
        }
    }

    // Removed static helper methods readLastTweetId and writeLastTweetId

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
