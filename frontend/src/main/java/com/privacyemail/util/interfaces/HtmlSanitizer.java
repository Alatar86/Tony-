package com.privacyemail.util.interfaces;

/**
 * Interface for components that sanitize HTML content.
 * This provides an abstraction layer for different sanitization strategies.
 */
public interface HtmlSanitizer {

    /**
     * Sanitizes HTML content to prevent security issues and rendering problems.
     *
     * @param html The HTML content to sanitize
     * @return Sanitized HTML content
     */
    String sanitize(String html);

    /**
     * Fixes common issues with HTML structure.
     *
     * @param html The HTML content to fix
     * @return Fixed HTML content
     */
    String fixHtmlStructure(String html);

    /**
     * Removes potentially harmful content from HTML.
     *
     * @param html The HTML content to process
     * @return HTML content with harmful elements removed
     */
    String removeHarmfulContent(String html);
}
