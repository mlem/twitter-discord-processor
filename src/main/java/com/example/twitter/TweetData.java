package com.example.twitter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Using a standard class for compatibility and clarity with potentially null lists
public class TweetData {
    private final String id;
    private final String text;
    private final String url;
    private final List<String> imageUrls; // Added field for image URLs

    public TweetData(String id, String text, String url, List<String> imageUrls) {
        this.id = Objects.requireNonNull(id, "Tweet ID cannot be null");
        this.text = Objects.requireNonNull(text, "Tweet text cannot be null");
        this.url = Objects.requireNonNull(url, "Tweet URL cannot be null");
        // Ensure imageUrls is never null, use an empty list instead
        this.imageUrls = (imageUrls != null) ? Collections.unmodifiableList(imageUrls) : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    @Override
    public String toString() {
        return "TweetData{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", url='" + url + '\'' +
                ", imageUrls=" + imageUrls +
                '}';
    }

    // Optional: equals and hashCode if needed
}