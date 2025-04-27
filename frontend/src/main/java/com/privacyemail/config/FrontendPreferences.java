package com.privacyemail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages user-specific frontend preferences that are stored locally.
 * This includes UI theme preferences and email display settings.
 */
public class FrontendPreferences {

    private static final Logger logger = LoggerFactory.getLogger(FrontendPreferences.class);

    // Constants for preference keys
    public static final String THEME_PREFERENCE = "ui.theme";
    public static final String IMAGE_LOADING_PREFERENCE = "email.image_loading";
    public static final String AI_SUGGESTIONS_ENABLED = "ai.suggestions.enabled";

    // Constants for theme values
    public static final String THEME_LIGHT = "Light";
    public static final String THEME_DARK = "Dark";
    public static final String THEME_SYSTEM = "System Default";

    // Constants for image loading values
    public static final String IMAGE_LOADING_ALWAYS = "Always";
    public static final String IMAGE_LOADING_ASK = "Ask";
    public static final String IMAGE_LOADING_NEVER = "Never";

    // Default values
    public static final String DEFAULT_THEME = THEME_SYSTEM;
    public static final String DEFAULT_IMAGE_LOADING = IMAGE_LOADING_ASK;
    public static final boolean DEFAULT_AI_SUGGESTIONS_ENABLED = true;

    private static FrontendPreferences instance;
    private final Properties properties = new Properties();
    private final File preferencesFile;

    /**
     * Private constructor to enforce singleton pattern
     */
    private FrontendPreferences() {
        // Determine preferences file location in user's home directory
        preferencesFile = getPreferencesFile();
        loadPreferences();
    }

    /**
     * Get the preferences file in a cross-platform compatible way
     *
     * @return The preferences file
     */
    private File getPreferencesFile() {
        String userHome = System.getProperty("user.home");

        // Create a directory path that works on all platforms
        Path appDirPath;
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            // Windows: AppData/Local
            Path appDataPath = Paths.get(userHome, "AppData", "Local");
            appDirPath = appDataPath.resolve("PrivacyEmail");
        } else if (osName.contains("mac")) {
            // macOS: ~/Library/Application Support
            Path libPath = Paths.get(userHome, "Library", "Application Support");
            appDirPath = libPath.resolve("PrivacyEmail");
        } else {
            // Linux/Unix: ~/.config
            Path configPath = Paths.get(userHome, ".config");
            appDirPath = configPath.resolve("privacyemail");
        }

        // Create directory if it doesn't exist
        File appDir = appDirPath.toFile();
        if (!appDir.exists()) {
            boolean created = appDir.mkdirs();
            if (!created) {
                logger.warn("Could not create preferences directory: {}", appDir.getAbsolutePath());

                // Fall back to user home directory if we can't create the preferred location
                appDir = new File(userHome);
                logger.info("Using fallback preferences directory: {}", appDir.getAbsolutePath());
            }
        }

        return new File(appDir, "frontend_preferences.properties");
    }

    /**
     * Get the singleton instance
     * @return The FrontendPreferences instance
     */
    public static synchronized FrontendPreferences getInstance() {
        if (instance == null) {
            instance = new FrontendPreferences();
        }
        return instance;
    }

    /**
     * Load preferences from file
     */
    private void loadPreferences() {
        // Initialize with defaults
        properties.setProperty(THEME_PREFERENCE, DEFAULT_THEME);
        properties.setProperty(IMAGE_LOADING_PREFERENCE, DEFAULT_IMAGE_LOADING);
        properties.setProperty(AI_SUGGESTIONS_ENABLED, String.valueOf(DEFAULT_AI_SUGGESTIONS_ENABLED));

        // Load from file if exists
        if (preferencesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(preferencesFile)) {
                properties.load(fis);
                logger.info("Frontend preferences loaded from: {}", preferencesFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Error loading frontend preferences", e);
            }
        } else {
            logger.info("Frontend preferences file not found, using defaults");
            savePreferences(); // Create the file with defaults
        }
    }

    /**
     * Save preferences to file
     */
    public void savePreferences() {
        try (FileOutputStream fos = new FileOutputStream(preferencesFile)) {
            properties.store(fos, "Privacy-Focused Email Agent Frontend Preferences");
            logger.info("Frontend preferences saved to: {}", preferencesFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error saving frontend preferences", e);
        }
    }

    /**
     * Get a preference value
     * @param key The preference key
     * @return The preference value or null if not found
     */
    public String getPreference(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get a preference value with default
     * @param key The preference key
     * @param defaultValue Default value if not found
     * @return The preference value or default if not found
     */
    public String getPreference(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Set a preference value
     * @param key The preference key
     * @param value The preference value
     */
    public void setPreference(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Get the theme preference
     * @return The current theme preference
     */
    public String getThemePreference() {
        return getPreference(THEME_PREFERENCE, DEFAULT_THEME);
    }

    /**
     * Set the theme preference
     * @param theme The theme to set
     */
    public void setThemePreference(String theme) {
        setPreference(THEME_PREFERENCE, theme);
    }

    /**
     * Get the image loading preference
     * @return The current image loading preference
     */
    public String getImageLoadingPreference() {
        return getPreference(IMAGE_LOADING_PREFERENCE, DEFAULT_IMAGE_LOADING);
    }

    /**
     * Set the image loading preference
     * @param imageLoading The image loading policy to set
     */
    public void setImageLoadingPreference(String imageLoading) {
        setPreference(IMAGE_LOADING_PREFERENCE, imageLoading);
    }

    /**
     * Validate if a theme value is supported
     * @param theme The theme value to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidTheme(String theme) {
        return THEME_LIGHT.equals(theme) ||
               THEME_DARK.equals(theme) ||
               THEME_SYSTEM.equals(theme);
    }

    /**
     * Validate if an image loading value is supported
     * @param imageLoading The image loading value to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidImageLoading(String imageLoading) {
        return IMAGE_LOADING_ALWAYS.equals(imageLoading) ||
               IMAGE_LOADING_ASK.equals(imageLoading) ||
               IMAGE_LOADING_NEVER.equals(imageLoading);
    }

    /**
     * Get a boolean preference value
     * @param key The preference key
     * @param defaultValue Default value if not found
     * @return The preference value as boolean or default if not found
     */
    public boolean getBooleanPreference(String key, boolean defaultValue) {
        String value = getPreference(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Set a boolean preference value
     * @param key The preference key
     * @param value The boolean preference value
     */
    public void setBooleanPreference(String key, boolean value) {
        setPreference(key, String.valueOf(value));
    }

    /**
     * Check if AI suggestions are enabled
     * @return true if AI suggestions are enabled, false otherwise
     */
    public boolean isAiSuggestionsEnabled() {
        String value = getPreference(AI_SUGGESTIONS_ENABLED, String.valueOf(DEFAULT_AI_SUGGESTIONS_ENABLED));
        return Boolean.parseBoolean(value);
    }

    /**
     * Set whether AI suggestions are enabled
     * @param enabled Whether AI suggestions should be enabled
     */
    public void setAiSuggestionsEnabled(boolean enabled) {
        setPreference(AI_SUGGESTIONS_ENABLED, String.valueOf(enabled));
    }
}
