package com.privacyemail.util.interfaces;

import javafx.scene.Scene;

/**
 * Interface for components that provide theme-related functionality.
 * This allows for different theme implementation strategies.
 */
public interface ThemeProvider {

    /**
     * Determines if the dark theme is currently active.
     *
     * @param scene The JavaFX scene to check
     * @return true if dark theme is active, false otherwise
     */
    boolean isDarkThemeActive(Scene scene);

    /**
     * Generates CSS styles for email content based on the current theme.
     *
     * @param isDarkTheme Whether the dark theme is active
     * @return A string containing the CSS styles
     */
    String generateEmailContentStyles(boolean isDarkTheme);

    /**
     * Generates CSS styles for plain text email content.
     *
     * @param isDarkTheme Whether the dark theme is active
     * @return A string containing the CSS styles
     */
    String generatePlainTextStyles(boolean isDarkTheme);

    /**
     * Applies a theme to a scene.
     *
     * @param scene The scene to apply the theme to
     * @param themeName The name of the theme to apply
     */
    void applyTheme(Scene scene, String themeName);
}
