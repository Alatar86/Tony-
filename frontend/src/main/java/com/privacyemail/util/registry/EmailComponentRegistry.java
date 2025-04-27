package com.privacyemail.util.registry;

import com.privacyemail.util.interfaces.EmailRenderer;
import com.privacyemail.util.interfaces.WebViewConfigurer;
import com.privacyemail.util.interfaces.ThemeProvider;
import com.privacyemail.util.interfaces.HtmlSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for email-related components.
 * This allows for runtime registration and discovery of component implementations.
 */
public class EmailComponentRegistry {

    private static final Logger logger = LoggerFactory.getLogger(EmailComponentRegistry.class);

    // Component type maps
    private static final Map<String, EmailRenderer> renderers = new ConcurrentHashMap<>();
    private static final Map<String, WebViewConfigurer> configurers = new ConcurrentHashMap<>();
    private static final Map<String, ThemeProvider> themeProviders = new ConcurrentHashMap<>();
    private static final Map<String, HtmlSanitizer> sanitizers = new ConcurrentHashMap<>();

    // Default component identifiers
    private static final String DEFAULT_RENDERER = "default";
    private static final String DEFAULT_CONFIGURER = "default";
    private static final String DEFAULT_THEME_PROVIDER = "default";
    private static final String DEFAULT_SANITIZER = "default";

    private EmailComponentRegistry() {
        // Private constructor to prevent instantiation
    }

    // Email Renderer registry methods

    /**
     * Registers an EmailRenderer implementation.
     *
     * @param name Unique identifier for this renderer
     * @param renderer The renderer implementation
     */
    public static void registerRenderer(String name, EmailRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("Cannot register null renderer");
        }
        renderers.put(name, renderer);
        logger.debug("Registered EmailRenderer: {}", name);
    }

    /**
     * Retrieves an EmailRenderer by name.
     *
     * @param name The name of the renderer to retrieve
     * @return The requested renderer, or the default if not found
     */
    public static EmailRenderer getRenderer(String name) {
        EmailRenderer renderer = renderers.get(name);
        if (renderer == null) {
            logger.warn("EmailRenderer '{}' not found, using default", name);
            renderer = renderers.get(DEFAULT_RENDERER);
            if (renderer == null) {
                throw new IllegalStateException("No default EmailRenderer registered");
            }
        }
        return renderer;
    }

    /**
     * Gets the default EmailRenderer.
     *
     * @return The default renderer
     */
    public static EmailRenderer getDefaultRenderer() {
        return getRenderer(DEFAULT_RENDERER);
    }

    // WebViewConfigurer registry methods

    /**
     * Registers a WebViewConfigurer implementation.
     *
     * @param name Unique identifier for this configurer
     * @param configurer The configurer implementation
     */
    public static void registerConfigurer(String name, WebViewConfigurer configurer) {
        if (configurer == null) {
            throw new IllegalArgumentException("Cannot register null configurer");
        }
        configurers.put(name, configurer);
        logger.debug("Registered WebViewConfigurer: {}", name);
    }

    /**
     * Retrieves a WebViewConfigurer by name.
     *
     * @param name The name of the configurer to retrieve
     * @return The requested configurer, or the default if not found
     */
    public static WebViewConfigurer getConfigurer(String name) {
        WebViewConfigurer configurer = configurers.get(name);
        if (configurer == null) {
            logger.warn("WebViewConfigurer '{}' not found, using default", name);
            configurer = configurers.get(DEFAULT_CONFIGURER);
            if (configurer == null) {
                throw new IllegalStateException("No default WebViewConfigurer registered");
            }
        }
        return configurer;
    }

    /**
     * Gets the default WebViewConfigurer.
     *
     * @return The default configurer
     */
    public static WebViewConfigurer getDefaultConfigurer() {
        return getConfigurer(DEFAULT_CONFIGURER);
    }

    // ThemeProvider registry methods

    /**
     * Registers a ThemeProvider implementation.
     *
     * @param name Unique identifier for this theme provider
     * @param provider The theme provider implementation
     */
    public static void registerThemeProvider(String name, ThemeProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Cannot register null theme provider");
        }
        themeProviders.put(name, provider);
        logger.debug("Registered ThemeProvider: {}", name);
    }

    /**
     * Retrieves a ThemeProvider by name.
     *
     * @param name The name of the theme provider to retrieve
     * @return The requested theme provider, or the default if not found
     */
    public static ThemeProvider getThemeProvider(String name) {
        ThemeProvider provider = themeProviders.get(name);
        if (provider == null) {
            logger.warn("ThemeProvider '{}' not found, using default", name);
            provider = themeProviders.get(DEFAULT_THEME_PROVIDER);
            if (provider == null) {
                throw new IllegalStateException("No default ThemeProvider registered");
            }
        }
        return provider;
    }

    /**
     * Gets the default ThemeProvider.
     *
     * @return The default theme provider
     */
    public static ThemeProvider getDefaultThemeProvider() {
        return getThemeProvider(DEFAULT_THEME_PROVIDER);
    }

    // HtmlSanitizer registry methods

    /**
     * Registers a HtmlSanitizer implementation.
     *
     * @param name Unique identifier for this sanitizer
     * @param sanitizer The sanitizer implementation
     */
    public static void registerSanitizer(String name, HtmlSanitizer sanitizer) {
        if (sanitizer == null) {
            throw new IllegalArgumentException("Cannot register null sanitizer");
        }
        sanitizers.put(name, sanitizer);
        logger.debug("Registered HtmlSanitizer: {}", name);
    }

    /**
     * Retrieves a HtmlSanitizer by name.
     *
     * @param name The name of the sanitizer to retrieve
     * @return The requested sanitizer, or the default if not found
     */
    public static HtmlSanitizer getSanitizer(String name) {
        HtmlSanitizer sanitizer = sanitizers.get(name);
        if (sanitizer == null) {
            logger.warn("HtmlSanitizer '{}' not found, using default", name);
            sanitizer = sanitizers.get(DEFAULT_SANITIZER);
            if (sanitizer == null) {
                throw new IllegalStateException("No default HtmlSanitizer registered");
            }
        }
        return sanitizer;
    }

    /**
     * Gets the default HtmlSanitizer.
     *
     * @return The default sanitizer
     */
    public static HtmlSanitizer getDefaultSanitizer() {
        return getSanitizer(DEFAULT_SANITIZER);
    }
}
