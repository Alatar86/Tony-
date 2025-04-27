package com.privacyemail.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing email content processing plugins.
 * <p>
 * This class follows the singleton pattern to ensure a single instance
 * is used throughout the application. It handles plugin registration,
 * initialization, and retrieval.
 */
public class PluginRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRegistry.class);
    private static PluginRegistry instance;

    // Thread-safe map to store plugins by their ID
    private final Map<String, EmailPlugin> plugins = new ConcurrentHashMap<>();

    // Set to track which plugins have been initialized
    private final Set<String> initializedPlugins = new HashSet<>();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private PluginRegistry() {
        LOGGER.info("PluginRegistry initialized");
    }

    /**
     * Get the singleton instance of the PluginRegistry.
     *
     * @return the singleton PluginRegistry instance
     */
    public static synchronized PluginRegistry getInstance() {
        if (instance == null) {
            instance = new PluginRegistry();
        }
        return instance;
    }

    /**
     * Register a plugin with the registry.
     *
     * @param plugin the plugin to register
     * @return true if the plugin was registered successfully, false otherwise
     */
    public synchronized boolean registerPlugin(EmailPlugin plugin) {
        if (plugin == null) {
            LOGGER.warn("Attempted to register a null plugin");
            return false;
        }

        String pluginId = plugin.getId();
        if (pluginId == null || pluginId.isEmpty()) {
            LOGGER.warn("Plugin has null or empty ID");
            return false;
        }

        if (plugins.containsKey(pluginId)) {
            LOGGER.warn("Plugin with ID '" + pluginId + "' is already registered");
            return false;
        }

        plugins.put(pluginId, plugin);
        LOGGER.info("Registered plugin: " + plugin.getName() + " (ID: " + pluginId + ")");
        return true;
    }

    /**
     * Unregister a plugin from the registry.
     * This will also call the plugin's cleanup method.
     *
     * @param pluginId the ID of the plugin to unregister
     * @return true if the plugin was unregistered successfully, false otherwise
     */
    public synchronized boolean unregisterPlugin(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            LOGGER.warn("Attempted to unregister a plugin with null or empty ID");
            return false;
        }

        EmailPlugin plugin = plugins.remove(pluginId);
        if (plugin == null) {
            LOGGER.warn("No plugin found with ID: " + pluginId);
            return false;
        }

        // Call cleanup on the plugin
        try {
            plugin.cleanup();
            LOGGER.info("Unregistered and cleaned up plugin: " + plugin.getName() + " (ID: " + pluginId + ")");
        } catch (Exception e) {
            LOGGER.warn("Error during plugin cleanup: " + pluginId, e);
        }

        // Remove from initialized set if it was initialized
        synchronized (initializedPlugins) {
            initializedPlugins.remove(pluginId);
        }

        return true;
    }

    /**
     * Initialize a plugin that has been registered.
     * This method calls the plugin's initialize method.
     *
     * @param pluginId the ID of the plugin to initialize
     * @return true if the plugin was initialized successfully, false otherwise
     */
    public boolean initializePlugin(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            LOGGER.warn("Attempted to initialize a plugin with null or empty ID");
            return false;
        }

        EmailPlugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            LOGGER.warn("Cannot initialize: No plugin found with ID: " + pluginId);
            return false;
        }

        synchronized (initializedPlugins) {
            if (initializedPlugins.contains(pluginId)) {
                LOGGER.info("Plugin already initialized: " + pluginId);
                return true;
            }

            try {
                plugin.initialize();
                initializedPlugins.add(pluginId);
                LOGGER.info("Initialized plugin: " + plugin.getName() + " (ID: " + pluginId + ")");
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to initialize plugin: " + pluginId, e);
                return false;
            }
        }
    }

    /**
     * Check if a plugin has been initialized.
     *
     * @param pluginId the ID of the plugin to check
     * @return true if the plugin is initialized, false otherwise
     */
    public boolean isPluginInitialized(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return false;
        }

        synchronized (initializedPlugins) {
            return initializedPlugins.contains(pluginId);
        }
    }

    /**
     * Get a plugin by its ID.
     *
     * @param pluginId the ID of the plugin to retrieve
     * @return the plugin, or null if no plugin with the given ID is registered
     */
    public EmailPlugin getPlugin(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            LOGGER.warn("Attempted to get a plugin with null or empty ID");
            return null;
        }

        return plugins.get(pluginId);
    }

    /**
     * Get all registered plugins.
     *
     * @return a map of plugin IDs to plugin instances
     */
    public Map<String, EmailPlugin> getAllPlugins() {
        return new ConcurrentHashMap<>(plugins);
    }

    /**
     * Get the number of registered plugins.
     *
     * @return the number of plugins in the registry
     */
    public int getPluginCount() {
        return plugins.size();
    }
}
