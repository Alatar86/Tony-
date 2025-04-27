package com.privacyemail.api;

import javafx.scene.web.WebView;

/**
 * Interface for email content processing plugins.
 * <p>
 * Plugins extend the email viewing functionality by providing
 * custom processing of email content and enhancing WebView
 * configurations for email display.
 */
public interface EmailPlugin {

    /**
     * Initialize the plugin and allocate necessary resources.
     * This method is called once when the plugin is registered.
     *
     * @throws Exception if initialization fails
     */
    void initialize() throws Exception;

    /**
     * Get the unique identifier for this plugin.
     * The ID should be unique across all plugins.
     *
     * @return the plugin's unique identifier
     */
    String getId();

    /**
     * Get the display name of the plugin.
     * This name may be shown to users in UI components.
     *
     * @return the human-readable name of the plugin
     */
    String getName();

    /**
     * Get a description of the plugin's functionality.
     *
     * @return a brief description of what the plugin does
     */
    String getDescription();

    /**
     * Process email content before displaying it.
     * This method is called for each email being displayed
     * and allows plugins to modify the content.
     *
     * @param htmlContent the original HTML content of the email
     * @param emailId the unique identifier of the email being processed
     * @return the processed HTML content
     */
    String processEmailContent(String htmlContent, String emailId);

    /**
     * Configure the WebView component used to display emails.
     * This method allows plugins to modify WebView settings,
     * add event handlers, or adjust WebEngine properties.
     *
     * @param webView the WebView component to configure
     * @param emailId the unique identifier of the email being displayed
     */
    void configureWebView(WebView webView, String emailId);

    /**
     * Clean up resources when the plugin is being unregistered or when
     * the application is shutting down.
     */
    void cleanup();
}
