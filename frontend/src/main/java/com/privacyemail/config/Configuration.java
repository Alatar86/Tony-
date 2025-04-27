package com.privacyemail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration manager for the Privacy-Focused Email Agent frontend.
 * Loads and provides access to configuration properties.
 */
public class Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    private static final String PROPERTIES_FILE = "/application.properties";

    private static Configuration instance;
    private final Properties properties = new Properties();

    /**
     * Private constructor to prevent direct instantiation.
     */
    private Configuration() {
        // Load properties file (logic to be added)
        // System.out.println("Placeholder: Load application.properties here");
        this.loadProperties(); // Call the method to load properties
    }

    /**
     * Get the singleton instance of the Configuration.
     *
     * @return Configuration instance
     */
    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    /**
     * Load properties from the configuration file.
     */
    private void loadProperties() {
        try (InputStream inputStream = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                logger.info("Configuration loaded successfully");
            } else {
                logger.error("Configuration file not found: {}", PROPERTIES_FILE);
                // Load defaults
                loadDefaults();
            }
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            // Load defaults
            loadDefaults();
        }
    }

    /**
     * Load default configuration values.
     */
    private void loadDefaults() {
        logger.info("Loading default configuration values");
        properties.setProperty("api.baseUrl", "http://localhost:5000");
        properties.setProperty("ui.title", "Privacy-Focused Email Agent");
        properties.setProperty("ui.windowWidth", "1024");
        properties.setProperty("ui.windowHeight", "768");
        properties.setProperty("connection.timeout", "10");
        properties.setProperty("connection.retries", "3");
    }

    /**
     * Get a string property.
     *
     * @param key Property key
     * @return Property value or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get a string property with a default value.
     *
     * @param key Property key
     * @param defaultValue Default value if property is not found
     * @return Property value or default value if not found
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get an integer property.
     *
     * @param key Property key
     * @param defaultValue Default value if property is not found or not an integer
     * @return Property value as integer or default value
     */
    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Get a boolean property.
     *
     * @param key Property key
     * @param defaultValue Default value if property is not found
     * @return Property value as boolean or default value
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
}
