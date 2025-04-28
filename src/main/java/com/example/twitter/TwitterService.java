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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        twitterClient.setAutomaticRetry(false);
        // Fetch user initially - consider error handling if this fails
        this.twitterUser = fetchTwitterUserObject();
    }

    // Helper method to fetch the User object
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


    public List<TweetData> fetchTimelineTweets(int maxResults) {
        if (this.twitterUser == null) {
            logger.error("Cannot fetch timeline because the initial user object fetch failed for {}", this.twitterUsername);
            return Collections.emptyList(); // Cannot proceed without user ID
        }
        String userId = this.twitterUser.getId();
        logger.info("Attempting to fetch timeline tweets for user: {} (ID: {}), max results: {}", twitterUsername, userId, maxResults);

        try {
            // Request expansions and media fields
            AdditionalParameters params = AdditionalParameters.builder()
                    .maxResults(Math.min(maxResults, 100))
                    .build();
            logger.debug("Fetching timeline with parameters: {}", params);

            TweetList tweetList = twitterClient.getUserTimeline(userId, params);

            if (tweetList == null || tweetList.getData() == null) {
                logger.warn("No tweets found or error fetching timeline for user ID: {}", userId);
                return Collections.emptyList();
            }
            logger.info("Successfully fetched {} tweets from timeline.", tweetList.getData().size());

            List<TweetData> tweetDataList = new ArrayList<>();
            List<TweetV2.MediaEntityV2> includedMedia = (tweetList.getIncludes() != null && tweetList.getIncludes().getMedia() != null)
                    ? tweetList.getIncludes().getMedia() : Collections.emptyList();

            // Prepare author details (use fetched user object)
            String authorName = this.twitterUser.getName(); // Display name
            String authorProfileUrl = "https://x.com/" + this.twitterUser.getName(); // Use username for URL
            String authorProfileImageUrl = this.twitterUser.getProfileImageUrl(); // URL fetched earlier

            for (Tweet tweet : tweetList.getData()) {
                String tweetUrl = "https://x.com/" + this.twitterUsername + "/status/" + tweet.getId();
                List<String> imageUrls = new ArrayList<>();

                if (tweet.getAttachments() != null && tweet.getAttachments().getMediaKeys() != null) {
                    List<String> mediaKeys = List.of(tweet.getAttachments().getMediaKeys()); // Convert array
                    logger.debug("Tweet {} has media keys: {}", tweet.getId(), mediaKeys);
                    imageUrls = includedMedia.stream()
                            .filter(media -> mediaKeys.contains(media.getKey()) && "photo".equals(media.getType()))
                            .map(TweetV2.MediaEntityV2::getUrl)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    logger.debug("Extracted image URLs for tweet {}: {}", tweet.getId(), imageUrls);
                } else {
                    logger.debug("Tweet {} has no media attachments.", tweet.getId());
                }

                // Create TweetData with author info
                tweetDataList.add(new TweetData(
                        tweet.getId(),
                        tweet.getText(),
                        tweetUrl,
                        imageUrls,
                        authorName,
                        authorProfileUrl,
                        authorProfileImageUrl
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
