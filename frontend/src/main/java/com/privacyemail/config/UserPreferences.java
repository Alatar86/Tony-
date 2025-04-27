package com.privacyemail.config;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing user preferences in the application.
 * Provides methods to get and set user preferences for email display,
 * plugins, and other application settings.
 */
public class UserPreferences {

    // Preference keys
    public static final String PREF_IMAGE_LOADING = "email.image.loading";
    public static final String PREF_PLUGINS_ENABLED = "plugins.enabled";
    public static final String PREF_THEME = "app.theme";
    public static final String PREF_FONT_SIZE = "app.font.size";

    // Preference values
    public static final String IMAGE_LOADING_ALWAYS = "ALWAYS";
    public static final String IMAGE_LOADING_NEVER = "NEVER";
    public static final String IMAGE_LOADING_ASK = "ASK";
    public static final String IMAGE_LOADING_BLOCK_EXTERNAL = "BLOCK_EXTERNAL";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    // Default values
    private static final String DEFAULT_IMAGE_LOADING = IMAGE_LOADING_BLOCK_EXTERNAL;
    private static final boolean DEFAULT_PLUGINS_ENABLED = true;
    private static final String DEFAULT_THEME = THEME_SYSTEM;
    private static final int DEFAULT_FONT_SIZE = 12;

    private static final Logger logger = LoggerFactory.getLogger(UserPreferences.class);
    private static UserPreferences instance;

    private final Preferences prefs;
    private final Map<String, Object> cache = new HashMap<>();

    /**
     * Private constructor for singleton pattern
     */
    private UserPreferences() {
        prefs = Preferences.userNodeForPackage(UserPreferences.class);
        loadCachedPreferences();
    }

    /**
     * Get the singleton instance of UserPreferences
     * @return the UserPreferences instance
     */
    public static synchronized UserPreferences getInstance() {
        if (instance == null) {
            instance = new UserPreferences();
        }
        return instance;
    }

    /**
     * Load commonly used preferences into cache for faster access
     */
    private void loadCachedPreferences() {
        cache.put(PREF_IMAGE_LOADING, prefs.get(PREF_IMAGE_LOADING, DEFAULT_IMAGE_LOADING));
        cache.put(PREF_PLUGINS_ENABLED, prefs.getBoolean(PREF_PLUGINS_ENABLED, DEFAULT_PLUGINS_ENABLED));
        cache.put(PREF_THEME, prefs.get(PREF_THEME, DEFAULT_THEME));
        cache.put(PREF_FONT_SIZE, prefs.getInt(PREF_FONT_SIZE, DEFAULT_FONT_SIZE));
    }

    /**
     * Get the image loading preference
     * @return the image loading preference value
     */
    public String getImageLoadingPreference() {
        return (String) cache.get(PREF_IMAGE_LOADING);
    }

    /**
     * Set the image loading preference
     * @param value the new image loading preference value
     */
    public void setImageLoadingPreference(String value) {
        if (!IMAGE_LOADING_ALWAYS.equals(value) &&
            !IMAGE_LOADING_NEVER.equals(value) &&
            !IMAGE_LOADING_ASK.equals(value) &&
            !IMAGE_LOADING_BLOCK_EXTERNAL.equals(value)) {

            logger.warn("Invalid image loading preference: {}, using default", value);
            value = DEFAULT_IMAGE_LOADING;
        }

        prefs.put(PREF_IMAGE_LOADING, value);
        cache.put(PREF_IMAGE_LOADING, value);
        logger.debug("Set image loading preference to: {}", value);
    }

    /**
     * Check if plugins are enabled
     * @return true if plugins are enabled
     */
    public boolean isPluginsEnabled() {
        return (Boolean) cache.get(PREF_PLUGINS_ENABLED);
    }

    /**
     * Enable or disable plugins
     * @param enabled true to enable plugins, false to disable
     */
    public void setPluginsEnabled(boolean enabled) {
        prefs.putBoolean(PREF_PLUGINS_ENABLED, enabled);
        cache.put(PREF_PLUGINS_ENABLED, enabled);
        logger.debug("Plugins {} enabled", enabled ? "are now" : "are now not");
    }

    /**
     * Get the theme preference
     * @return the theme name
     */
    public String getTheme() {
        return (String) cache.get(PREF_THEME);
    }

    /**
     * Set the theme preference
     * @param theme the theme name
     */
    public void setTheme(String theme) {
        if (!THEME_LIGHT.equals(theme) && !THEME_DARK.equals(theme) && !THEME_SYSTEM.equals(theme)) {
            logger.warn("Invalid theme: {}, using default", theme);
            theme = DEFAULT_THEME;
        }

        prefs.put(PREF_THEME, theme);
        cache.put(PREF_THEME, theme);
        logger.debug("Set theme to: {}", theme);
    }

    /**
     * Get the font size preference
     * @return the font size
     */
    public int getFontSize() {
        return (Integer) cache.get(PREF_FONT_SIZE);
    }

    /**
     * Set the font size preference
     * @param size the font size
     */
    public void setFontSize(int size) {
        if (size < 8 || size > 24) {
            logger.warn("Invalid font size: {}, using default", size);
            size = DEFAULT_FONT_SIZE;
        }

        prefs.putInt(PREF_FONT_SIZE, size);
        cache.put(PREF_FONT_SIZE, size);
        logger.debug("Set font size to: {}", size);
    }

    /**
     * Reset all preferences to their default values
     */
    public void resetToDefaults() {
        setImageLoadingPreference(DEFAULT_IMAGE_LOADING);
        setPluginsEnabled(DEFAULT_PLUGINS_ENABLED);
        setTheme(DEFAULT_THEME);
        setFontSize(DEFAULT_FONT_SIZE);
        logger.info("Reset all preferences to defaults");
    }
}
