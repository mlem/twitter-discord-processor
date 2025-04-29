package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates the necessary configuration properties and environment variables
 * required for the application to run.
 */
public class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    // Define required environment variables
    private static final String[] REQUIRED_ENV_VARS = {
            "TWITTER_BEARER_TOKEN",
            "DISCORD_BOT_TOKEN",
            "TWITCH_CLIENT_ID",
            "TWITCH_CLIENT_SECRET"
    };

    // Define required properties from config.properties
    private static final String[] REQUIRED_PROPERTIES = {
            "discord.channel.id",
            "twitch.username"
    };

    // Define the conditional Twitter username property/env var
    private static final String TWITTER_USERNAME_ENV = "TWITTER_USERNAME";
    private static final String TWITTER_USERNAME_PROP = "twitter.username";

    private final PropertiesLoader properties;
    private final List<String> validationErrors;

    /**
     * Constructor requires the loaded properties.
     * @param properties The loaded Properties object from config.properties.
     */
    public ConfigurationValidator(PropertiesLoader properties) {
        this.properties = properties;
        this.validationErrors = new ArrayList<>();
    }

    /**
     * Checks if a given string is null or effectively empty (only whitespace).
     * @param str The string to check.
     * @return true if the string is null or empty/whitespace, false otherwise.
     */
    private boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Performs the validation checks.
     * @return true if all required configurations are present and valid, false otherwise.
     */
    public boolean validate() {
        validationErrors.clear(); // Clear previous errors

        logger.debug("Starting configuration validation...");

        // 1. Check required environment variables
        logger.debug("Validating required environment variables...");
        for (String envVar : REQUIRED_ENV_VARS) {
            if (isNullOrBlank(System.getenv(envVar))) {
                String errorMsg = String.format("Required environment variable '%s' is missing or empty.", envVar);
                validationErrors.add(errorMsg);
                logger.warn(errorMsg); // Log warning immediately
            } else {
                logger.trace("Environment variable '{}' found.", envVar);
            }
        }

        // 2. Check required properties file entries
        logger.debug("Validating required properties from config.properties...");
        for (String propKey : REQUIRED_PROPERTIES) {
            if (isNullOrBlank(properties.getProperty(propKey))) {
                String errorMsg = String.format("Required property '%s' is missing or empty in config.properties.", propKey);
                validationErrors.add(errorMsg);
                logger.warn(errorMsg);
            } else {
                logger.trace("Property '{}' found.", propKey);
            }
        }

        // 3. Check conditional Twitter Username requirement
        logger.debug("Validating Twitter username configuration...");
        String twitterUsernameEnvValue = System.getenv(TWITTER_USERNAME_ENV);
        String twitterUsernamePropValue = properties.getProperty(TWITTER_USERNAME_PROP);

        if (isNullOrBlank(twitterUsernameEnvValue) && isNullOrBlank(twitterUsernamePropValue)) {
            String errorMsg = String.format("Required configuration missing: Set either environment variable '%s' OR property '%s' in config.properties.",
                    TWITTER_USERNAME_ENV, TWITTER_USERNAME_PROP);
            validationErrors.add(errorMsg);
            logger.warn(errorMsg);
        } else {
            logger.trace("Twitter username configuration found (either via ENV or properties).");
        }

        logger.debug("Configuration validation finished.");
        return validationErrors.isEmpty(); // Return true if no errors were added
    }

    /**
     * Gets the list of validation errors found during the last call to validate().
     * @return A List of strings describing the errors. Returns an empty list if validation passed.
     */
    public List<String> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors); // Return immutable view
    }

    /**
     * Generates a formatted error message listing all validation problems.
     * @return A string detailing all validation errors, or an empty string if none.
     */
    public String getFormattedErrorSummary() {
        if (validationErrors.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder("Configuration validation failed. Please address the following issues:\n");
        for (String error : validationErrors) {
            summary.append("  - ").append(error).append("\n");
        }
        return summary.toString();
    }
}
