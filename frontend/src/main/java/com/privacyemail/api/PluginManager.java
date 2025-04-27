package com.privacyemail.api;

import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manager class for email content processing plugins.
 * This class provides methods to:
 * 1. Discover and load plugins using Java's ServiceLoader
 * 2. Initialize plugins and manage their lifecycle
 * 3. Apply plugins to process email content
 * 4. Configure WebView instances with all active plugins
 */
public class PluginManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    private static PluginManager instance;

    private final PluginRegistry registry;
    private final Map<String, Boolean> enabledPlugins = new ConcurrentHashMap<>();

    /**
     * Private constructor for singleton pattern
     */
    private PluginManager() {
        registry = PluginRegistry.getInstance();
    }

    /**
     * Get the singleton instance of the PluginManager
     * @return the PluginManager instance
     */
    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    /**
     * Discover and register all available plugins using ServiceLoader
     */
    public void discoverPlugins() {
        logger.info("Discovering email content plugins...");

        ServiceLoader<EmailPlugin> loader = ServiceLoader.load(EmailPlugin.class);
        int count = 0;

        for (EmailPlugin plugin : loader) {
            if (registry.registerPlugin(plugin)) {
                count++;
                // Enable plugin by default
                enabledPlugins.put(plugin.getId(), true);
            }
        }

        logger.info("Discovered {} plugins", count);
    }

    /**
     * Initialize all discovered plugins
     */
    public void initializePlugins() {
        logger.info("Initializing plugins...");

        // First discover plugins, just like HtmlContentHandler did
        discoverPlugins();

        for (EmailPlugin plugin : registry.getAllPlugins().values()) {
            try {
                if (!registry.isPluginInitialized(plugin.getId())) {
                    registry.initializePlugin(plugin.getId());
                }
            } catch (Exception e) {
                logger.error("Error initializing plugin: {}", plugin.getId(), e);
            }
        }
    }

    /**
     * Register a plugin manually
     * @param plugin the plugin to register
     * @return true if registration was successful
     */
    public boolean registerPlugin(EmailPlugin plugin) {
        boolean registered = registry.registerPlugin(plugin);
        if (registered) {
            enabledPlugins.put(plugin.getId(), true);
        }
        return registered;
    }

    /**
     * Enable or disable a plugin
     * @param pluginId the ID of the plugin
     * @param enabled true to enable, false to disable
     */
    public void setPluginEnabled(String pluginId, boolean enabled) {
        if (registry.getPlugin(pluginId) != null) {
            enabledPlugins.put(pluginId, enabled);
            logger.info("Plugin {} is now {}", pluginId, enabled ? "enabled" : "disabled");
        } else {
            logger.warn("Cannot change state for unknown plugin: {}", pluginId);
        }
    }

    /**
     * Check if a plugin is enabled
     * @param pluginId the ID of the plugin
     * @return true if the plugin is enabled
     */
    public boolean isPluginEnabled(String pluginId) {
        return enabledPlugins.getOrDefault(pluginId, false);
    }

    /**
     * Process email content through all enabled plugins
     * @param htmlContent the original HTML content
     * @param emailId the ID of the email being processed
     * @return the processed HTML content
     */
    public String processEmailContent(String htmlContent, String emailId) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }

        String processedContent = htmlContent;

        for (Map.Entry<String, EmailPlugin> entry : registry.getAllPlugins().entrySet()) {
            String pluginId = entry.getKey();

            // Skip disabled plugins
            if (!isPluginEnabled(pluginId)) {
                logger.debug("Skipping disabled plugin: {}", pluginId);
                continue;
            }

            EmailPlugin plugin = entry.getValue();
            try {
                processedContent = plugin.processEmailContent(processedContent, emailId);
                logger.debug("Applied plugin {} to email {}", pluginId, emailId);
            } catch (Exception e) {
                logger.error("Error applying plugin {} to email {}", pluginId, emailId, e);
            }
        }

        return processedContent;
    }

    /**
     * Configure a WebView with all enabled plugins
     * @param webView the WebView to configure
     * @param emailId the ID of the email being displayed
     */
    public void configureWebView(WebView webView, String emailId) {
        if (webView == null) {
            logger.warn("Cannot configure null WebView");
            return;
        }

        for (Map.Entry<String, EmailPlugin> entry : registry.getAllPlugins().entrySet()) {
            String pluginId = entry.getKey();

            // Skip disabled plugins
            if (!isPluginEnabled(pluginId)) {
                logger.debug("Skipping disabled plugin configuration: {}", pluginId);
                continue;
            }

            EmailPlugin plugin = entry.getValue();
            try {
                plugin.configureWebView(webView, emailId);
                logger.debug("Plugin {} configured WebView for email {}", pluginId, emailId);
            } catch (Exception e) {
                logger.error("Error configuring WebView with plugin {} for email {}", pluginId, emailId, e);
            }
        }
    }

    /**
     * Get a list of all enabled plugins
     * @return list of enabled plugin IDs
     */
    public List<String> getEnabledPlugins() {
        java.util.List<String> result = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : enabledPlugins.entrySet()) {
            if (entry.getValue()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Shutdown all plugins and cleanup resources
     */
    public void shutdownPlugins() {
        logger.info("Shutting down plugins...");

        for (EmailPlugin plugin : registry.getAllPlugins().values()) {
            try {
                plugin.cleanup();
                logger.debug("Plugin {} shutdown successfully", plugin.getId());
            } catch (Exception e) {
                logger.error("Error shutting down plugin: {}", plugin.getId(), e);
            }
        }
    }
}
