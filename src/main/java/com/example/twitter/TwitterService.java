package com.example.twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.tweet.entities.MediaEntity;
import io.github.redouane59.twitter.dto.user.User;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.signature.TwitterCredentials;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TwitterService {

    private final TwitterClient twitterClient;
    private final String twitterUsername;

    public TwitterService(String bearerToken, String username) {
        if (bearerToken == null || username == null) {
            throw new IllegalArgumentException("Twitter Bearer Token and Username must be provided.");
        }
        this.twitterUsername = username;
        TwitterCredentials credentials = TwitterCredentials.builder()
                .bearerToken(bearerToken)
                .build();
        this.twitterClient = new TwitterClient(credentials);
    }

    public List<TweetData> fetchTimelineTweets(int maxResults) {
        try {
            User me = twitterClient.getUserFromUserName(twitterUsername);
            if (me == null) {
                System.err.println("Failed to retrieve user data for username: " + twitterUsername);
                return Collections.emptyList();
            }
            String userId = me.getId();

            // Request expansions and media fields
            AdditionalParameters params = AdditionalParameters.builder()
                    .maxResults(Math.min(maxResults, 100)) // API V2 limit is 100 for user timeline
                    // Add tweet fields if needed, e.g., tweetFields(List.of("created_at", "public_metrics"))
                    .build();

            TweetList tweetList = twitterClient.getUserTimeline(userId, params);

            if (tweetList == null || tweetList.getData() == null) {
                System.out.println("No tweets found or error fetching timeline for user ID: " + userId);
                return Collections.emptyList();
            }

            List<TweetData> tweetDataList = new ArrayList<>();
            List<TweetV2.MediaEntityV2> includedMedia = (tweetList.getIncludes() != null && tweetList.getIncludes().getMedia() != null)
                    ? tweetList.getIncludes().getMedia() : Collections.emptyList();

            for (Tweet tweet : tweetList.getData()) {
                String tweetUrl = "https://x.com/" + twitterUsername + "/status/" + tweet.getId();
                List<String> imageUrls = new ArrayList<>();

                // Check if the tweet has media attachments
                if (tweet.getAttachments() != null && tweet.getAttachments().getMediaKeys() != null) {
                    List<String> mediaKeys = Arrays.stream(tweet.getAttachments().getMediaKeys()).toList();
                    imageUrls = includedMedia.stream()
                            .filter(media -> mediaKeys.contains(media.getId()) && "photo".equals(media.getType())) // Filter for photos matching keys
                            .map(MediaEntity::getUrl) // Get the URL of the photo
                            .filter(java.util.Objects::nonNull) // Ensure URL is not null
                            .collect(Collectors.toList());
                }

                tweetDataList.add(new TweetData(tweet.getId(), tweet.getText(), tweetUrl, imageUrls));
            }
            return tweetDataList;

        } catch (Exception e) {
            System.err.println("Error fetching Twitter timeline: " + e.getMessage());
            e.printStackTrace(); // Consider more robust logging
            return Collections.emptyList();
        }
    }
}