package com.privacyemail.util.plugin.impl;

import com.privacyemail.models.EmailDetails;
import com.privacyemail.util.plugin.EmailViewPlugin;
import javafx.scene.web.WebView;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin that blocks external images in emails for privacy protection.
 * This plugin replaces external image URLs with placeholders.
 */
public class ImageBlockerPlugin implements EmailViewPlugin {
    private static final Logger LOGGER = Logger.getLogger(ImageBlockerPlugin.class.getName());
    private static final String PLUGIN_ID = "com.privacyemail.plugin.imageblocker";
    private static final String PLUGIN_NAME = "Image Blocker";
    private static final String PLUGIN_DESCRIPTION = "Blocks external images in emails for privacy protection";
    private static final String PLUGIN_VERSION = "1.0";

    private static final String PLACEHOLDER_IMAGE = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2VlZWVlZSIvPgogIDx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMTQiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5OTk5OTkiPkltYWdlIEJsb2NrZWQ8L3RleHQ+Cjwvc3ZnPg==";

    // Patterns for matching image URLs
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BACKGROUND_IMAGE_PATTERN = Pattern.compile("background-image:\\s*url\\([\"']?([^\"')]+)[\"']?\\)", Pattern.CASE_INSENSITIVE);

    private WebView webView;
    private boolean enabled = true;

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return PLUGIN_DESCRIPTION;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public void initialize(WebView webView) {
        // Store the WebView reference for potential future use
        // Currently not directly used but kept for plugin API consistency
        this.webView = webView;
        LOGGER.info("Initialized ImageBlockerPlugin");
    }

    @Override
    public String processContent(String content, EmailDetails emailDetails) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String processedContent = content;

        try {
            // Process <img> tags
            processedContent = processImgTags(processedContent);

            // Process CSS background images
            processedContent = processBackgroundImages(processedContent);

            LOGGER.fine("Processed HTML content for image blocking");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing HTML content for image blocking", e);
        }

        return processedContent;
    }

    @Override
    public void onEmailLoaded(EmailDetails emailDetails) {
        // No action needed after email is loaded
    }

    @Override
    public void cleanup() {
        webView = null;
        LOGGER.info("Cleaned up ImageBlockerPlugin");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("ImageBlockerPlugin " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Processes all img tags in the content to replace external image sources with placeholders.
     *
     * @param content The HTML content to process
     * @return The processed HTML content
     */
    private String processImgTags(String content) {
        StringBuilder result = new StringBuilder(content);
        Matcher matcher = IMG_TAG_PATTERN.matcher(content);
        int offset = 0;

        while (matcher.find()) {
            String imgTag = matcher.group(0);
            String srcUrl = matcher.group(1);

            // Skip data URLs (they're already embedded)
            if (srcUrl.startsWith("data:")) {
                continue;
            }

            // Create new img tag with placeholder
            String newImgTag = imgTag.replace(srcUrl, PLACEHOLDER_IMAGE);

            // Replace in result
            result.replace(
                matcher.start(0) + offset,
                matcher.end(0) + offset,
                newImgTag
            );

            // Adjust offset for subsequent replacements
            offset += (newImgTag.length() - imgTag.length());
        }

        return result.toString();
    }

    /**
     * Processes CSS background images to replace external URLs with placeholders.
     *
     * @param content The HTML content to process
     * @return The processed HTML content
     */
    private String processBackgroundImages(String content) {
        StringBuilder result = new StringBuilder(content);
        Matcher matcher = BACKGROUND_IMAGE_PATTERN.matcher(content);
        int offset = 0;

        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String imageUrl = matcher.group(1);

            // Skip data URLs (they're already embedded)
            if (imageUrl.startsWith("data:")) {
                continue;
            }

            // Create new CSS with placeholder
            String newCss = "background-image: url('" + PLACEHOLDER_IMAGE + "')";

            // Replace in result
            result.replace(
                matcher.start(0) + offset,
                matcher.end(0) + offset,
                newCss
            );

            // Adjust offset for subsequent replacements
            offset += (newCss.length() - fullMatch.length());
        }

        return result.toString();
    }
}
