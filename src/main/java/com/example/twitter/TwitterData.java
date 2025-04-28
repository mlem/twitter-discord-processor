package com.example.twitter;

// Using record for brevity (requires Java 14+)
// If using Java 8, create a standard class with fields, constructor, getters.
public record TweetData(String id, String text, String url) {}