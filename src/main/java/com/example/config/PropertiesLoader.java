package com.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {

    private static final String CONFIG_FILE = "config.properties";
    private final Properties properties;

    public PropertiesLoader() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("Sorry, unable to find " + CONFIG_FILE);
                // Handle error appropriately - maybe throw an exception or exit
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            System.err.println("Error loading properties file: " + CONFIG_FILE);
            ex.printStackTrace(); // Consider better logging
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}