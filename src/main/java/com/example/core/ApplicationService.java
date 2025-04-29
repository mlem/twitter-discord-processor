package com.example.core;

import com.example.config.AppConfig; // Need config for usernames
import com.example.args.CommandLineArgs; // Need args for maxTweets
import com.example.twitch.TwitchUserInfo;
import com.example.twitter.TweetData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the main application workflow using services from the ServiceRegistry.
 */
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    // Dependencies injected from ServiceRegistry
    private final AppConfig appConfig;
    private final CommandLineArgs commandLineArgs;
    private final ServiceRegistry services;


    /**
     * Constructor requires the configuration, args, and the service registry.
     * @param appConfig The application configuration.
     * @param commandLineArgs The parsed command line arguments.
     * @param services The registry containing initialized services.
     */
    public ApplicationService(AppConfig appConfig, CommandLineArgs commandLineArgs, ServiceRegistry services) {
        this.appConfig = appConfig;
        this.commandLineArgs = commandLineArgs;
        this.services = services;
    }

    /**
     * Executes one cycle of the application's core logic:
     * - Fetches Twitch info.
     * - Reads last tweet ID.
     * - Fetches new tweets.
     * - Writes new tweets to files.
     * - Updates last tweet ID.
     * - Processes files in the input directory.
     */
    public void runCycle() {
        logger.info("Starting application cycle...");

        // --- Fetch Twitch Info Once ---
        Optional<TwitchUserInfo> twitchInfoOpt = fetchTwitchInfo();
        String twitchLogoUrl = twitchInfoOpt.map(TwitchUserInfo::profileImageUrl).orElse(null);
        String twitchChanUrl = twitchInfoOpt.map(TwitchUserInfo::channelUrl).orElse(null);
        // --- End Fetch Twitch Info ---

        // --- Read Last Tweet ID ---
        Optional<String> sinceId = services.getLastTweetIdManager().readLastTweetId();
        // --- End Read Last Tweet ID ---

        // --- Attempt to Fetch and Write Tweets ---
        List<TweetData> fetchedTweets = fetchAndWriteTweets(
                sinceId,
                appConfig.getTwitchUsername(), // Use config for Twitch username
                twitchLogoUrl,
                twitchChanUrl
        );
        // --- End Fetch and Write Tweets ---

        // --- Process Input Files (Always Run) ---
        processInputFiles();
        // --- End Process Input Files ---

        logger.info("Application cycle finished.");
    }

    /**
     * Helper method to fetch Twitch user info.
     * @return Optional containing TwitchUserInfo.
     */
    private Optional<TwitchUserInfo> fetchTwitchInfo() {
        Optional<TwitchUserInfo> twitchInfoOpt = Optional.empty();
        try {
            logger.info("Fetching Twitch user info for configured user: {}", appConfig.getTwitchUsername());
            twitchInfoOpt = services.getTwitchService().fetchUserInfo(appConfig.getTwitchUsername());
            if (!twitchInfoOpt.isPresent()) {
                logger.warn("Could not fetch Twitch user info for {}. Embeds will not include Twitch thumbnail.", appConfig.getTwitchUsername());
            }
        } catch (Exception e) {
            logger.error("Error fetching Twitch user info for {}: {}", appConfig.getTwitchUsername(), e.getMessage(), e);
        }
        return twitchInfoOpt;
    }

    /**
     * Helper method to fetch new tweets, write them to files, and update the last ID.
     * @param sinceId Optional last tweet ID.
     * @param twitchUsername Configured Twitch username.
     * @param twitchLogoUrl Fetched Twitch logo URL.
     * @param twitchChanUrl Fetched Twitch channel URL.
     * @return The list of fetched tweets (can be empty).
     */
    private List<TweetData> fetchAndWriteTweets(Optional<String> sinceId, String twitchUsername, String twitchLogoUrl, String twitchChanUrl) {
        List<TweetData> fetchedTweets = Collections.emptyList();
        String highestFetchedId = null;
        try {
            logger.info("Attempting to fetch up to {} tweets for user {} since ID {}",
                    commandLineArgs.getMaxTweetsToFetch(), appConfig.getTwitterUsername(), sinceId.orElse("None"));

            fetchedTweets = services.getTwitterService().fetchTimelineTweets(
                    commandLineArgs.getMaxTweetsToFetch(),
                    sinceId,
                    twitchUsername,
                    twitchLogoUrl,
                    twitchChanUrl
            );

            if (fetchedTweets.isEmpty()) {
                logger.info("No new tweets fetched since ID {} or an error occurred.", sinceId.orElse("start"));
            } else {
                logger.info("Fetched {} new tweets.", fetchedTweets.size());
                logger.info("Writing tweets to input directory: {}", services.getDirectoryManager().getInputDir());

                highestFetchedId = fetchedTweets.stream()
                        .map(TweetData::getId)
                        .max(Comparator.naturalOrder())
                        .orElse(null);

                for (TweetData tweet : fetchedTweets) {
                    services.getTweetWriter().writeTweetToFile(tweet);
                }

                if (highestFetchedId != null) {
                    services.getLastTweetIdManager().writeLastTweetId(highestFetchedId);
                } else {
                    logger.warn("Could not determine the highest ID from the fetched tweets.");
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred during tweet fetching or writing: {}. Proceeding to process existing files.", e.getMessage(), e);
        }
        return fetchedTweets; // Return the list (might be empty)
    }

    /**
     * Helper method to trigger the processing of files in the input directory.
     */
    private void processInputFiles() {
        try {
            logger.info("Processing existing files in input directory and notifying Discord...");
            services.getTweetProcessor().processInputFiles();
        } catch (Exception e) {
            logger.error("Error occurred during input file processing: {}", e.getMessage(), e);
            // Decide if this error should halt the application or just be logged
        }
    }
}
