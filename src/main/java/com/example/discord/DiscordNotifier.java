package com.example.discord;

import com.example.file.TweetWriter; // Import to access separator
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordNotifier {

    // Initialize Logger
    private static final Logger logger = LoggerFactory.getLogger(DiscordNotifier.class);
    private final JDA jda;
    private final String channelId;
    private static final int MAX_DISCORD_MESSAGE_LENGTH = 2000;

    public DiscordNotifier(String botToken, String channelId) throws LoginException, InterruptedException {
        if (botToken == null || channelId == null) {
            logger.error("Discord Bot Token and Channel ID must be provided.");
            throw new IllegalArgumentException("Discord Bot Token and Channel ID must be provided.");
        }
        this.channelId = channelId;
        logger.info("Initializing Discord Notifier for channel ID: {}", channelId);
        try {
            this.jda = JDABuilder.createDefault(botToken).build();
            this.jda.awaitReady(); // Wait for JDA to be ready
            logger.info("Discord Bot Connected and Ready!");
        } catch (LoginException e) {
            logger.error("Discord login failed: {}", e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            logger.error("Discord connection interrupted while waiting for ready state.", e);
            Thread.currentThread().interrupt(); // Re-interrupt the thread
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during JDA initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize JDA", e); // Wrap unexpected exceptions
        }
    }

    public boolean consume(File tweetFile) {
        logger.info("Consuming tweet file: {}", tweetFile.getName());
        try {
            List<String> lines = FileUtils.readLines(tweetFile, StandardCharsets.UTF_8);

            String tweetText = "";
            String tweetUrl = "";
            List<String> imageUrls = Collections.emptyList();

            // Parse the file content
            for (String line : lines) {
                if (line.startsWith("Text: ")) {
                    tweetText = line.substring("Text: ".length());
                } else if (line.startsWith("URL: ")) {
                    tweetUrl = line.substring("URL: ".length());
                } else if (line.startsWith("ImageURLs: ")) {
                    String urlsPart = line.substring("ImageURLs: ".length());
                    if (!urlsPart.trim().isEmpty()) {
                        // Use the separator defined in TweetWriter
                        imageUrls = Arrays.asList(urlsPart.split(TweetWriter.IMAGE_URL_SEPARATOR));
                    }
                }
            }
            logger.debug("Parsed from {}: Text='{}...', URL='{}', ImageCount={}",
                    tweetFile.getName(), tweetText.substring(0, Math.min(tweetText.length(), 30)), tweetUrl, imageUrls.size());

            if (tweetText.isEmpty() || tweetUrl.isEmpty()) {
                logger.error("Could not parse required fields (Text, URL) from file: {}", tweetFile.getName());
                return false;
            }

            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Discord channel with ID {} not found or bot lacks access.", channelId);
                return false;
            }

            // Construct the message content
            StringBuilder messageContentBuilder = new StringBuilder();
            messageContentBuilder.append(tweetText).append("\n"); // Tweet text first

            for (String imageUrl : imageUrls) {
                if (messageContentBuilder.length() + imageUrl.length() + 1 < MAX_DISCORD_MESSAGE_LENGTH) {
                    messageContentBuilder.append(imageUrl).append("\n");
                } else {
                    logger.warn("Message content too long for file {}, skipping image URL: {}", tweetFile.getName(), imageUrl);
                    // Optionally send remaining URLs in a separate message or truncate
                    break; // Stop adding more images if limit is near
                }
            }

            String originalTweetLink = "\nOriginal Tweet: " + tweetUrl;
            if (messageContentBuilder.length() + originalTweetLink.length() <= MAX_DISCORD_MESSAGE_LENGTH) {
                messageContentBuilder.append(originalTweetLink);
            } else {
                logger.warn("Message content too long for file {}, skipping original tweet link.", tweetFile.getName());
            }

            MessageCreateData messageData = new MessageCreateBuilder()
                    .setContent(messageContentBuilder.toString())
                    .build();

            logger.debug("Sending message to Discord channel {}: {}", channelId, messageData.getContent().substring(0, Math.min(messageData.getContent().length(), 50)) + "...");
            channel.sendMessage(messageData).queue(
                    success -> logger.info("Successfully sent content of {} to Discord channel {}", tweetFile.getName(), channelId),
                    error -> {
                        if (error instanceof InsufficientPermissionException) {
                            logger.error("Discord bot lacks permission to send messages in channel {}: {}", channelId, error.getMessage());
                        } else {
                            logger.error("Failed to send message for file {} to Discord channel {}: {}", tweetFile.getName(), channelId, error.getMessage(), error);
                        }
                        // Note: The success/failure of the queue doesn't directly impact the return value here,
                        // as the file processing logic depends on whether consume *attempted* successfully.
                        // Consider if failed queue should mark the file as failed.
                    }
            );
            return true; // Indicate successful attempt to process and queue

        } catch (IOException e) {
            logger.error("Failed to read tweet file {}: {}", tweetFile.getName(), e.getMessage(), e);
            return false; // Indicate failure
        } catch (Exception e) {
            logger.error("An unexpected error occurred during Discord notification for file {}: {}", tweetFile.getName(), e.getMessage(), e);
            return false; // Indicate failure
        }
    }

    public void shutdown() {
        if (jda != null) {
            logger.info("Shutting down Discord Bot connection...");
            jda.shutdown();
            // Consider using jda.shutdownNow() if shutdown hangs
            logger.info("Discord Bot Shut Down.");
        }
    }
}
