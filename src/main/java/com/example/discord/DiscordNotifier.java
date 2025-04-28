package com.example.discord;

import com.example.file.TweetWriter; // Import to access separator
import com.example.twitch.TwitchUserInfo; // Import Twitch info holder
import net.dv8tion.jda.api.EmbedBuilder; // Import EmbedBuilder
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed; // Import MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
// Removed MessageCreateBuilder/Data as we now use embeds directly
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*; // Import Color for embeds
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant; // Import Instant for timestamp
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional; // Import Optional

public class DiscordNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotifier.class);
    private final JDA jda;
    private final String channelId;
    // private static final int MAX_DISCORD_MESSAGE_LENGTH = 2000; // Less relevant for embeds

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
     * Consumes a tweet file and sends a formatted embed to Discord.
     *
     * @param tweetFile The file containing tweet data.
     * @param twitchInfo Optional containing Twitch user info (profile image, channel URL).
     * @return True if the message was successfully queued, false otherwise.
     */
    public boolean consume(File tweetFile, Optional<TwitchUserInfo> twitchInfo) { // Added twitchInfo parameter
        logger.info("Consuming tweet file: {} with Twitch info present: {}", tweetFile.getName(), twitchInfo.isPresent());
        try {
            List<String> lines = FileUtils.readLines(tweetFile, StandardCharsets.UTF_8);

            // --- Parse Data from File ---
            String tweetText = "";
            String tweetUrl = "";
            List<String> imageUrls = Collections.emptyList();
            String authorName = null;
            String authorProfileUrl = null;
            String authorImageUrl = null;

            for (String line : lines) {
                if (line.startsWith("Text: ")) tweetText = line.substring("Text: ".length());
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

            // --- Build the Embed ---
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("X Relay", tweetUrl); // Title links to the tweet
            embedBuilder.setColor(Color.CYAN); // Or any color you like

            // Set Author using Twitter info (handle nulls)
            if (authorName != null && !authorName.isEmpty()) {
                // Use profile URL if available, otherwise null. Use image URL if available, otherwise null.
                embedBuilder.setAuthor(
                        authorName,
                        (authorProfileUrl != null && !authorProfileUrl.isEmpty()) ? authorProfileUrl : null,
                        (authorImageUrl != null && !authorImageUrl.isEmpty()) ? authorImageUrl : null
                );
            } else {
                logger.warn("Author name missing for tweet {}, cannot set embed author.", tweetUrl);
            }

            // Set Description (Tweet Text) - Check length
            if (tweetText.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
                logger.warn("Tweet text exceeds Discord embed description limit ({} chars). Truncating.", MessageEmbed.DESCRIPTION_MAX_LENGTH);
                embedBuilder.setDescription(tweetText.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 3) + "...");
            } else {
                embedBuilder.setDescription(tweetText);
            }

            // Set Thumbnail using Twitch logo (if available)
            twitchInfo.ifPresent(info -> {
                if (info.profileImageUrl() != null && !info.profileImageUrl().isEmpty()) {
                    embedBuilder.setThumbnail(info.profileImageUrl());
                    logger.debug("Set embed thumbnail to Twitch logo: {}", info.profileImageUrl());
                } else {
                    logger.warn("Twitch user info present, but profile image URL is missing.");
                }
            });

            // Set Image using the first image from the tweet (if available)
            if (!imageUrls.isEmpty() && imageUrls.get(0) != null && !imageUrls.get(0).isEmpty()) {
                embedBuilder.setImage(imageUrls.get(0));
                logger.debug("Set embed image to first tweet image: {}", imageUrls.get(0));
                if (imageUrls.size() > 1) {
                    logger.info("Tweet {} has multiple images, only the first is shown in the main embed image.", tweetUrl);
                    // Optionally add other image URLs as fields or in description if space allows
                }
            }

            // Set Timestamp and Footer
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter("via twitter-discord-processor", "https://raw.githubusercontent.com/mlem/twitter-discord-processor/main/icon.png"); // Optional: Link to your repo icon

            // --- Send the Embed ---
            MessageEmbed embed = embedBuilder.build();

            logger.debug("Sending embed to Discord channel {}: Title='{}'", channelId, embed.getTitle());
            channel.sendMessageEmbeds(embed).queue( // Use sendMessageEmbeds
                    success -> logger.info("Successfully sent embed for {} to Discord channel {}", tweetFile.getName(), channelId),
                    error -> {
                        if (error instanceof InsufficientPermissionException) {
                            logger.error("Discord bot lacks permission (e.g., SEND_MESSAGES, EMBED_LINKS) in channel {}: {}", channelId, error.getMessage());
                        } else {
                            logger.error("Failed to send embed for file {} to Discord channel {}: {}", tweetFile.getName(), channelId, error.getMessage(), error);
                        }
                    }
            );
            return true; // Indicate successful attempt to process and queue

        } catch (IOException e) {
            logger.error("Failed to read tweet file {}: {}", tweetFile.getName(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("An unexpected error occurred during Discord notification for file {}: {}", tweetFile.getName(), e.getMessage(), e);
            return false;
        }
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
