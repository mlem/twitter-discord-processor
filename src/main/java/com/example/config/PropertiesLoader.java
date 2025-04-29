package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PropertiesLoader {

    private static final String CONFIG_FILE_NAME = "config.properties"; // Use constant for filename
    private static final Logger logger = LoggerFactory.getLogger(PropertiesLoader.class);
    private final Properties properties;

    public PropertiesLoader() {
        properties = new Properties();
        boolean loaded = loadFromExternalFile(); // Try external first

        if (!loaded) {
            logger.info("External {} not found or failed to load. Attempting to load from classpath.", CONFIG_FILE_NAME);
            loadFromClasspath(); // Fallback to classpath
        }
    }

    /**
     * Attempts to load properties from an external file located next to the JAR.
     * @return true if loading was successful, false otherwise.
     */
    private boolean loadFromExternalFile() {
        try {
            // 1. Find the directory containing the JAR
            File jarFile = new File(PropertiesLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path jarDir = jarFile.getParentFile().toPath(); // Directory containing the jar

            // 2. Construct the path to the external config file
            Path externalConfigPath = jarDir.resolve(CONFIG_FILE_NAME);
            logger.info("Attempting to load configuration from external file: {}", externalConfigPath.toAbsolutePath());

            // 3. Check if the external file exists and is readable
            if (Files.exists(externalConfigPath) && Files.isReadable(externalConfigPath)) {
                // 4. Load properties using FileInputStream
                try (InputStream input = new FileInputStream(externalConfigPath.toFile())) {
                    properties.load(input);
                    logger.info("Successfully loaded properties from external file: {}", externalConfigPath.toAbsolutePath());
                    return true; // Loaded successfully
                } catch (IOException ioEx) {
                    logger.error("Error reading external properties file {}: {}", externalConfigPath.toAbsolutePath(), ioEx.getMessage(), ioEx);
                    return false; // Failed to load
                }
            } else {
                logger.debug("External configuration file not found or not readable at: {}", externalConfigPath.toAbsolutePath());
                return false; // File not found or readable
            }
        } catch (URISyntaxException | SecurityException | NullPointerException | IllegalStateException e) {
            // Handle potential errors finding the JAR path (e.g., running from IDE)
            logger.warn("Could not determine JAR location to check for external config file: {}. Will fall back to classpath.", e.getMessage());
            return false; // Cannot determine external path
        } catch (Exception e) {
            // Catch any other unexpected errors during external load attempt
            logger.error("Unexpected error attempting to load external config file: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads properties from the classpath (inside the JAR).
     */
    private void loadFromClasspath() {
        logger.info("Attempting to load configuration from classpath file: {}", CONFIG_FILE_NAME);
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (input == null) {
                logger.error("Could not find properties file {} in classpath either.", CONFIG_FILE_NAME);
                // Consider throwing an exception if config is absolutely required
                return;
            }
            properties.load(input);
            logger.info("Successfully loaded properties from classpath file: {}", CONFIG_FILE_NAME);
        } catch (IOException ex) {
            logger.error("Error loading properties file {} from classpath: {}", CONFIG_FILE_NAME, ex.getMessage(), ex);
            // Consider throwing an exception
        }
    }


    /**
     * Gets a property value by key.
     * @param key The property key.
     * @return The property value, or null if not found.
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets a property value by key, returning a default value if not found.
     * @param key The property key.
     * @param defaultValue The value to return if the key is not found.
     * @return The property value or the defaultValue.
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}
