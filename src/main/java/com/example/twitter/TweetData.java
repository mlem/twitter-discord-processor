package com.example.twitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds processed information about a single tweet, its author,
 * and associated Twitch context.
 */
public class TweetData {
    // Twitter Tweet Info
    private final String id;
    private final String text;
    private final String url;
    private final List<String> imageUrls;
    private final LocalDateTime createdAt;

    // Twitter Author Info
    private final String authorName;
    private final String authorProfileUrl;
    private final String authorProfileImageUrl;

    // Twitch Context Info
    private final String twitchUsername; // The configured Twitch username
    private final String twitchProfileImageUrl; // Fetched Twitch profile image URL
    // twitchChannelUrl is not strictly needed downstream if we only use the logo,
    // but keeping it for potential future use or completeness.
    private final String twitchChannelUrl;

    public TweetData(String id, String text, String url, List<String> imageUrls,
                     LocalDateTime createdAt,
                     String authorName, String authorProfileUrl, String authorProfileImageUrl,
                     String twitchUsername, String twitchProfileImageUrl, String twitchChannelUrl) { // Added Twitch params
        // Twitter Tweet
        this.id = Objects.requireNonNull(id, "Tweet ID cannot be null");
        this.text = Objects.requireNonNull(text, "Tweet text cannot be null");
        this.url = Objects.requireNonNull(url, "Tweet URL cannot be null");
        this.imageUrls = (imageUrls != null) ? Collections.unmodifiableList(imageUrls) : Collections.emptyList();
        this.createdAt = createdAt;

        // Twitter Author
        this.authorName = authorName;
        this.authorProfileUrl = authorProfileUrl;
        this.authorProfileImageUrl = authorProfileImageUrl;

        // Twitch Context
        this.twitchUsername = twitchUsername; // Store configured Twitch username
        this.twitchProfileImageUrl = twitchProfileImageUrl; // Store fetched Twitch logo URL (can be null)
        this.twitchChannelUrl = twitchChannelUrl; // Store fetched Twitch channel URL (can be null)
    }

    // --- Getters ---
    // Twitter Tweet
    public String getId() { return id; }
    public String getText() { return text; }
    public String getUrl() { return url; }
    public List<String> getImageUrls() { return imageUrls; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    // Twitter Author
    public String getAuthorName() { return authorName; }
    public String getAuthorProfileUrl() { return authorProfileUrl; }
    public String getAuthorProfileImageUrl() { return authorProfileImageUrl; }
    // Twitch Context
    public String getTwitchUsername() { return twitchUsername; }
    public String getTwitchProfileImageUrl() { return twitchProfileImageUrl; }
    public String getTwitchChannelUrl() { return twitchChannelUrl; }


    @Override
    public String toString() {
        return "TweetData{" +
                "id='" + id + '\'' +
                ", text='" + text.substring(0, Math.min(text.length(), 20)) + "...'" +
                ", url='" + url + '\'' +
                ", imageUrlsCount=" + imageUrls.size() +
                ", createdAt=" + createdAt +
                ", authorName='" + authorName + '\'' +
                ", authorProfileUrl='" + authorProfileUrl + '\'' +
                ", authorProfileImageUrl='" + (authorProfileImageUrl != null ? "present" : "null") + '\'' +
                ", twitchUsername='" + twitchUsername + '\'' + // Added Twitch info
                ", twitchProfileImageUrl='" + (twitchProfileImageUrl != null ? "present" : "null") + '\'' +
                ", twitchChannelUrl='" + twitchChannelUrl + '\'' +
                '}';
    }
}
