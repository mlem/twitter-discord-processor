package com.example.core;

import com.example.config.AppConfig;
import com.example.args.CommandLineArgs; // Assuming CommandLineArgs is in com.example.args
import com.example.discord.DiscordNotifier;
import com.example.file.DirectoryManager;
import com.example.file.LastTweetIdManager;
import com.example.file.SingleTweetFileProcessor;
import com.example.file.TweetProcessor;
import com.example.file.TweetWriter;
import com.example.log.LogsDirLogBackPropertyDefiner; // Import log definer
import com.example.twitch.TwitchService;
import com.example.twitter.TwitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;

/**
 * Instantiates and holds references to all application services.
 * Acts as a simple dependency registry.
 */
public class ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    // Core Services
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
     * @throws RuntimeException If TwitchService initialization fails.
     */
    public ServiceRegistry(AppConfig config, CommandLineArgs args)
            throws IOException, LoginException, InterruptedException, RuntimeException {

        logger.info("Initializing Service Registry...");

        // 1. Directory and Logging Setup (must happen first)
        logger.debug("Initializing DirectoryManager...");
        this.directoryManager = new DirectoryManager(args.getBasePath());
        LogsDirLogBackPropertyDefiner.setDirectoryManager(this.directoryManager);
        System.setProperty("LOG_DIR", this.directoryManager.getLogsDir().toAbsolutePath().toString());
        logger.info("Log directory set to: {}", this.directoryManager.getLogsDir().toAbsolutePath());

        // 2. Initialize Managers and External API Services
        logger.debug("Initializing LastTweetIdManager...");
        this.lastTweetIdManager = new LastTweetIdManager(this.directoryManager);

        logger.debug("Initializing DiscordNotifier...");
        this.discordNotifier = new DiscordNotifier(config.getDiscordBotToken(), config.getDiscordChannelId());

        logger.debug("Initializing TwitchService...");
        this.twitchService = new TwitchService(config.getTwitchClientId(), config.getTwitchClientSecret());

        logger.debug("Initializing TwitterService...");
        this.twitterService = new TwitterService(config.getTwitterBearerToken(), config.getTwitterUsername());

        // 3. Initialize File/Processing Services
        logger.debug("Initializing TweetWriter...");
        this.tweetWriter = new TweetWriter(this.directoryManager.getInputDir());

        // SingleTweetFileProcessor now only needs DirectoryManager and DiscordNotifier
        logger.debug("Initializing SingleTweetFileProcessor...");
        this.singleTweetFileProcessor = new SingleTweetFileProcessor(this.directoryManager, this.discordNotifier);

        logger.debug("Initializing TweetProcessor...");
        this.tweetProcessor = new TweetProcessor(this.directoryManager, this.singleTweetFileProcessor);

        logger.info("Service Registry initialization complete.");
    }

    // --- Getters for Services ---
    public DirectoryManager getDirectoryManager() { return directoryManager; }
    public LastTweetIdManager getLastTweetIdManager() { return lastTweetIdManager; }
    public DiscordNotifier getDiscordNotifier() { return discordNotifier; }
    public TwitchService getTwitchService() { return twitchService; }
    public TwitterService getTwitterService() { return twitterService; }
    public TweetWriter getTweetWriter() { return tweetWriter; }
    public SingleTweetFileProcessor getSingleTweetFileProcessor() { return singleTweetFileProcessor; }
    public TweetProcessor getTweetProcessor() { return tweetProcessor; }
}
