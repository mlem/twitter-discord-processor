package com.example.discord;

import com.example.file.TweetWriter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.io.FileUtils;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordNotifier {

    private final JDA jda;
    private final String channelId;
    private static final int MAX_DISCORD_MESSAGE_LENGTH = 2000; // Discord message length limit

    public DiscordNotifier(String botToken, String channelId) throws LoginException, InterruptedException {
        if (botToken == null || channelId == null) {
            throw new IllegalArgumentException("Discord Bot Token and Channel ID must be provided.");
        }
        this.channelId = channelId;
        this.jda = JDABuilder.createDefault(botToken)
                .build();
        this.jda.awaitReady();
        System.out.println("Discord Bot Connected!");
    }

    public boolean consume(File tweetFile) {
        try {
            List<String> lines = FileUtils.readLines(tweetFile, StandardCharsets.UTF_8);

            String tweetText = "";
            String tweetUrl = "";
            List<String> imageUrls = Collections.emptyList();

            // Parse the file content based on prefixes
            for (String line : lines) {
                if (line.startsWith("Text: ")) {
                    tweetText = line.substring("Text: ".length());
                } else if (line.startsWith("URL: ")) {
                    tweetUrl = line.substring("URL: ".length());
                } else if (line.startsWith("ImageURLs: ")) {
                    String urlsPart = line.substring("ImageURLs: ".length());
                    if (!urlsPart.trim().isEmpty()) {
                        imageUrls = Arrays.asList(urlsPart.split(TweetWriter.IMAGE_URL_SEPARATOR)); // Use the same separator
                    }
                }
            }

            if (tweetText.isEmpty() || tweetUrl.isEmpty()) {
                System.err.println("Could not parse required fields (Text, URL) from file: " + tweetFile.getName());
                return false;
            }


            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                System.err.println("Discord channel with ID " + channelId + " not found.");
                return false; // Indicate failure
            }

            // Construct the message content
            StringBuilder messageContentBuilder = new StringBuilder();
            messageContentBuilder.append(tweetText).append("\n"); // Tweet text first

            // Add image URLs on separate lines - Discord usually embeds these
            for (String imageUrl : imageUrls) {
                // Check length before appending each URL to prevent exceeding limit
                if (messageContentBuilder.length() + imageUrl.length() + 1 < MAX_DISCORD_MESSAGE_LENGTH) {
                    messageContentBuilder.append(imageUrl).append("\n");
                } else {
                    System.err.println("Warning: Message content too long to add image URL: " + imageUrl);
                    // Optionally send remaining URLs in a separate message or truncate
                }
            }

            // Add the original tweet link at the bottom
            String originalTweetLink = "\nOriginal Tweet: " + tweetUrl;
            if (messageContentBuilder.length() + originalTweetLink.length() <= MAX_DISCORD_MESSAGE_LENGTH) {
                messageContentBuilder.append(originalTweetLink);
            } else {
                System.err.println("Warning: Message content too long to add original tweet link.");
                // Consider sending link separately if needed
            }

            // Send the message
            MessageCreateData messageData = new MessageCreateBuilder()
                    .setContent(messageContentBuilder.toString())
                    // Disable mentions if desired: .setAllowedMentions(Collections.emptyList())
                    .build();

            channel.sendMessage(messageData).queue(
                    success -> System.out.println("Successfully sent content of " + tweetFile.getName() + " to Discord channel " + channelId),
                    error -> System.err.println("Failed to send message to Discord channel " + channelId + ": " + error.getMessage())
            );
            return true; // Indicate success

        } catch (IOException e) {
            System.err.println("Failed to read tweet file " + tweetFile.getName() + ": " + e.getMessage());
            return false; // Indicate failure
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during Discord notification for file " + tweetFile.getName() + ": " + e.getMessage());
            e.printStackTrace(); // Consider better logging
            return false; // Indicate failure
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            System.out.println("Discord Bot Shut Down.");
        }
    }
}