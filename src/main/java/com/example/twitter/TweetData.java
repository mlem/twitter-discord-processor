package com.example.twitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds processed information about a single tweet, its author,
 * associated Twitch context, and additional raw tweet fields.
 */
public class TweetData {
    // --- Core Tweet Info ---
    private final String id;
    private final String text;
    private final String url; // Derived URL
    private final List<String> imageUrls; // Derived image URLs
    private final LocalDateTime createdAt;

    // --- Twitter Author Info ---
    private final String authorName;
    private final String authorProfileUrl; // Derived URL
    private final String authorProfileImageUrl;

    // --- Twitch Context Info ---
    private final String twitchUsername;
    private final String twitchProfileImageUrl;
    private final String twitchChannelUrl;

    // --- Additional Raw Tweet Fields ---
    private final String tweetAuthorId; // From Tweet object
    private final String tweetConversationId; // From Tweet object
    private final String tweetLang; // From Tweet object
    private final String tweetSource; // From Tweet object
    private final String tweetReplySettings; // From Tweet object
    private final String tweetInReplyToUserId; // From Tweet object
    // Store complex objects as strings
    private final String tweetEntitiesStr;
    private final String tweetAttachmentsStr;
    private final String tweetGeoStr;


    public TweetData() {

        id = "";
        text = "";
        url = "";
        imageUrls = List.of();
        createdAt = null;
        authorName = "";
        authorProfileUrl = "";
        authorProfileImageUrl = "";
        twitchUsername = "";
        twitchProfileImageUrl = "";
        twitchChannelUrl = "";
        tweetAuthorId = "";
        tweetConversationId = "";
        tweetLang = "";
        tweetSource = "";
        tweetReplySettings = "";
        tweetInReplyToUserId = "";
        tweetEntitiesStr = "";
        tweetAttachmentsStr = "";
        tweetGeoStr = "";
    }
    // Updated Constructor
    public TweetData(
            // Core Tweet
            String id, String text, String url, List<String> imageUrls, LocalDateTime createdAt,
            // Author
            String authorName, String authorProfileUrl, String authorProfileImageUrl,
            // Twitch
            String twitchUsername, String twitchProfileImageUrl, String twitchChannelUrl,
            // Additional Raw Tweet Fields
            String tweetAuthorId, String tweetConversationId, String tweetLang, String tweetSource,
            String tweetReplySettings, String tweetInReplyToUserId, String tweetEntitiesStr, String tweetAttachmentsStr,
            String tweetGeoStr
    ) {
        // Core Tweet
        this.id = Objects.requireNonNull(id, "Tweet ID cannot be null");
        this.text = Objects.requireNonNull(text, "Tweet text cannot be null");
        this.url = Objects.requireNonNull(url, "Tweet URL cannot be null");
        this.imageUrls = (imageUrls != null) ? Collections.unmodifiableList(imageUrls) : Collections.emptyList();
        this.createdAt = createdAt;

        // Author
        this.authorName = authorName;
        this.authorProfileUrl = authorProfileUrl;
        this.authorProfileImageUrl = authorProfileImageUrl;

        // Twitch
        this.twitchUsername = twitchUsername;
        this.twitchProfileImageUrl = twitchProfileImageUrl;
        this.twitchChannelUrl = twitchChannelUrl;

        // Additional Raw Tweet
        this.tweetAuthorId = tweetAuthorId;
        this.tweetConversationId = tweetConversationId;
        this.tweetLang = tweetLang;
        this.tweetSource = tweetSource;
        this.tweetReplySettings = tweetReplySettings;
        this.tweetInReplyToUserId = tweetInReplyToUserId;
        this.tweetEntitiesStr = tweetEntitiesStr;
        this.tweetAttachmentsStr = tweetAttachmentsStr;
        this.tweetGeoStr = tweetGeoStr;
    }

    // --- Getters ---
    // Core Tweet
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
    // Additional Raw Tweet
    public String getTweetAuthorId() { return tweetAuthorId; }
    public String getTweetConversationId() { return tweetConversationId; }
    public String getTweetLang() { return tweetLang; }
    public String getTweetSource() { return tweetSource; }
    public String getTweetReplySettings() { return tweetReplySettings; }
    public String getTweetInReplyToUserId() { return tweetInReplyToUserId; }
    public String getTweetEntitiesStr() { return tweetEntitiesStr; }
    public String getTweetAttachmentsStr() { return tweetAttachmentsStr; }
    public String getTweetGeoStr() { return tweetGeoStr; }


    @Override
    public String toString() {
        // Basic toString for logging brevity
        return "TweetData{" +
                "id='" + id + '\'' +
                ", text='" + text.substring(0, Math.min(text.length(), 20)) + "...'" +
                ", createdAt=" + createdAt +
                ", authorName='" + authorName + '\'' +
                ", twitchUsername='" + twitchUsername + '\'' +
                // Indicate presence of other fields if needed
                '}';
    }
}
