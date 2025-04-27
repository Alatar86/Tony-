package com.privacyemail.util.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.privacyemail.models.EmailDetails;
import javafx.scene.web.WebView;

/**
 * Manages email view plugins, handling registration, lifecycle, and execution.
 */
public class PluginManager {
    private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());
    private static PluginManager instance;

    private final Map<String, EmailViewPlugin> plugins;
    private WebView webView;

    /**
     * Gets the singleton instance of the PluginManager.
     *
     * @return The PluginManager instance
     */
    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    private PluginManager() {
        plugins = new HashMap<>();
    }

    /**
     * Sets the WebView that plugins will interact with.
     * This will initialize any registered plugins that haven't been initialized yet.
     *
     * @param webView The WebView used for email display
     */
    public void setWebView(WebView webView) {
        this.webView = webView;

        // Initialize all registered plugins with the WebView
        for (EmailViewPlugin plugin : plugins.values()) {
            try {
                plugin.initialize(webView);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize plugin: " + plugin.getPluginId(), e);
            }
        }
    }

    /**
     * Registers a plugin with the manager.
     * If the plugin ID already exists, the existing plugin will be replaced.
     *
     * @param plugin The plugin to register
     */
    public void registerPlugin(EmailViewPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        String pluginId = plugin.getPluginId();
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin ID cannot be null or empty");
        }

        plugins.put(pluginId, plugin);
        LOGGER.info("Registered plugin: " + pluginId);

        // Initialize the plugin if we already have a WebView
        if (webView != null) {
            try {
                plugin.initialize(webView);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize plugin: " + pluginId, e);
            }
        }
    }

    /**
     * Unregisters a plugin from the manager.
     *
     * @param pluginId The ID of the plugin to unregister
     * @return true if the plugin was unregistered, false if it wasn't registered
     */
    public boolean unregisterPlugin(String pluginId) {
        EmailViewPlugin plugin = plugins.remove(pluginId);
        if (plugin != null) {
            try {
                plugin.cleanup();
                LOGGER.info("Unregistered plugin: " + pluginId);
                return true;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during plugin cleanup: " + pluginId, e);
            }
        }
        return false;
    }

    /**
     * Gets a plugin by its ID.
     *
     * @param pluginId The ID of the plugin to get
     * @return The plugin, or null if no plugin with the given ID is registered
     */
    public EmailViewPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    /**
     * Gets all registered plugins.
     *
     * @return An unmodifiable list of all registered plugins
     */
    public List<EmailViewPlugin> getAllPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(plugins.values()));
    }

    /**
     * Processes email content through all enabled plugins.
     *
     * @param content The original email content
     * @param emailDetails The email details
     * @return The processed content
     */
    public String processContent(String content, EmailDetails emailDetails) {
        String processedContent = content;

        for (EmailViewPlugin plugin : plugins.values()) {
            if (plugin.isEnabled()) {
                try {
                    processedContent = plugin.processContent(processedContent, emailDetails);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing content with plugin: " + plugin.getPluginId(), e);
                }
            }
        }

        return processedContent;
    }

    /**
     * Notifies all enabled plugins that an email has been loaded.
     *
     * @param emailDetails The email details
     */
    public void notifyEmailLoaded(EmailDetails emailDetails) {
        for (EmailViewPlugin plugin : plugins.values()) {
            if (plugin.isEnabled()) {
                try {
                    plugin.onEmailLoaded(emailDetails);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error notifying plugin of email load: " + plugin.getPluginId(), e);
                }
            }
        }
    }

    /**
     * Cleans up all registered plugins and clears the registry.
     * This should be called when the application is shutting down.
     */
    public void cleanup() {
        for (EmailViewPlugin plugin : plugins.values()) {
            try {
                plugin.cleanup();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during plugin cleanup: " + plugin.getPluginId(), e);
            }
        }

        plugins.clear();
        webView = null;
    }
}
