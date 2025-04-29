package com.example.discord;

import com.example.twitter.TweetData; // Need TweetData for deserialization
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message; // Import Message
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
// Removed unused imports like Arrays, HashMap, Map, Optional

public class DiscordNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotifier.class);
    private final JDA jda;
    private final String channelId;
    private static final int MAX_STANDARD_MESSAGE_LENGTH = 2000;
    private static final int HISTORY_CHECK_LIMIT = 10; // How many messages back to check
    private final ObjectMapper objectMapper;

    public DiscordNotifier(String botToken, String channelId) throws LoginException, InterruptedException {
        if (botToken == null || channelId == null) {
            logger.error("Discord Bot Token and Channel ID must be provided.");
            throw new IllegalArgumentException("Discord Bot Token and Channel ID must be provided.");
        }
        this.channelId = channelId;
        logger.info("Initializing Discord Notifier for channel ID: {}", channelId);

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

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
     * Consumes a tweet JSON file, checks recent channel history for duplicates,
     * and sends an embed to Discord if the tweet hasn't been posted recently.
     *
     * @param tweetJsonFile The JSON file containing tweet context.
     * @return True if processing is considered successful (either posted or skipped duplicate), false on error.
     */
    public boolean consume(File tweetJsonFile) {
        logger.info("Consuming tweet context from JSON file: {}", tweetJsonFile.getName());
        TweetData tweetData = null; // Declare outside try block

        try {
            // --- Deserialize JSON file to TweetData object ---
            tweetData = objectMapper.readValue(tweetJsonFile, TweetData.class);
            logger.debug("Successfully deserialized JSON data for tweet ID: {}", tweetData.getId());
            // --- End Deserialization ---

            // --- Extract key data for checking and embedding ---
            String tweetUrl = tweetData.getUrl(); // URL is crucial for duplicate check

            if (tweetUrl == null || tweetUrl.isEmpty()) {
                logger.error("Tweet URL missing in deserialized TweetData from file: {}. Cannot process.", tweetJsonFile.getName());
                return false; // Cannot check for duplicates or post meaningfully without URL
            }
            // --- End Data Extraction ---

            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Discord channel with ID {} not found or bot lacks access.", channelId);
                return false;
            }
            if (!channel.canTalk()) { // Check send permission early
                logger.error("Discord bot lacks permission to send messages in channel {}.", channelId);
                return false;
            }

            // --- Duplicate Check ---
            logger.debug("Checking recent message history in channel {} for tweet URL: {}", channelId, tweetUrl);
            try {
                // Bot needs READ_MESSAGE_HISTORY permission for this
                List<Message> history = channel.getHistory().retrievePast(HISTORY_CHECK_LIMIT).complete(); // Blocking call

                for (Message msg : history) {
                    // Check embeds first (more reliable)
                    if (!msg.getEmbeds().isEmpty()) {
                        for (MessageEmbed embed : msg.getEmbeds()) {
                            if (tweetUrl.equals(embed.getUrl())) { // Check if embed URL matches tweet URL
                                logger.info("Duplicate found: Tweet URL {} already present in recent message history (Embed URL match). Skipping post.", tweetUrl);
                                return true; // Treat skipping duplicate as success for processing flow
                            }
                        }
                    }
                    // Optional: Check raw content as a fallback (less reliable)
                    // if (msg.getContentRaw().contains(tweetUrl)) {
                    //     logger.info("Duplicate found: Tweet URL {} already present in recent message history (Raw content match). Skipping post.", tweetUrl);
                    //     return true;
                    // }
                }
                logger.debug("No duplicate found for tweet URL {} in the last {} messages.", tweetUrl, HISTORY_CHECK_LIMIT);

            } catch (InsufficientPermissionException permEx) {
                logger.error("Bot lacks 'Read Message History' permission in channel {} to check for duplicates. Proceeding without check.", channelId);
                // Continue without check if permission is missing
            } catch (Exception historyEx) {
                logger.error("Error retrieving message history for channel {}: {}. Proceeding without duplicate check.", channelId, historyEx.getMessage(), historyEx);
                // Continue without check on other errors
            }
            // --- End Duplicate Check ---


            // --- Proceed with building and sending if no duplicate found ---
            // Extract remaining data needed for embed
            String tweetText = tweetData.getText();
            String authorName = tweetData.getAuthorName();
            String authorProfileUrl = tweetData.getAuthorProfileUrl();
            String authorImageUrl = tweetData.getAuthorProfileImageUrl();
            List<String> imageUrls = tweetData.getImageUrls();
            String twitchImageUrl = tweetData.getTwitchProfileImageUrl();
            LocalDateTime createdAt = tweetData.getCreatedAt();

            // Basic Validation
            if (tweetText == null || tweetText.isEmpty() || authorName == null || authorName.isEmpty()) {
                logger.error("Required fields (Text, AuthorName) missing or empty in deserialized TweetData from file: {}", tweetJsonFile.getName());
                return false; // Cannot build embed properly
            }

            // Parse Timestamp
            Instant timestamp = Instant.now();
            if (createdAt != null) {
                try {
                    timestamp = createdAt.toInstant(ZoneOffset.UTC);
                } catch (Exception e) {
                    logger.error("Error converting LocalDateTime to Instant for tweet {}. Using current time. Error: {}",
                            tweetData.getId(), e.getMessage());
                }
            } else {
                logger.warn("CreatedAt timestamp missing in TweetData for tweet {}. Using current time for embed.", tweetData.getId());
            }

            // Build Embed
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
            }

            if (!imageUrls.isEmpty() && imageUrls.get(0) != null && !imageUrls.get(0).isEmpty()) {
                embedBuilder.setImage(imageUrls.get(0));
            }

            embedBuilder.setTimestamp(timestamp);
            embedBuilder.setFooter("via https://github.com/mlem/twitter-discord-processor", null);

            // Send Message(s)
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

            return true; // Indicate successful posting attempt

        } catch (IOException e) {
            logger.error("Failed to read or parse JSON tweet file {}: {}", tweetJsonFile.getName(), e.getMessage(), e);
            return false; // Indicate failure reading file
        } catch (Exception e) {
            logger.error("An unexpected error occurred during Discord notification for file {}: {}",
                    (tweetData != null ? tweetJsonFile.getName() : "UNKNOWN"), e.getMessage(), e);
            return false; // Indicate general failure
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
