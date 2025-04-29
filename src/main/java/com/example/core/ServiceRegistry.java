package com.example.core;

import com.example.config.AppConfig;
import com.example.args.CommandLineArgs;
import com.example.discord.DiscordNotifier;
import com.example.file.DirectoryManager;
import com.example.file.LastTweetIdManager;
import com.example.file.SingleTweetFileProcessor;
import com.example.file.TweetProcessor;
import com.example.file.TweetWriter;
import com.example.log.LogsDirLogBackPropertyDefiner;
import com.example.twitch.TwitchService;
import com.example.twitter.TwitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;

/**
 * Instantiates and holds references to all application services.
 * Acts as a simple dependency registry and manages service shutdown.
 */
public class ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    // Core Services (kept private)
    private final DirectoryManager directoryManager;
    private final LastTweetIdManager lastTweetIdManager;
    private final DiscordNotifier discordNotifier;
    private final TwitchService twitchService;
    private final TwitterService twitterService;
    private final TweetWriter tweetWriter;
    private final SingleTweetFileProcessor singleTweetFileProcessor;
    private final TweetProcessor tweetProcessor;

    /**
     * Initializes all services based on configuration and arguments.
     * Throws exceptions if essential services cannot be initialized.
     *
     * @param config Validated application configuration.
     * @param args Parsed command-line arguments.
     * @throws IOException If DirectoryManager fails.
     * @throws LoginException If DiscordNotifier fails login.
     * @throws InterruptedException If DiscordNotifier initialization is interrupted.
     * @throws RuntimeException If TwitchService or other critical initialization fails.
     */
    public ServiceRegistry(AppConfig config, CommandLineArgs args)
            throws IOException, LoginException, InterruptedException, RuntimeException {

        logger.info("Initializing Service Registry...");

        // 1. Directory and Logging Setup
        logger.debug("Initializing DirectoryManager...");
        this.directoryManager = new DirectoryManager(args.getBasePath());
        LogsDirLogBackPropertyDefiner.setDirectoryManager(this.directoryManager);
        System.setProperty("LOG_DIR", this.directoryManager.getLogsDir().toAbsolutePath().toString());
        logger.info("Log directory set to: {}", this.directoryManager.getLogsDir().toAbsolutePath());

        // 2. Initialize Managers and External API Services
        logger.debug("Initializing LastTweetIdManager...");
        this.lastTweetIdManager = new LastTweetIdManager(this.directoryManager);

        logger.debug("Initializing DiscordNotifier...");
        // Assign to local variable first to handle potential null in shutdown if constructor fails partially
        DiscordNotifier tempDiscordNotifier = null;
        try {
            tempDiscordNotifier = new DiscordNotifier(config.getDiscordBotToken(), config.getDiscordChannelId());
        } finally {
            // Ensure discordNotifier field is set even if constructor threw an exception
            // (though it would likely propagate up)
            this.discordNotifier = tempDiscordNotifier;
        }


        logger.debug("Initializing TwitchService...");
        this.twitchService = new TwitchService(config.getTwitchClientId(), config.getTwitchClientSecret());

        logger.debug("Initializing TwitterService...");
        this.twitterService = new TwitterService(config.getTwitterBearerToken(), config.getTwitterUsername());

        // 3. Initialize File/Processing Services
        logger.debug("Initializing TweetWriter...");
        this.tweetWriter = new TweetWriter(this.directoryManager.getInputDir());

        logger.debug("Initializing SingleTweetFileProcessor...");
        this.singleTweetFileProcessor = new SingleTweetFileProcessor(this.directoryManager, this.discordNotifier);

        logger.debug("Initializing TweetProcessor...");
        this.tweetProcessor = new TweetProcessor(this.directoryManager, this.singleTweetFileProcessor);

        logger.info("Service Registry initialization complete.");
    }

    /**
     * Gracefully shuts down services that require it (e.g., closing connections).
     */
    public void shutdown() {
        logger.info("Shutting down registered services...");

        // Shut down DiscordNotifier
        if (this.discordNotifier != null) {
            try {
                logger.debug("Attempting to shut down DiscordNotifier...");
                this.discordNotifier.shutdown();
            } catch (Exception e) {
                logger.error("Error during DiscordNotifier shutdown: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("DiscordNotifier was null, skipping its shutdown.");
        }

        // Add shutdown logic for other services here if needed in the future
        // e.g., if TwitchService held persistent connections:
        // if (this.twitchService != null) {
        //     try {
        //         logger.debug("Attempting to shut down TwitchService...");
        //         // this.twitchService.close(); // Assuming a close() method exists
        //     } catch (Exception e) {
        //         logger.error("Error during TwitchService shutdown: {}", e.getMessage(), e);
        //     }
        // }

        logger.info("Service shutdown sequence complete.");
    }


    // --- Getters for Services (needed by ApplicationService) ---
    // Consider making these package-private if only core package needs them
    public DirectoryManager getDirectoryManager() { return directoryManager; }
    public LastTweetIdManager getLastTweetIdManager() { return lastTweetIdManager; }
    // public DiscordNotifier getDiscordNotifier() { return discordNotifier; } // Might not be needed externally now
    public TwitchService getTwitchService() { return twitchService; }
    public TwitterService getTwitterService() { return twitterService; }
    public TweetWriter getTweetWriter() { return tweetWriter; }
    // public SingleTweetFileProcessor getSingleTweetFileProcessor() { return singleTweetFileProcessor; } // Internal detail?
    public TweetProcessor getTweetProcessor() { return tweetProcessor; }
}
