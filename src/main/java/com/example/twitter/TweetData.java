package com.example.twitter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds processed information about a single tweet and its author.
 */
public class TweetData {
    private final String id;
    private final String text;
    private final String url;
    private final List<String> imageUrls;
    // Added Twitter User Info
    private final String authorName;
    private final String authorProfileUrl;
    private final String authorProfileImageUrl;

    public TweetData(String id, String text, String url, List<String> imageUrls,
                     String authorName, String authorProfileUrl, String authorProfileImageUrl) {
        this.id = Objects.requireNonNull(id, "Tweet ID cannot be null");
        this.text = Objects.requireNonNull(text, "Tweet text cannot be null");
        this.url = Objects.requireNonNull(url, "Tweet URL cannot be null");
        this.imageUrls = (imageUrls != null) ? Collections.unmodifiableList(imageUrls) : Collections.emptyList();
        // Store author info
        this.authorName = authorName; // Can be null if not found
        this.authorProfileUrl = authorProfileUrl; // Can be null
        this.authorProfileImageUrl = authorProfileImageUrl; // Can be null
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getText() { return text; }
    public String getUrl() { return url; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getAuthorName() { return authorName; }
    public String getAuthorProfileUrl() { return authorProfileUrl; }
    public String getAuthorProfileImageUrl() { return authorProfileImageUrl; }

    @Override
    public String toString() {
        return "TweetData{" +
                "id='" + id + '\'' +
                ", text='" + text.substring(0, Math.min(text.length(), 20)) + "...'" + // Truncate for logging
                ", url='" + url + '\'' +
                ", imageUrlsCount=" + imageUrls.size() +
                ", authorName='" + authorName + '\'' +
                ", authorProfileUrl='" + authorProfileUrl + '\'' +
                ", authorProfileImageUrl='" + (authorProfileImageUrl != null ? "present" : "null") + '\'' +
                '}';
    }
}
