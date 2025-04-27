package com.privacyemail.util.interfaces;

/**
 * Interface for components that render email content.
 * This provides an abstraction layer for different rendering strategies.
 */
public interface EmailRenderer {

    /**
     * Renders HTML email content with appropriate styling.
     *
     * @param content The original HTML content
     * @param isDarkTheme Whether dark theme is active
     * @param imageLoadingPreference User's preference for loading images
     * @return Processed and styled HTML content
     */
    String renderHtmlContent(String content, boolean isDarkTheme, String imageLoadingPreference);

    /**
     * Renders plain text email content with appropriate styling.
     *
     * @param content The plain text content
     * @param isDarkTheme Whether dark theme is active
     * @return HTML-formatted content
     */
    String renderPlainTextContent(String content, boolean isDarkTheme);
}
