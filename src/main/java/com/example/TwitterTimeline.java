package com.example;

import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.Get2UsersMeResponse;
import com.twitter.clientlib.model.Tweet;

import java.util.List;

public class TwitterTimeline {
    public static void main(String[] args) {
        // Replace these with your actual Twitter API credentials
        String consumerKey = System.getenv("TWITTER_CONSUMER_KEY");
        String consumerSecret = System.getenv("TWITTER_CONSUMER_SECRET");
        String accessToken = System.getenv("TWITTER_ACCESS_TOKEN");
        String accessTokenSecret = System.getenv("TWITTER_ACCESS_TOKEN_SECRET");

        // Set up OAuth 1.0a credentials
        TwitterCredentialsOAuth2 credentials = new TwitterCredentialsOAuth2(
                consumerKey, consumerSecret, accessToken, accessTokenSecret
        );

        // Initialize the Twitter API client
        TwitterApi apiInstance = new TwitterApi(credentials);

        try {
            // Get the authenticated user's ID
            Get2UsersMeResponse meResponse = apiInstance.users().findMyUser().execute();
            if (meResponse.getData() == null) {
                System.out.println("Failed to retrieve user data.");
                return;
            }
            String userId = meResponse.getData().getId();

            // Fetch the user's timeline (up to 10 tweets)
            List<Tweet> tweets = apiInstance.tweets()
                    .usersIdTweets(userId)
                    .execute()
                    .getData();

            if (tweets == null || tweets.isEmpty()) {
                System.out.println("No tweets found in your timeline.");
                return;
            }

            // Display the tweets
            System.out.println("Your Twitter Timeline:");
            for (Tweet tweet : tweets) {
                System.out.println("- " + tweet.getText());
            }
        } catch (ApiException e) {
            System.err.println("Error calling Twitter API: " + e.getMessage());
            e.printStackTrace();
        }
    }
}