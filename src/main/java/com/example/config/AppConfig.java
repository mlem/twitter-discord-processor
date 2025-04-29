package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Holds validated application configuration values loaded from environment
 * variables and the properties file.
 */
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private final String twitterBearerToken;
    private final String discordBotToken;
    private final String discordChannelId;
    private final String twitchClientId;
    private final String twitchClientSecret;
    private final String twitchUsername; // Configured Twitch username for logo
    private final String twitterUsername; // Resolved Twitter username to use

    // Private constructor - use factory method to create
    private AppConfig(String twitterBearerToken, String discordBotToken, String discordChannelId,
                      String twitchClientId, String twitchClientSecret, String twitchUsername,
                      String twitterUsername) {
        this.twitterBearerToken = twitterBearerToken;
        this.discordBotToken = discordBotToken;
        this.discordChannelId = discordChannelId;
        this.twitchClientId = twitchClientId;
        this.twitchClientSecret = twitchClientSecret;
        this.twitchUsername = twitchUsername;
        this.twitterUsername = twitterUsername;
    }

    /**
     * Loads configuration from environment variables and properties, validates it,
     * and returns an AppConfig instance if valid.
     *
     * @return Optional containing AppConfig if validation passes, Optional.empty() otherwise.
     */
    public static Optional<AppConfig> loadAndValidate() {
        logger.info("Loading and validating application configuration...");
        PropertiesLoader propsLoader = new PropertiesLoader();
        ConfigurationValidator validator = new ConfigurationValidator(propsLoader);

        if (!validator.validate()) {
            logger.error("Configuration validation failed:\n{}", validator.getFormattedErrorSummary());
            return Optional.empty(); // Validation failed
        }

        // Validation passed, load validated values
        String twitterBearerToken = System.getenv("TWITTER_BEARER_TOKEN");
        String discordBotToken = System.getenv("DISCORD_BOT_TOKEN");
        String discordChannelId = propsLoader.getProperty("discord.channel.id");
        String twitchClientId = System.getenv("TWITCH_CLIENT_ID");
        String twitchClientSecret = System.getenv("TWITCH_CLIENT_SECRET");
        String twitchUsername = propsLoader.getProperty("twitch.username");

        // Determine Twitter Username (we know at least one is present due to validation)
        String twitterUsername = System.getenv("TWITTER_USERNAME");
        String usernameSource;
        if (isNullOrBlank(twitterUsername)) {
            twitterUsername = propsLoader.getProperty("twitter.username");
            usernameSource = "config.properties";
        } else {
            usernameSource = "environment variable";
        }
        logger.info("Using Twitter username '{}' (from {})", twitterUsername, usernameSource);

        logger.info("Configuration loaded and validated successfully.");
        return Optional.of(new AppConfig(
                twitterBearerToken, discordBotToken, discordChannelId,
                twitchClientId, twitchClientSecret, twitchUsername,
                twitterUsername
        ));
    }

    // Helper method consistent with validator
    private static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    // --- Getters ---
    public String getTwitterBearerToken() { return twitterBearerToken; }
    public String getDiscordBotToken() { return discordBotToken; }
    public String getDiscordChannelId() { return discordChannelId; }
    public String getTwitchClientId() { return twitchClientId; }
    public String getTwitchClientSecret() { return twitchClientSecret; }
    public String getTwitchUsername() { return twitchUsername; }
    public String getTwitterUsername() { return twitterUsername; }
}
