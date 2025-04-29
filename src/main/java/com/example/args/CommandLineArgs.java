package com.example.args;

import com.example.file.DirectoryManager; // Needed for default path logic
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

/**
 * Parses and holds command-line arguments for the application.
 */
public class CommandLineArgs {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineArgs.class);
    private static final int DEFAULT_MAX_TWEETS = 10;

    private final String basePath;
    private final int maxTweetsToFetch;

    // Private constructor, use parse method
    private CommandLineArgs(String basePath, int maxTweetsToFetch) {
        this.basePath = basePath;
        this.maxTweetsToFetch = maxTweetsToFetch;
    }

    /**
     * Parses the command-line arguments array.
     *
     * @param args The String array from the main method.
     * @return An instance of CommandLineArgs.
     */
    public static CommandLineArgs parse(String[] args) {
        logger.debug("Parsing command line arguments...");
        String parsedBasePath;
        int parsedMaxTweets = DEFAULT_MAX_TWEETS;

        // Argument 0: Base Path (Optional)
        if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
            parsedBasePath = args[0];
            logger.info("Using provided base path: {}", parsedBasePath);
        } else {
            // Determine default base path (relative to JAR)
            try {
                parsedBasePath = DirectoryManager.basePathRelativeToJar();
                logger.info("No base path provided. Using default relative to JAR: {}", parsedBasePath);
            } catch (SecurityException | NullPointerException e) {
                logger.warn("Error determining JAR file location for default path: {}. Falling back to current working directory.", e.getMessage());
                parsedBasePath = Paths.get("data").toAbsolutePath().toString();
                logger.warn("Using default path in current working directory: {}", parsedBasePath);
            }
        }

        // Argument 1: Max Tweets (Optional)
        if (args.length >= 2 && args[1] != null && !args[1].trim().isEmpty()) {
            try {
                parsedMaxTweets = Integer.parseInt(args[1]);
                if (parsedMaxTweets <= 0) {
                    logger.warn("Invalid number provided for max tweets ({}). Must be positive. Using default: {}", args[1], DEFAULT_MAX_TWEETS);
                    parsedMaxTweets = DEFAULT_MAX_TWEETS;
                } else {
                    if (parsedMaxTweets > 100) {
                        logger.warn("Requested max tweets ({}) exceeds typical API limit (100). Will fetch up to 100.", parsedMaxTweets);
                        parsedMaxTweets = 100; // Cap at a reasonable limit
                    }
                    logger.info("Using provided max tweets to fetch: {}", parsedMaxTweets);
                }
            } catch (NumberFormatException e) {
                logger.warn("Could not parse second argument '{}' as a number for max tweets. Using default: {}", args[1], DEFAULT_MAX_TWEETS, e);
                parsedMaxTweets = DEFAULT_MAX_TWEETS;
            }
        } else {
            logger.info("No max tweets argument provided. Using default: {}", DEFAULT_MAX_TWEETS);
        }

        return new CommandLineArgs(parsedBasePath, parsedMaxTweets);
    }

    // --- Getters ---
    public String getBasePath() {
        return basePath;
    }

    public int getMaxTweetsToFetch() {
        return maxTweetsToFetch;
    }
}
