package com.example.discord;

// Removed TweetWriter import
// Removed TwitchUserInfo import
import com.example.twitter.TweetData; // Import TweetData
import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Import JavaTimeModule
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
// Removed FileUtils import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
// Removed StandardCharsets import
import java.time.Instant;
import java.time.ZoneOffset; // Keep ZoneOffset
// Removed LocalDateTime, DateTimeFormatter, DateTimeParseException
import java.util.Collections;
import java.util.List;
// Removed Arrays, HashMap, Map
// Removed Optional

public class DiscordNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotifier.class);
    private final JDA jda;
    private final String channelId;
    private static final int MAX_STANDARD_MESSAGE_LENGTH = 2000;
    private final ObjectMapper objectMapper; // Jackson ObjectMapper instance

    public DiscordNotifier(String botToken, String channelId) throws LoginException, InterruptedException {
        if (botToken == null || channelId == null) {
            logger.error("Discord Bot Token and Channel ID must be provided.");
            throw new IllegalArgumentException("Discord Bot Token and Channel ID must be provided.");
        }
        this.channelId = channelId;
        logger.info("Initializing Discord Notifier for channel ID: {}", channelId);

        // Initialize ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Needed for deserializing LocalDateTime

        try {
            this.jda = JDABuilder.createDefault(botToken).build();
            this.jda.awaitReady();
            logger.info("Discord Bot Connected and Ready!");
        } catch (InterruptedException e) {
            logger.error("Discord connection interrupted while waiting for ready state.", e);
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during JDA initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize JDA", e);
        }
    }

    /**
     * Consumes a tweet JSON file containing all context and sends an embed to Discord.
     *
     * @param tweetJsonFile The JSON file containing tweet, author, and twitch data.
     * @return True if the message(s) were successfully queued, false otherwise.
     */
    // Removed twitchInfo parameter
    public boolean consume(File tweetJsonFile) {
        logger.info("Consuming tweet context from JSON file: {}", tweetJsonFile.getName());
        try {
            // --- Deserialize JSON file to TweetData object ---
            TweetData tweetData = objectMapper.readValue(tweetJsonFile, TweetData.class);
            logger.debug("Successfully deserialized JSON data for tweet ID: {}", tweetData.getId());
            // --- End Deserialization ---

            // --- Extract data from the TweetData object ---
            String tweetText = tweetData.getText();
            String tweetUrl = tweetData.getUrl();
            String authorName = tweetData.getAuthorName();
            String authorProfileUrl = tweetData.getAuthorProfileUrl();
            String authorImageUrl = tweetData.getAuthorProfileImageUrl();
            List<String> imageUrls = tweetData.getImageUrls();
            String twitchImageUrl = tweetData.getTwitchProfileImageUrl(); // Get Twitch logo URL

            // Convert LocalDateTime to Instant for timestamp
            Instant timestamp = Instant.now(); // Default to now
            if (tweetData.getCreatedAt() != null) {
                try {
                    // Assuming UTC for Twitter timestamps
                    timestamp = tweetData.getCreatedAt().toInstant(ZoneOffset.UTC);
                    logger.debug("Using timestamp from TweetData: {}", timestamp);
                } catch (Exception e) {
                    logger.error("Error converting LocalDateTime to Instant for tweet {}. Using current time. Error: {}",
                            tweetData.getId(), e.getMessage());
                }
            } else {
                logger.warn("CreatedAt timestamp missing in TweetData for tweet {}. Using current time for embed.", tweetData.getId());
            }
            // --- End Data Extraction ---


            // --- Basic Validation ---
            if (tweetText == null || tweetText.isEmpty() || tweetUrl == null || tweetUrl.isEmpty() || authorName == null || authorName.isEmpty()) {
                logger.error("Required fields (Text, URL, AuthorName) missing or empty in deserialized TweetData from file: {}", tweetJsonFile.getName());
                return false;
            }
            // --- End Validation ---


            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Discord channel with ID {} not found or bot lacks access.", channelId);
                return false;
            }
            if (!channel.canTalk()) {
                logger.error("Discord bot lacks permission to send messages in channel {}.", channelId);
                return false;
            }

            // --- Build the Embed (using fields from tweetData object) ---
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("X Relay", tweetUrl);
            embedBuilder.setColor(Color.CYAN);

            embedBuilder.setAuthor(
                    authorName,
                    (authorProfileUrl != null && !authorProfileUrl.isEmpty()) ? authorProfileUrl : null,
                    (authorImageUrl != null && !authorImageUrl.isEmpty()) ? authorImageUrl : null
            );

            boolean textIsLong = tweetText.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH;
            String embedDescription;
            String extraTextMessage = null;
            if (textIsLong) {
                embedDescription = "(Full tweet text sent in separate message below)";
                extraTextMessage = tweetText;
            } else {
                embedDescription = tweetText;
            }
            embedBuilder.setDescription(embedDescription);

            if (twitchImageUrl != null && !twitchImageUrl.isEmpty()) {
                embedBuilder.setThumbnail(twitchImageUrl);
                logger.debug("Set embed thumbnail using Twitch logo URL from TweetData: {}", twitchImageUrl);
            } else {
                logger.debug("No Twitch image URL found in TweetData, thumbnail not set.");
            }

            if (!imageUrls.isEmpty() && imageUrls.get(0) != null && !imageUrls.get(0).isEmpty()) {
                embedBuilder.setImage(imageUrls.get(0));
            }

            embedBuilder.setTimestamp(timestamp); // Use parsed/converted timestamp
            embedBuilder.setFooter("via https://github.com/mlem/twitter-discord-processor", null);

            // --- Send the Message(s) ---
            MessageEmbed embed = embedBuilder.build();

            logger.debug("Sending embed to Discord channel {}: Title='{}'", channelId, embed.getTitle());
            channel.sendMessageEmbeds(embed).queue(
                    success -> logger.info("Successfully sent embed for {} to Discord channel {}", tweetJsonFile.getName(), channelId),
                    error -> handleDiscordSendError(error, tweetJsonFile.getName(), channelId, "embed")
            );

            if (extraTextMessage != null) {
                logger.info("Sending separate message(s) for long tweet text from file {}", tweetJsonFile.getName());
                List<String> messageChunks = splitMessage(extraTextMessage, MAX_STANDARD_MESSAGE_LENGTH);
                for (String chunk : messageChunks) {
                    channel.sendMessage(chunk).queue(
                            success -> logger.debug("Successfully sent text chunk for {} to Discord channel {}", tweetJsonFile.getName(), channelId),
                            error -> handleDiscordSendError(error, tweetJsonFile.getName(), channelId, "text chunk")
                    );
                }
            }

            return true;

        } catch (IOException e) {
            logger.error("Failed to read or parse JSON tweet file {}: {}", tweetJsonFile.getName(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("An unexpected error occurred during Discord notification for file {}: {}", tweetJsonFile.getName(), e.getMessage(), e);
            return false;
        }
    }

    // Helper methods (handleDiscordSendError, splitMessage, shutdown) remain the same...
    private void handleDiscordSendError(Throwable error, String fileName, String channelId, String messageType) {
        if (error instanceof InsufficientPermissionException) {
            logger.error("Discord bot lacks permission (e.g., SEND_MESSAGES, EMBED_LINKS) to send {} in channel {}: {}", messageType, channelId, error.getMessage());
        } else {
            logger.error("Failed to send {} for file {} to Discord channel {}: {}", messageType, fileName, channelId, error.getMessage(), error);
        }
    }

    private List<String> splitMessage(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return Collections.singletonList(text);
        }
        List<String> parts = new java.util.ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += maxLength) {
            parts.add(text.substring(i, Math.min(length, i + maxLength)));
        }
        return parts;
    }

    public void shutdown() {
        if (jda != null) {
            logger.info("Shutting down Discord Bot connection...");
            jda.shutdown();
            logger.info("Discord Bot Shut Down.");
        }
    }
}
