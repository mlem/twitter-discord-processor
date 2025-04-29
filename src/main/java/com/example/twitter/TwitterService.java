package com.example.twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.user.User; // Keep User import
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime; // Import LocalDateTime
import java.util.ArrayList;
import java.util.Arrays; // Import Arrays
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional; // Import Optional
import java.util.stream.Collectors;

public class TwitterService {

    private static final Logger logger = LoggerFactory.getLogger(TwitterService.class);

    private final TwitterClient twitterClient;
    private final String twitterUsername;
    private User twitterUser; // Store the fetched user object

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
        // Fetch user initially - consider error handling if this fails
        this.twitterUser = fetchTwitterUserObject();
    }

    // Helper method to fetch the User object (including profile image)
    private User fetchTwitterUserObject() {
        logger.info("Fetching Twitter user object for username: {}", this.twitterUsername);
        try {
            User user = twitterClient.getUserFromUserName(this.twitterUsername);
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
     * Fetches timeline tweets since a given ID.
     *
     * @param maxResults Max tweets to fetch.
     * @param sinceId Optional containing the ID of the earliest tweet to fetch (exclusive).
     * @param twitchUsername Configured Twitch username for context.
     * @param twitchProfileImageUrl Fetched Twitch profile image URL.
     * @param twitchChannelUrl Fetched Twitch channel URL.
     * @return List of TweetData objects containing all context.
     */
    // Updated signature based on previous logic
    public List<TweetData> fetchTimelineTweets(int maxResults,
                                               Optional<String> sinceId, // Added sinceId parameter
                                               String twitchUsername,
                                               String twitchProfileImageUrl,
                                               String twitchChannelUrl) {
        // Use the stored twitterUser object
        if (this.twitterUser == null) {
            logger.error("Cannot fetch timeline because the user object is not available for {}", this.twitterUsername);
            return Collections.emptyList(); // Cannot proceed without user ID
        }
        String userId = this.twitterUser.getId();
        logger.info("Attempting to fetch timeline tweets for user: {} (ID: {}), max results: {}, since_id: {}",
                twitterUsername, userId, maxResults, sinceId.orElse("None"));

        try {
            // Build parameters, including sinceId if present
            AdditionalParameters.AdditionalParametersBuilder paramsBuilder = AdditionalParameters.builder()
                    .maxResults(Math.min(maxResults, 100));

            // Add sinceId if it's present
            sinceId.ifPresent(paramsBuilder::sinceId);

            AdditionalParameters params = paramsBuilder.build();
            logger.debug("Fetching timeline with parameters: {}", params);

            TweetList tweetList = twitterClient.getUserTimeline(userId, params);

            if (tweetList == null || tweetList.getData() == null) {
                // This can happen normally if there are no new tweets since the sinceId
                logger.info("No new tweets found or error fetching timeline for user ID: {} since ID: {}", userId, sinceId.orElse("None"));
                return Collections.emptyList();
            }
            logger.info("Successfully fetched {} tweets from timeline.", tweetList.getData().size());

            List<TweetData> tweetDataList = new ArrayList<>();
            // Use TweetV2.MediaEntityV2 as per user's version
            List<TweetV2.MediaEntityV2> includedMedia = (tweetList.getIncludes() != null && tweetList.getIncludes().getMedia() != null)
                    ? tweetList.getIncludes().getMedia() : Collections.emptyList();

            // Prepare author details (use stored user object)
            String authorName = this.twitterUser.getName();
            String authorProfileUrl = "https://x.com/" + this.twitterUser.getName();
            String authorProfileImageUrl = this.twitterUser.getProfileImageUrl();

            for (Tweet tweet : tweetList.getData()) {
                String tweetUrl = "https://x.com/" + this.twitterUsername + "/status/" + tweet.getId();
                LocalDateTime createdAt = tweet.getCreatedAt(); // Get createdAt timestamp
                List<String> imageUrls = new ArrayList<>();

                if (tweet.getAttachments() != null && tweet.getAttachments().getMediaKeys() != null) {
                    // Use Arrays.asList for compatibility
                    List<String> mediaKeys = Arrays.asList(tweet.getAttachments().getMediaKeys());
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


                // Create TweetData with all context (using constructor from user's fixed TweetData)
                tweetDataList.add(new TweetData(
                        // Core Tweet
                        tweet.getId(),
                        tweet.getText(),
                        tweetUrl,
                        imageUrls,
                        createdAt,
                        // Author
                        authorName,
                        authorProfileUrl,
                        authorProfileImageUrl,
                        // Twitch
                        twitchUsername,
                        twitchProfileImageUrl,
                        twitchChannelUrl,
                        // Additional Raw Tweet Fields (matching user's TweetData constructor)
                        tweet.getAuthorId(),
                        tweet.getConversationId(),
                        tweet.getLang(),
                        tweet.getSource(),
                        tweet.getReplySettings().toString(),
                        tweet.getInReplyToUserId(),
                        safeToString(tweet.getEntities()),
                        safeToString(tweet.getAttachments()),
                        safeToString(tweet.getGeo())
                        // Removed fields based on user's TweetData constructor
                        // tweet.isPossiblySensitive(),
                        // safeToString(tweet.getPublicMetrics()),
                        // safeToString(tweet.getReferencedTweets()),
                        // safeToString(tweet.getWithheld())
                ));
            }
            logger.info("Processed {} tweets into TweetData objects.", tweetDataList.size());
            return tweetDataList;

        } catch (Exception e) {
            logger.error("Error fetching Twitter timeline for user {}: {}", twitterUsername, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
