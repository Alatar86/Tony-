package com.privacyemail.util.plugin;

import com.privacyemail.models.EmailDetails;
import javafx.scene.web.WebView;

/**
 * Interface for plugins that modify or enhance the email viewing experience.
 * Plugins can process email content, interact with the WebView, and respond to email loading events.
 */
public interface EmailViewPlugin {

    /**
     * Returns the unique identifier for this plugin.
     *
     * @return The plugin ID
     */
    String getPluginId();

    /**
     * Returns the display name of this plugin.
     *
     * @return The plugin display name
     */
    String getDisplayName();

    /**
     * Returns the description of this plugin.
     *
     * @return The plugin description
     */
    String getDescription();

    /**
     * Returns the version of this plugin.
     *
     * @return The plugin version
     */
    String getVersion();

    /**
     * Initializes the plugin with the WebView used for displaying emails.
     * This method is called when the plugin is registered, if the WebView is already available,
     * or when the WebView becomes available.
     *
     * @param webView The WebView used to display emails
     */
    void initialize(WebView webView);

    /**
     * Processes the email content.
     * This method is called before the content is loaded into the WebView.
     *
     * @param content The original email content
     * @param emailDetails The email details
     * @return The processed content
     */
    String processContent(String content, EmailDetails emailDetails);

    /**
     * Called when an email is loaded into the WebView.
     * This method is called after the content is loaded and rendered.
     *
     * @param emailDetails The email details
     */
    void onEmailLoaded(EmailDetails emailDetails);

    /**
     * Cleans up resources used by the plugin.
     * This method is called when the plugin is unregistered or when the application is closing.
     */
    void cleanup();

    /**
     * Checks if the plugin is enabled.
     *
     * @return true if the plugin is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Sets the enabled state of the plugin.
     *
     * @param enabled true to enable the plugin, false to disable
     */
    void setEnabled(boolean enabled);
}
