package com.example.twitch;

/**
 * Holds relevant information fetched from the Twitch API.
 *
 * @param profileImageUrl URL of the user's profile picture.
 * @param channelUrl      URL to the user's Twitch channel.
 */
public record TwitchUserInfo(String profileImageUrl, String channelUrl) {
    // Record automatically generates constructor, getters, equals, hashCode, toString
}
