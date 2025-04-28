package com.example.discord;

import com.example.file.TweetWriter;
import com.example.twitch.TwitchUserInfo;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DiscordNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotifier.class);
    private final JDA jda;
    private final String channelId;
    private static final int MAX_STANDARD_MESSAGE_LENGTH = 2000; // Discord standard message limit

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
     * Consumes a tweet file and sends a formatted embed (and potentially extra text messages) to Discord.
     *
     * @param tweetFile The file containing tweet data.
     * @param twitchInfo Optional containing Twitch user info (profile image, channel URL).
     * @return True if the message(s) were successfully queued, false otherwise.
     */
    public boolean consume(File tweetFile, Optional<TwitchUserInfo> twitchInfo) {
        logger.info("Consuming tweet file: {} with Twitch info present: {}", tweetFile.getName(), twitchInfo.isPresent());
        try {
            List<String> lines = FileUtils.readLines(tweetFile, StandardCharsets.UTF_8);

            // --- Parse Data from File (remains the same) ---
            String tweetText = "";
            String tweetUrl = "";
            List<String> imageUrls = Collections.emptyList();
            String authorName = null;
            String authorProfileUrl = null;
            String authorImageUrl = null;

            for (String line : lines) {
                if (line.startsWith("Text: ")) tweetText = line.substring("Text: ".length()).replaceAll("###n###", "\n");
                else if (line.startsWith("URL: ")) tweetUrl = line.substring("URL: ".length());
                else if (line.startsWith("ImageURLs: ")) {
                    String urlsPart = line.substring("ImageURLs: ".length());
                    if (!urlsPart.trim().isEmpty()) {
                        imageUrls = Arrays.asList(urlsPart.split(TweetWriter.IMAGE_URL_SEPARATOR));
                    }
                }
                else if (line.startsWith("AuthorName: ")) authorName = line.substring("AuthorName: ".length());
                else if (line.startsWith("AuthorProfileURL: ")) authorProfileUrl = line.substring("AuthorProfileURL: ".length());
                else if (line.startsWith("AuthorImageURL: ")) authorImageUrl = line.substring("AuthorImageURL: ".length());
            }
            logger.debug("Parsed from {}: Author='{}', Text='{}...', URL='{}', ImageCount={}",
                    tweetFile.getName(), authorName, tweetText.substring(0, Math.min(tweetText.length(), 30)), tweetUrl, imageUrls.size());

            if (tweetText.isEmpty() || tweetUrl.isEmpty()) {
                logger.error("Could not parse required fields (Text, URL) from file: {}", tweetFile.getName());
                return false;
            }
            // --- End Parsing ---


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

            // Set Author (remains the same)
            if (authorName != null && !authorName.isEmpty()) {
                embedBuilder.setAuthor(
                        authorName,
                        (authorProfileUrl != null && !authorProfileUrl.isEmpty()) ? authorProfileUrl : null,
                        (authorImageUrl != null && !authorImageUrl.isEmpty()) ? authorImageUrl : null
                );
            } else {
                logger.warn("Author name missing for tweet {}, cannot set embed author.", tweetUrl);
            }

            // --- Handle Tweet Text based on Length ---
            boolean textIsLong = tweetText.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH;
            String embedDescription;
            String extraTextMessage = null; // Will hold text if it's too long for embed

            if (textIsLong) {
                logger.warn("Tweet text exceeds Discord embed description limit ({} chars). Sending full text separately.", MessageEmbed.DESCRIPTION_MAX_LENGTH);
                // Option 1: Truncate description
                // embedDescription = tweetText.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 3) + "...";
                // Option 2: Placeholder description
                embedDescription = "(Full tweet text sent in separate message below)";
                extraTextMessage = tweetText; // Store the full text to send later
            } else {
                embedDescription = tweetText; // Fits entirely in description
            }
            embedBuilder.setDescription(embedDescription);
            // --- End Handle Tweet Text ---


            // Set Thumbnail using Twitch logo (remains the same)
            twitchInfo.ifPresent(info -> {
                if (info.profileImageUrl() != null && !info.profileImageUrl().isEmpty()) {
                    embedBuilder.setThumbnail(info.profileImageUrl());
                    logger.debug("Set embed thumbnail to Twitch logo: {}", info.profileImageUrl());
                } else {
                    logger.warn("Twitch user info present, but profile image URL is missing.");
                }
            });

            // Set Image using the first image from the tweet (remains the same)
            if (!imageUrls.isEmpty() && imageUrls.get(0) != null && !imageUrls.get(0).isEmpty()) {
                embedBuilder.setImage(imageUrls.get(0));
                logger.debug("Set embed image to first tweet image: {}", imageUrls.get(0));
                // Note: You could potentially add more image URLs as fields if the text is short
                // but Discord has limits on the number and size of fields too.
            }

            // Set Timestamp and Footer (remains the same)
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter("via https://github.com/mlem/twitter-discord-processor", null);

            // --- Send the Message(s) ---
            MessageEmbed embed = embedBuilder.build();

            logger.debug("Sending embed to Discord channel {}: Title='{}'", channelId, embed.getTitle());
            // Send the embed first
            channel.sendMessageEmbeds(embed).queue(
                    success -> logger.info("Successfully sent embed for {} to Discord channel {}", tweetFile.getName(), channelId),
                    error -> handleDiscordSendError(error, tweetFile.getName(), channelId, "embed") // Use helper for error logging
            );

            // If the text was too long, send it separately
            if (extraTextMessage != null) {
                logger.info("Sending separate message(s) for long tweet text from file {}", tweetFile.getName());
                // Split the message if it exceeds the standard 2000 char limit
                List<String> messageChunks = splitMessage(extraTextMessage, MAX_STANDARD_MESSAGE_LENGTH);
                for (String chunk : messageChunks) {
                    channel.sendMessage(chunk).queue(
                            success -> logger.debug("Successfully sent text chunk for {} to Discord channel {}", tweetFile.getName(), channelId),
                            error -> handleDiscordSendError(error, tweetFile.getName(), channelId, "text chunk") // Use helper
                    );
                }
            }

            return true; // Indicate successful attempt to process and queue

        } catch (IOException e) {
            logger.error("Failed to read tweet file {}: {}", tweetFile.getName(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("An unexpected error occurred during Discord notification for file {}: {}", tweetFile.getName(), e.getMessage(), e);
            return false;
        }
    }

    // Helper method to log Discord sending errors consistently
    private void handleDiscordSendError(Throwable error, String fileName, String channelId, String messageType) {
        if (error instanceof InsufficientPermissionException) {
            logger.error("Discord bot lacks permission (e.g., SEND_MESSAGES, EMBED_LINKS) to send {} in channel {}: {}", messageType, channelId, error.getMessage());
        } else {
            logger.error("Failed to send {} for file {} to Discord channel {}: {}", messageType, fileName, channelId, error.getMessage(), error);
        }
    }

    // Helper method to split long messages
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
        // Shutdown remains the same...
        if (jda != null) {
            logger.info("Shutting down Discord Bot connection...");
            jda.shutdown();
            logger.info("Discord Bot Shut Down.");
        }
    }
}
