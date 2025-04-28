package com.example.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TwitchService {

    private static final Logger logger = LoggerFactory.getLogger(TwitchService.class);
    private final TwitchClient twitchClient;
    private final String clientId;
    private final String clientSecret;

    public TwitchService(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) {
            logger.error("Twitch Client ID and Client Secret must be provided via environment variables.");
            throw new IllegalArgumentException("Twitch Client ID and Client Secret are required.");
        }
        this.clientId = clientId;
        this.clientSecret = clientSecret;

        logger.info("Initializing TwitchService...");
        // Build TwitchClient instance
        // Using App Access Token flow (Client Credentials) for server-to-server API calls
        try {
            // Note: For production, consider a more robust credential manager or token refresh strategy
            OAuth2Credential credential = new OAuth2Credential("twitch", clientSecret); // Secret used here, might need App Access Token flow explicitly

            twitchClient = TwitchClientBuilder.builder()
                    .withEnableHelix(true)
                    .withClientId(clientId) // Client ID needed for requests
                    .withClientSecret(clientSecret) // Needed for App Access Token generation
                    // .withDefaultAuthToken(credential) // Might be needed depending on library version/flow
                    .build();
            logger.info("TwitchClient initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize TwitchClient: {}", e.getMessage(), e);
            throw new RuntimeException("TwitchClient initialization failed", e);
        }
    }

    /**
     * Fetches user information from Twitch Helix API.
     *
     * @param username The Twitch username to look up.
     * @return An Optional containing TwitchUserInfo if found, otherwise Optional.empty().
     */
    public Optional<TwitchUserInfo> fetchUserInfo(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Cannot fetch Twitch user info for null or empty username.");
            return Optional.empty();
        }
        logger.info("Fetching Twitch user info for username: {}", username);
        try {
            // Fetch user data using the Helix endpoint
            UserList resultList = twitchClient.getHelix().getUsers(null, null, Collections.singletonList(username)).execute();

            if (resultList == null || resultList.getUsers() == null || resultList.getUsers().isEmpty()) {
                logger.warn("No Twitch user found for username: {}", username);
                return Optional.empty();
            }

            User twitchUser = resultList.getUsers().get(0);
            String profileImageUrl = twitchUser.getProfileImageUrl();
            String channelUrl = "https://www.twitch.tv/" + twitchUser.getLogin(); // Construct channel URL

            logger.info("Successfully fetched Twitch info for {}: Profile Image URL obtained.", username);
            return Optional.of(new TwitchUserInfo(profileImageUrl, channelUrl));

        } catch (Exception e) {
            // Log API errors or other exceptions
            logger.error("Error fetching Twitch user info for {}: {}", username, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
