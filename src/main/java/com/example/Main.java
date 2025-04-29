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

import java.net.URISyntaxException; // Keep if using DirectoryManager.basePathRelativeToJar
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
            String twitchUsername = propsLoader.getProperty("twitch.username"); // Configured Twitch username

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
                logger.error("Missing required configuration.");
                // ... (error messages remain the same) ...
                System.exit(1);
            }
            logger.info("Configuration loaded successfully.");

            // --- Service Initialization ---
            logger.info("Initializing services...");
            TwitterService twitterService = new TwitterService(twitterBearerToken, twitterUsername);
            TweetWriter tweetWriter = new TweetWriter(dirManager.getInputDir());
            discordNotifier = new DiscordNotifier(discordBotToken, discordChannelId);
            twitchService = new TwitchService(twitchClientId, twitchClientSecret);

            // --- Fetch Twitch Info Once ---
            logger.info("Fetching Twitch user info for configured user: {}", twitchUsername);
            Optional<TwitchUserInfo> twitchInfoOpt = twitchService.fetchUserInfo(twitchUsername);
            if (!twitchInfoOpt.isPresent()) {
                logger.warn("Could not fetch Twitch user info for {}. Embeds will not include Twitch thumbnail.", twitchUsername);
            }
            // Extract info or use null if not present
            String twitchLogoUrl = twitchInfoOpt.map(TwitchUserInfo::profileImageUrl).orElse(null);
            String twitchChanUrl = twitchInfoOpt.map(TwitchUserInfo::channelUrl).orElse(null);
            // --- End Fetch Twitch Info ---

            // Instantiate processors (SingleTweetFileProcessor no longer needs Twitch info)
            SingleTweetFileProcessor singleFileProcessor = new SingleTweetFileProcessor(dirManager, discordNotifier);
            TweetProcessor tweetProcessor = new TweetProcessor(dirManager, singleFileProcessor);

            // --- Core Logic ---
            logger.info("Fetching up to {} tweets for user: {}", maxTweetsToFetch, twitterUsername);
            // Pass Twitch info when creating TweetData objects within fetchTimelineTweets
            // NOTE: This requires modifying TwitterService to accept twitch info or modifying Main to create TweetData here.
            // Let's modify Main to create TweetData here for simplicity, assuming TwitterService returns raw API objects.
            // *** This requires a significant change in TwitterService's return type or this loop's logic. ***
            // *** Reverting to the previous approach where TwitterService creates TweetData, but now passing Twitch info ***

            // Fetch tweets (TwitterService needs modification to accept Twitch info for TweetData creation)
            List<TweetData> tweets = twitterService.fetchTimelineTweets(
                    maxTweetsToFetch,
                    twitchUsername, // Pass configured Twitch username
                    twitchLogoUrl,  // Pass fetched logo URL
                    twitchChanUrl   // Pass fetched channel URL
            );


            if (tweets.isEmpty()) {
                logger.info("No new tweets fetched or an error occurred during fetch.");
            } else {
                logger.info("Fetched {} tweets.", tweets.size());
                logger.info("Writing tweets to input directory: {}", dirManager.getInputDir());
                for (TweetData tweet : tweets) {
                    // TweetData object now contains all necessary context
                    tweetWriter.writeTweetToFile(tweet);
                }
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
