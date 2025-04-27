package com.privacyemail.util;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing application themes and providing theme-aware styling.
 * Centralizes theme detection and style generation for consistent UI appearance.
 */
public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    /**
     * Determines if the dark theme is currently active based on the scene's stylesheets.
     *
     * @param scene The JavaFX scene to check
     * @return true if dark theme is active, false otherwise
     */
    public static boolean isDarkThemeActive(Scene scene) {
        if (scene == null) {
            logger.debug("Scene is null, returning default theme (not dark)");
            return false;
        }

        // Check if scene's stylesheets include dark theme
        for (String stylesheet : scene.getStylesheets()) {
            if (stylesheet.contains("dark-theme")) {
                logger.debug("Dark theme detected in stylesheets");
                return true;
            }
        }
        logger.debug("Dark theme not detected in stylesheets");
        return false;
    }

    /**
     * Generates common base styles for HTML content based on the current theme.
     *
     * @param isDarkTheme Whether the dark theme is active
     * @return A StringBuilder containing common CSS styles
     */
    public static StringBuilder generateBaseStyles(boolean isDarkTheme) {
        StringBuilder styleBuilder = new StringBuilder();

        // Base styles for all themes
        styleBuilder.append("body { ");
        styleBuilder.append("font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; ");
        styleBuilder.append("line-height: 1.5; ");
        styleBuilder.append("margin: 20px; ");
        styleBuilder.append("font-size: 14px; ");

        // Apply theme-specific colors
        if (isDarkTheme) {
            styleBuilder.append("color: #e6e6e6; ");
            styleBuilder.append("background-color: #3c3f41; ");
        } else {
            styleBuilder.append("color: #212529; ");
            styleBuilder.append("background-color: #ffffff; ");
        }
        styleBuilder.append("}");

        return styleBuilder;
    }

    /**
     * Generates a complete set of HTML/CSS styles for email content based on the current theme.
     *
     * @param isDarkTheme Whether the dark theme is active
     * @return A string containing the complete CSS styles
     */
    public static String generateEmailContentStyles(boolean isDarkTheme) {
        StringBuilder styleBuilder = generateBaseStyles(isDarkTheme);

        // Pre/code blocks
        styleBuilder.append("pre, code { ");
        styleBuilder.append("font-family: 'Courier New', Courier, monospace; ");
        styleBuilder.append("white-space: pre-wrap; ");
        if (isDarkTheme) {
            styleBuilder.append("background-color: #2b2b2b; ");
        } else {
            styleBuilder.append("background-color: #f8f9fa; ");
        }
        styleBuilder.append("padding: 10px; ");
        styleBuilder.append("border-radius: 4px; ");
        styleBuilder.append("}");

        // Blockquotes
        styleBuilder.append("blockquote { ");
        if (isDarkTheme) {
            styleBuilder.append("border-left: 3px solid #555; ");
            styleBuilder.append("color: #bbb; ");
        } else {
            styleBuilder.append("border-left: 3px solid #ccc; ");
            styleBuilder.append("color: #555; ");
        }
        styleBuilder.append("margin-left: 5px; ");
        styleBuilder.append("padding-left: 15px; ");
        styleBuilder.append("}");

        // Links
        styleBuilder.append("a { ");
        if (isDarkTheme) {
            styleBuilder.append("color: #4dacff; ");
        } else {
            styleBuilder.append("color: #0d6efd; ");
        }
        styleBuilder.append("text-decoration: none; ");
        styleBuilder.append("}");
        styleBuilder.append("a:hover { text-decoration: underline; }");

        // Tables - modified to respect original email formatting
        styleBuilder.append("table { border-collapse: collapse; width: auto; max-width: 100%; }");
        styleBuilder.append("table[width] { width: attr(width); }");

        // Don't add borders to tables in marketing emails
        styleBuilder.append("td, th { padding: inherit; }");

        // Images
        styleBuilder.append("img { max-width: 100%; height: auto; vertical-align: middle; }");

        // Common email client elements
        styleBuilder.append(".ExternalClass { width: 100%; }"); // Outlook-specific
        styleBuilder.append(".ReadMsgBody { width: 100%; }"); // Older Outlook
        styleBuilder.append("span.MsoHyperlink { color: inherit; }"); // MS Word-generated links
        styleBuilder.append("span.MsoHyperlinkFollowed { color: inherit; }");

        // Override for marketing emails with specific backgrounds
        styleBuilder.append(".email-body { max-width: 100% !important; margin: 0 !important; padding: 0 !important; }");

        return styleBuilder.toString();
    }

    /**
     * Generates styles specific for plain text email content.
     *
     * @param isDarkTheme Whether the dark theme is active
     * @return A string containing the plain text specific styles
     */
    public static String generatePlainTextStyles(boolean isDarkTheme) {
        StringBuilder styleBuilder = generateBaseStyles(isDarkTheme);

        // Pre tag for plain text content
        styleBuilder.append("pre { ");
        styleBuilder.append("font-family: 'Courier New', Courier, monospace; ");
        styleBuilder.append("white-space: pre-wrap; ");
        styleBuilder.append("word-wrap: break-word; ");
        styleBuilder.append("margin: 0; ");
        styleBuilder.append("padding: 0; ");

        // Apply theme-specific colors for pre tag
        if (isDarkTheme) {
            styleBuilder.append("color: #e6e6e6; ");
        } else {
            styleBuilder.append("color: #212529; ");
        }
        styleBuilder.append("}");

        return styleBuilder.toString();
    }
}
