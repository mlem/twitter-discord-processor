package com.example.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.commons.io.FileUtils; // Using Apache Commons IO

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DiscordNotifier {

    private final JDA jda;
    private final String channelId;

    public DiscordNotifier(String botToken, String channelId) throws LoginException, InterruptedException {
        if (botToken == null || channelId == null) {
            throw new IllegalArgumentException("Discord Bot Token and Channel ID must be provided.");
        }
        this.channelId = channelId;
        // Build JDA instance - Consider using createLight or createDefault depending on needs
        // Add necessary GatewayIntents if your bot needs to read messages, etc.
        this.jda = JDABuilder.createDefault(botToken)
                // Add .enableIntents(...) if needed
                .build();
        this.jda.awaitReady(); // Wait for JDA to be ready
        System.out.println("Discord Bot Connected!");
    }

    public boolean consume(File tweetFile) {
        try {
            String content = FileUtils.readFileToString(tweetFile, StandardCharsets.UTF_8);
            TextChannel channel = jda.getTextChannelById(channelId);

            if (channel != null) {
                channel.sendMessage("New Tweet Processed:\n```\n" + content + "\n```").queue(
                        success -> System.out.println("Successfully sent content of " + tweetFile.getName() + " to Discord channel " + channelId),
                        error -> System.err.println("Failed to send message to Discord channel " + channelId + ": " + error.getMessage())
                );
                return true; // Indicate success
            } else {
                System.err.println("Discord channel with ID " + channelId + " not found.");
                return false; // Indicate failure
            }
        } catch (IOException e) {
            System.err.println("Failed to read tweet file " + tweetFile.getName() + ": " + e.getMessage());
            return false; // Indicate failure
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during Discord notification: " + e.getMessage());
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