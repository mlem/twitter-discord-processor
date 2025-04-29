package com.example;

import com.example.config.PropertiesLoader;
import com.example.discord.DiscordNotifier;
import com.example.file.DirectoryManager;
import com.example.file.SingleTweetFileProcessor;
import com.example.file.TweetProcessor;
import com.example.file.TweetWriter;
import com.example.log.LogsDirLogBackPropertyDefiner;
import com.example.twitch.TwitchService;
import com.example.twitch.TwitchUserInfo;
import com.example.twitter.TweetData;
import com.example.twitter.TwitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
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
                basePath = DirectoryManager.basePathRelativeToJar();
                logger.info("No base path provided. Using default relative to JAR: {}", basePath);
            } catch (SecurityException | NullPointerException e) {
                logger.warn("Error determining JAR file location for default path: {}. Falling back to current working directory.", e.getMessage());
                basePath = Paths.get("data").toAbsolutePath().toString();
                logger.warn("Using default path in current working directory: {}", basePath);
            }
        }
        if (args.length >= 2 && args[1] != null && !args[1].trim().isEmpty()) {
            try {
                maxTweetsToFetch = Integer.parseInt(args[1]);
                if (maxTweetsToFetch <= 0) {
                    logger.warn("Invalid number for max tweets ({}). Using default: {}", args[1], DEFAULT_MAX_TWEETS);
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
        TweetProcessor tweetProcessor = null; // Declare TweetProcessor earlier

        try {
            // --- Setup Directories and Logging Path ---
            dirManager = new DirectoryManager(basePath);
            LogsDirLogBackPropertyDefiner.setDirectoryManager(dirManager);
            System.setProperty("LOG_DIR", dirManager.getLogsDir().toAbsolutePath().toString());
            logger.info("Log directory set to: {}", dirManager.getLogsDir().toAbsolutePath());

            // --- Configuration Loading ---
            logger.info("Loading configuration...");
            PropertiesLoader propsLoader = new PropertiesLoader();
            String twitterBearerToken = System.getenv("TWITTER_BEARER_TOKEN");
            String twitterUsernameEnv = System.getenv("TWITTER_USERNAME");
            String discordBotToken = System.getenv("DISCORD_BOT_TOKEN");
            String discordChannelId = propsLoader.getProperty("discord.channel.id");
            String twitchClientId = System.getenv("TWITCH_CLIENT_ID");
            String twitchClientSecret = System.getenv("TWITCH_CLIENT_SECRET");
            String twitchUsername = propsLoader.getProperty("twitch.username");

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

            // Validation
            if (isNullOrEmpty(twitterBearerToken) || isNullOrEmpty(twitterUsername) ||
                    isNullOrEmpty(discordBotToken) || isNullOrEmpty(discordChannelId) ||
                    isNullOrEmpty(twitchClientId) || isNullOrEmpty(twitchClientSecret) ||
                    isNullOrEmpty(twitchUsername)) {
                logger.error("Missing required configuration. Exiting.");
                // ... (error messages remain the same) ...
                System.exit(1);
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
            // --- End Fetch Twitch Info ---


            // --- Attempt to Fetch and Write Tweets ---
            // This block can fail without stopping the subsequent processing step
            try {
                logger.info("Attempting to fetch up to {} tweets for user: {}", maxTweetsToFetch, twitterUsername);

                // Extract Twitch details safely
                String twitchLogoUrl = twitchInfoOpt.map(TwitchUserInfo::profileImageUrl).orElse(null);
                String twitchChanUrl = twitchInfoOpt.map(TwitchUserInfo::channelUrl).orElse(null);

                List<TweetData> tweets = twitterService.fetchTimelineTweets(
                        maxTweetsToFetch,
                        twitchUsername,
                        twitchLogoUrl,
                        twitchChanUrl
                );

                if (tweets.isEmpty()) {
                    logger.info("No new tweets fetched or an error occurred during fetch.");
                } else {
                    logger.info("Fetched {} tweets.", tweets.size());
                    logger.info("Writing tweets to input directory: {}", dirManager.getInputDir());
                    for (TweetData tweet : tweets) {
                        tweetWriter.writeTweetToFile(tweet);
                    }
                }
            } catch (Exception e) {
                // Log the error but allow the program to continue to the processing step
                logger.error("Error occurred during tweet fetching or writing: {}. Proceeding to process existing files.", e.getMessage(), e);
            }
            // --- End Fetch and Write Tweets ---


            // --- Process Input Files (Always Run) ---
            // Instantiate processors right before processing
            logger.info("Initializing file processors...");
            SingleTweetFileProcessor singleFileProcessor = new SingleTweetFileProcessor(dirManager, discordNotifier); // Pass only needed dependencies
            tweetProcessor = new TweetProcessor(dirManager, singleFileProcessor); // Instantiate the main processor

            logger.info("Processing existing files in input directory and notifying Discord...");
            tweetProcessor.processInputFiles(); // This call now happens regardless of tweet fetch success
            // --- End Process Input Files ---


            logger.info("Main processing cycle complete.");

        } catch (Exception e) {
            // Catch initialization or other critical errors
            logger.error("A critical unhandled error occurred during execution: {}", e.getMessage(), e);
            System.exit(1);
        } finally {
            // --- Shutdown ---
            logger.info("Initiating shutdown sequence...");
            if (discordNotifier != null) {
                discordNotifier.shutdown();
            }
            // Add shutdown for other services if necessary (e.g., TwitchClient if it held resources)
            logger.info("Application finished.");
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
