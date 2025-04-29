package com.example.discord;

import com.example.file.TweetWriter;
// import com.example.twitch.TwitchUserInfo; // No longer needed as parameter
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
// import java.util.Optional; // No longer needed

public class DiscordNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotifier.class);
    private final JDA jda;
    private final String channelId;
    private static final int MAX_STANDARD_MESSAGE_LENGTH = 2000;

    public DiscordNotifier(String botToken, String channelId) throws LoginException, InterruptedException {
        // Initialization remains the same...
        if (botToken == null || channelId == null) {
            logger.error("Discord Bot Token and Channel ID must be provided.");
            throw new IllegalArgumentException("Discord Bot Token and Channel ID must be provided.");
        }
        this.channelId = channelId;
        logger.info("Initializing Discord Notifier for channel ID: {}", channelId);
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
     * Consumes a tweet file containing all context and sends an embed to Discord.
     *
     * @param tweetFile The file containing tweet, author, and twitch data.
     * @return True if the message(s) were successfully queued, false otherwise.
     */
    // Removed twitchInfo parameter
    public boolean consume(File tweetFile) {
        logger.info("Consuming tweet context file: {}", tweetFile.getName());
        try {
            List<String> lines = FileUtils.readLines(tweetFile, StandardCharsets.UTF_8);

            // --- Parse Data from File ---
            String tweetText = "";
            String tweetUrl = "";
            List<String> imageUrls = Collections.emptyList();
            String createdAtString = null;
            String authorName = null;
            String authorProfileUrl = null;
            String authorImageUrl = null;
            String twitchUsername = null; // Variable for Twitch username
            String twitchImageUrl = null; // Variable for Twitch logo
            // String twitchChannelUrl = null; // Parse if needed later

            for (String line : lines) {
                if (line.startsWith("Text: ")) tweetText = line.substring("Text: ".length()).replaceAll("###n###", "\n");
                else if (line.startsWith("URL: ")) tweetUrl = line.substring("URL: ".length());
                else if (line.startsWith("ImageURLs: ")) {
                    String urlsPart = line.substring("ImageURLs: ".length());
                    if (!urlsPart.trim().isEmpty()) {
                        imageUrls = Arrays.asList(urlsPart.split(TweetWriter.IMAGE_URL_SEPARATOR));
                    }
                }
                else if (line.startsWith("CreatedAt: ")) createdAtString = line.substring("CreatedAt: ".length());
                else if (line.startsWith("AuthorName: ")) authorName = line.substring("AuthorName: ".length());
                else if (line.startsWith("AuthorProfileURL: ")) authorProfileUrl = line.substring("AuthorProfileURL: ".length());
                else if (line.startsWith("AuthorImageURL: ")) authorImageUrl = line.substring("AuthorImageURL: ".length());
                    // Parse Twitch info
                else if (line.startsWith("TwitchUsername: ")) twitchUsername = line.substring("TwitchUsername: ".length());
                else if (line.startsWith("TwitchImageURL: ")) twitchImageUrl = line.substring("TwitchImageURL: ".length());
                // else if (line.startsWith("TwitchChannelURL: ")) twitchChannelUrl = line.substring("TwitchChannelURL: ".length());
            }
            logger.debug("Parsed from {}: Author='{}', TwitchUser='{}', Text='{}...', URL='{}', ImageCount={}, CreatedAt='{}', TwitchLogo={}",
                    tweetFile.getName(), authorName, twitchUsername, tweetText.substring(0, Math.min(tweetText.length(), 30)),
                    tweetUrl, imageUrls.size(), createdAtString, (twitchImageUrl != null && !twitchImageUrl.isEmpty()));

            if (tweetText.isEmpty() || tweetUrl.isEmpty() || authorName == null || authorName.isEmpty()) {
                logger.error("Could not parse required fields (Text, URL, AuthorName) from file: {}", tweetFile.getName());
                return false;
            }
            // --- End Parsing ---

            // --- Parse Timestamp (remains the same) ---
            Instant timestamp = Instant.now();
            if (createdAtString != null && !createdAtString.isEmpty()) {
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(createdAtString, DateTimeFormatter.ISO_DATE_TIME);
                    timestamp = localDateTime.toInstant(ZoneOffset.UTC);
                } catch (DateTimeParseException e) {
                    logger.error("Failed to parse CreatedAt timestamp '{}' from file {}. Using current time. Error: {}",
                            createdAtString, tweetFile.getName(), e.getMessage());
                }
            } else {
                logger.warn("CreatedAt timestamp missing in file {}. Using current time for embed.", tweetFile.getName());
            }
            // --- End Parse Timestamp ---


            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Discord channel with ID {} not found or bot lacks access.", channelId);
                return false;
            }
            if (!channel.canTalk()) {
                logger.error("Discord bot lacks permission to send messages in channel {}.", channelId);
                return false;
            }

            // --- Build the Embed ---
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("X Relay", tweetUrl);
            embedBuilder.setColor(Color.CYAN);

            // Set Author (using parsed Twitter info)
            embedBuilder.setAuthor(
                    authorName,
                    (authorProfileUrl != null && !authorProfileUrl.isEmpty()) ? authorProfileUrl : null,
                    (authorImageUrl != null && !authorImageUrl.isEmpty()) ? authorImageUrl : null
            );

            // Handle Tweet Text based on Length (remains the same)
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

            // Set Thumbnail using parsed Twitch logo URL
            if (twitchImageUrl != null && !twitchImageUrl.isEmpty()) {
                embedBuilder.setThumbnail(twitchImageUrl);
                logger.debug("Set embed thumbnail using Twitch logo URL from file: {}", twitchImageUrl);
            } else {
                logger.debug("No Twitch image URL found in file {}, thumbnail not set.", tweetFile.getName());
            }

            // Set Image using the first image from the tweet (remains the same)
            if (!imageUrls.isEmpty() && imageUrls.get(0) != null && !imageUrls.get(0).isEmpty()) {
                embedBuilder.setImage(imageUrls.get(0));
            }

            // Set Timestamp using the parsed timestamp
            embedBuilder.setTimestamp(timestamp);

            // Set Footer
            embedBuilder.setFooter("via https://github.com/mlem/twitter-discord-processor", null);

            // --- Send the Message(s) ---
            MessageEmbed embed = embedBuilder.build();

            logger.debug("Sending embed to Discord channel {}: Title='{}'", channelId, embed.getTitle());
            channel.sendMessageEmbeds(embed).queue(
                    success -> logger.info("Successfully sent embed for {} to Discord channel {}", tweetFile.getName(), channelId),
                    error -> handleDiscordSendError(error, tweetFile.getName(), channelId, "embed")
            );

            if (extraTextMessage != null) {
                logger.info("Sending separate message(s) for long tweet text from file {}", tweetFile.getName());
                List<String> messageChunks = splitMessage(extraTextMessage, MAX_STANDARD_MESSAGE_LENGTH);
                for (String chunk : messageChunks) {
                    channel.sendMessage(chunk).queue(
                            success -> logger.debug("Successfully sent text chunk for {} to Discord channel {}", tweetFile.getName(), channelId),
                            error -> handleDiscordSendError(error, tweetFile.getName(), channelId, "text chunk")
                    );
                }
            }

            return true;

        } catch (IOException e) {
            logger.error("Failed to read tweet file {}: {}", tweetFile.getName(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("An unexpected error occurred during Discord notification for file {}: {}", tweetFile.getName(), e.getMessage(), e);
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
