package com.example.twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.user.User;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.signature.TwitterCredentials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

            AdditionalParameters params = AdditionalParameters.builder()
                    .maxResults(Math.min(maxResults, 100)) // Adjust max results as needed (API limits apply)
                    .build();
            TweetList tweetList = twitterClient.getUserTimeline(userId, params);

            if (tweetList == null || tweetList.getData() == null) {
                System.out.println("No tweets found or error fetching timeline for user ID: " + userId);
                return Collections.emptyList();
            }

            List<TweetData> tweetDataList = new ArrayList<>();
            for (Tweet tweet : tweetList.getData()) {
                String url = "https://x.com/" + twitterUsername + "/status/" + tweet.getId();
                tweetDataList.add(new TweetData(tweet.getId(), tweet.getText(), url));
            }
            return tweetDataList;

        } catch (Exception e) {
            System.err.println("Error fetching Twitter timeline: " + e.getMessage());
            e.printStackTrace(); // Consider more robust logging
            return Collections.emptyList();
        }
    }
}