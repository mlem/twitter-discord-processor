package com.example;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.user.User;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.signature.TwitterCredentials;

import java.util.List;

public class TwitterTimeline {


    /**
     * Rate limited to 1 request every 15 minutes.
     * @param args
     */
    public static void main(String[] args) {
        // Retrieve credentials from environment variables
        String consumerKey = System.getenv("TWITTER_CONSUMER_KEY");
        String consumerSecret = System.getenv("TWITTER_CONSUMER_SECRET");
        String accessToken = System.getenv("TWITTER_ACCESS_TOKEN");
        String accessTokenSecret = System.getenv("TWITTER_ACCESS_TOKEN_SECRET");
        String bearerToken = System.getenv("TWITTER_BEARER_TOKEN");
        String myUsername = "mlem86_tv";

        // Validate that all environment variables are set
        if (consumerKey == null || consumerSecret == null || accessToken == null ||
                accessTokenSecret == null || myUsername == null) {
            System.err.println("Error: One or more environment variables are missing. " +
                    "Please set TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET, " +
                    "TWITTER_ACCESS_TOKEN, TWITTER_ACCESS_TOKEN_SECRET, and TWITTER_USERNAME.");
            return;
        }

        // Build TwitterCredentials for OAuth 1.0a authentication
        TwitterCredentials credentials = TwitterCredentials.builder()
                .bearerToken(bearerToken)
                .build();

        // Initialize TwitterClient
        TwitterClient twitterClient = new TwitterClient(credentials);

        try {
            // Get your user ID using your username
            User me = twitterClient.getUserFromUserName(myUsername);
            if (me == null) {
                System.out.println("Failed to retrieve user data for username: " + myUsername);
                return;
            }
            String userId = me.getId();

            // Fetch your timeline (latest 10 tweets)
            AdditionalParameters params = AdditionalParameters.builder()
                    .maxResults(10)
                    .build();
            TweetList tweets = twitterClient.getUserTimeline(userId, params);

            // Check if tweets were retrieved successfully
            if (tweets == null || tweets.getData() == null || tweets.getData().isEmpty()) {
                System.out.println("No tweets found in your timeline.");
                return;
            }

            // Display the tweets
            System.out.println("Your Twitter Timeline:");
            for (Tweet tweet : tweets.getData()) {
                System.out.println("- " + tweet.getText());

                String id = tweet.getId();
                String url = "https://x.com/" + myUsername + "/status/" + id;
                System.out.println("- " + url);
            }
        } catch (Exception e) {
            System.err.println("Error fetching timeline: " + e.getMessage());
            e.printStackTrace();
        }
    }
}