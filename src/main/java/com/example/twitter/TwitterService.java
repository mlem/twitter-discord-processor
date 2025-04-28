package com.example.twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.user.User;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import java.util.*;
import java.util.stream.Collectors;

public class TwitterService {

    // Initialize Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(TwitterService.class);

    private final TwitterClient twitterClient;
    private final String twitterUsername;

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
    }

    public List<TweetData> fetchTimelineTweets(int maxResults) {
        logger.info("Attempting to fetch timeline tweets for user: {}, max results: {}", twitterUsername, maxResults);
        try {
            User me = twitterClient.getUserFromUserName(twitterUsername);
            if (me == null) {
                logger.error("Failed to retrieve user data for username: {}", twitterUsername);
                return Collections.emptyList();
            }
            String userId = me.getId();
            logger.debug("Found user ID: {} for username: {}", userId, twitterUsername);

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
            if (tweetList.getIncludes() != null && tweetList.getIncludes().getMedia() != null) {
                logger.debug("Included media count: {}", tweetList.getIncludes().getMedia().size());
            } else {
                logger.debug("No included media found in the response.");
            }


            List<TweetData> tweetDataList = new ArrayList<>();
            List<TweetV2.MediaEntityV2> includedMedia = (tweetList.getIncludes() != null && tweetList.getIncludes().getMedia() != null)
                    ? tweetList.getIncludes().getMedia() : Collections.emptyList();

            for (Tweet tweet : tweetList.getData()) {
                String tweetUrl = "https://x.com/" + twitterUsername + "/status/" + tweet.getId();
                List<String> imageUrls = new ArrayList<>();

                if (tweet.getAttachments() != null && tweet.getAttachments().getMediaKeys() != null) {
                    List<String> mediaKeys = Arrays.stream(tweet.getAttachments().getMediaKeys()).toList();
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

                tweetDataList.add(new TweetData(tweet.getId(), tweet.getText(), tweetUrl, imageUrls));
            }
            logger.info("Processed {} tweets into TweetData objects.", tweetDataList.size());
            return tweetDataList;

        } catch (Exception e) {
            // Log the exception with stack trace
            logger.error("Error fetching Twitter timeline for user {}: {}", twitterUsername, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
