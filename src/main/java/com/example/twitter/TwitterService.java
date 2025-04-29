package com.example.twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.tweet.TweetV2; // User's version uses this
import io.github.redouane59.twitter.dto.user.User;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime; // Added based on previous logic
import java.util.ArrayList;
import java.util.Arrays; // User's version uses this
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TwitterService {

    private static final Logger logger = LoggerFactory.getLogger(TwitterService.class);

    private final TwitterClient twitterClient;
    private final String twitterUsername;
    // User object is fetched per-request in user's version

    public TwitterService(String bearerToken, String username) {
        if (bearerToken == null || username == null) {
            logger.error("Twitter Bearer Token and Username must be provided.");
            throw new IllegalArgumentException("Twitter Bearer Token and Username must be provided.");
        }
        this.twitterUsername = username;
        logger.info("Initializing TwitterService for user: {}", twitterUsername);
        TwitterCredentials credentials = TwitterCredentials.builder()
                .bearerToken(bearerToken)
                .build();
        this.twitterClient = new TwitterClient(credentials);
        twitterClient.setAutomaticRetry(false); // Added by user
    }

    // Helper method to fetch the User object (including profile image)
    private User fetchTwitterUserObject() {
        logger.info("Fetching Twitter user object for username: {}", this.twitterUsername);
        try {
            User user = twitterClient.getUserFromUserName(this.twitterUsername); // Pass params here
            if (user == null) {
                logger.error("Failed to retrieve user data object for username: {}", this.twitterUsername);
            } else {
                logger.info("Successfully fetched user object for {}. Profile Image URL: {}",
                        this.twitterUsername, user.getProfileImageUrl());
            }
            return user;
        } catch (Exception e) {
            logger.error("Exception fetching Twitter user object for {}: {}", this.twitterUsername, e.getMessage(), e);
            return null; // Return null if fetching fails
        }
    }


    // Helper function to safely get toString or "null"
    private String safeToString(Object obj) {
        return obj == null ? "null" : obj.toString();
    }

    /**
     * Fetches timeline tweets and bundles them with author and Twitch context.
     *
     * @param maxResults Max tweets to fetch.
     * @param twitchUsername Configured Twitch username for context.
     * @param twitchProfileImageUrl Fetched Twitch profile image URL.
     * @param twitchChannelUrl Fetched Twitch channel URL.
     * @return List of TweetData objects containing all context.
     */
    // Updated signature based on previous logic
    public List<TweetData> fetchTimelineTweets(int maxResults,
                                               String twitchUsername,
                                               String twitchProfileImageUrl,
                                               String twitchChannelUrl) {
        User twitterUser = fetchTwitterUserObject(); // Fetch fresh user object each time

        if (twitterUser == null) {
            logger.error("Cannot fetch timeline because the user object fetch failed for {}", this.twitterUsername);
            return Collections.emptyList(); // Cannot proceed without user ID
        }
        String userId = twitterUser.getId();
        logger.info("Attempting to fetch timeline tweets for user: {} (ID: {}), max results: {}", twitterUsername, userId, maxResults);

        try {
            TweetList tweetList = twitterClient.getUserTimeline(userId);

            if (tweetList == null || tweetList.getData() == null) {
                logger.warn("No tweets found or error fetching timeline for user ID: {}", userId);
                return Collections.emptyList();
            }
            logger.info("Successfully fetched {} tweets from timeline.", tweetList.getData().size());

            List<TweetData> tweetDataList = new ArrayList<>();
            // Use TweetV2.MediaEntityV2 as per user's version
            List<TweetV2.MediaEntityV2> includedMedia = (tweetList.getIncludes() != null && tweetList.getIncludes().getMedia() != null)
                    ? tweetList.getIncludes().getMedia() : Collections.emptyList();

            // Prepare author details (using the freshly fetched user object)
            String authorName = twitterUser.getName();
            String authorProfileUrl = "https://x.com/" + twitterUser.getName();
            String authorProfileImageUrl = twitterUser.getProfileImageUrl();

            for (Tweet tweet : tweetList.getData()) {
                String tweetUrl = "https://x.com/" + this.twitterUsername + "/status/" + tweet.getId();
                LocalDateTime createdAt = tweet.getCreatedAt(); // Get createdAt timestamp
                List<String> imageUrls = new ArrayList<>();

                if (tweet.getAttachments() != null && tweet.getAttachments().getMediaKeys() != null) {
                    // Use Arrays.stream().toList() as per user's version
                    List<String> mediaKeys = Arrays.stream(tweet.getAttachments().getMediaKeys()).toList();
                    logger.debug("Tweet {} has media keys: {}", tweet.getId(), mediaKeys);
                    imageUrls = includedMedia.stream()
                            // Use getKey() as per user's version (TweetV2.MediaEntityV2)
                            .filter(media -> mediaKeys.contains(media.getKey()) && "photo".equals(media.getType()))
                            .map(TweetV2.MediaEntityV2::getUrl)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    logger.debug("Extracted image URLs for tweet {}: {}", tweet.getId(), imageUrls);
                } else {
                    logger.debug("Tweet {} has no media attachments.", tweet.getId());
                }


                // Create TweetData with all context (based on previous logic)
                tweetDataList.add(new TweetData(
                        // Core Tweet
                        tweet.getId(),
                        tweet.getText(),
                        tweetUrl, // Derived URL
                        imageUrls, // Derived image URLs
                        createdAt,
                        // Author
                        authorName,
                        authorProfileUrl, // Derived URL
                        authorProfileImageUrl,
                        // Twitch
                        twitchUsername,
                        twitchProfileImageUrl,
                        twitchChannelUrl,
                        // Additional Raw Tweet Fields
                        tweet.getAuthorId(),
                        tweet.getConversationId(),
                        tweet.getLang(),
                        tweet.getSource(),
                        tweet.getReplySettings().toString(),
                        tweet.getInReplyToUserId(),
                        // Convert complex objects to string
                        safeToString(tweet.getEntities()),
                        safeToString(tweet.getAttachments()),
                        safeToString(tweet.getGeo())
                        )
                );
            }
            logger.info("Processed {} tweets into TweetData objects.", tweetDataList.size());
            return tweetDataList;

        } catch (Exception e) {
            logger.error("Error fetching Twitter timeline for user {}: {}", twitterUsername, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
