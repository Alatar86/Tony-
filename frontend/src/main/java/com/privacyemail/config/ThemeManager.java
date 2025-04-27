package com.privacyemail.config;

import java.net.URL;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Parent;

/**
 * Manages application themes, providing methods to apply themes to JavaFX scenes.
 * Includes support for system theme detection where available.
 */
public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    private static final String APPLICATION_CSS = "/com/privacyemail/css/application.css";
    private static final String DARK_THEME_CSS = "/com/privacyemail/css/dark-theme.css";
    private static final String LIGHT_THEME_CSS = "/com/privacyemail/css/light-theme.css";
    private static final String WEBVIEW_DARK_CSS = "/com/privacyemail/css/webview-dark.css";

    // Windows registry key for app theme (dark/light mode)
    private static final String WINDOWS_THEME_REGISTRY = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final String WINDOWS_THEME_VALUE = "AppsUseLightTheme";

    // Track all application scenes to update when theme changes
    private static final List<Scene> managedScenes = new CopyOnWriteArrayList<>();

    // WebView URLs for user stylesheets
    private static URL webviewDarkCssUrl;

    static {
        try {
            // Initialize static URL references
            webviewDarkCssUrl = ThemeManager.class.getResource(WEBVIEW_DARK_CSS);
            if (webviewDarkCssUrl == null) {
                logger.warn("WebView dark CSS not found at path: {}", WEBVIEW_DARK_CSS);
            } else {
                logger.info("Successfully loaded WebView dark CSS at: {}", webviewDarkCssUrl);
            }
        } catch (Exception e) {
            logger.error("Error loading WebView CSS resources", e);
        }
    }

    private ThemeManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Register a scene with the ThemeManager to receive theme updates
     *
     * @param scene The scene to register
     */
    public static void registerScene(Scene scene) {
        if (scene != null && !managedScenes.contains(scene)) {
            managedScenes.add(scene);
            logger.debug("Scene registered with ThemeManager: {}", scene);
        }
    }

    /**
     * Unregister a scene from the ThemeManager when it's no longer needed
     *
     * @param scene The scene to unregister
     */
    public static void unregisterScene(Scene scene) {
        if (scene != null) {
            managedScenes.remove(scene);
            logger.debug("Scene unregistered from ThemeManager: {}", scene);
        }
    }

    /**
     * Apply the current theme from user preferences to all registered scenes
     */
    public static void applyThemeToAllScenes() {
        FrontendPreferences preferences = FrontendPreferences.getInstance();
        String themePreference = preferences.getThemePreference();

        logger.info("Applying theme {} to all scenes (count: {})", themePreference, managedScenes.size());

        // Apply to all managed scenes
        for (Scene scene : managedScenes) {
            try {
                applyTheme(scene, true); // Always reset stylesheets for consistency
            } catch (Exception e) {
                logger.error("Error applying theme to scene: {}", e.getMessage());
            }
        }
    }

    /**
     * Apply the current theme from user preferences to a scene
     *
     * @param scene The JavaFX scene to apply the theme to
     * @param resetStylesheets Whether to clear existing stylesheets before applying
     */
    public static void applyTheme(Scene scene, boolean resetStylesheets) {
        if (scene == null) {
            logger.warn("Cannot apply theme to null scene");
            return;
        }

        try {
            FrontendPreferences preferences = FrontendPreferences.getInstance();
            String themePreference = preferences.getThemePreference();
            String themeCssPath;

            // Determine which theme to use
            if (FrontendPreferences.THEME_SYSTEM.equals(themePreference)) {
                boolean isDarkMode = isSystemInDarkMode();
                themeCssPath = isDarkMode ? DARK_THEME_CSS : LIGHT_THEME_CSS;
                logger.debug("System theme detected as: {}", isDarkMode ? "dark" : "light");
            } else if (FrontendPreferences.THEME_DARK.equals(themePreference)) {
                themeCssPath = DARK_THEME_CSS;
            } else {
                // Default to light theme
                themeCssPath = LIGHT_THEME_CSS;
            }

            // Apply theme background color directly to scene and root to prevent white flash
            boolean isDarkTheme = DARK_THEME_CSS.equals(themeCssPath);
            final String backgroundColor = isDarkTheme ? "#2b2b2b" : "#ffffff";

            // Apply stylesheets on the JavaFX thread
            Platform.runLater(() -> {
                try {
                    // Set background color directly on scene and root
                    Parent root = scene.getRoot();
                    if (root != null) {
                        root.setStyle("-fx-background-color: " + backgroundColor + ";");
                    }

                    applyStylesheets(scene, themeCssPath, resetStylesheets);

                    // Auto-register this scene for future theme updates if it's not already registered
                    if (!managedScenes.contains(scene)) {
                        registerScene(scene);
                    }
                } catch (Exception e) {
                    logger.error("Error applying theme stylesheets", e);
                }
            });
        } catch (Exception e) {
            logger.error("Error determining theme preference", e);
        }
    }

    /**
     * Apply the specified stylesheets to a scene
     *
     * @param scene The scene to apply stylesheets to
     * @param themeCssPath The path to the theme CSS resource
     * @param resetStylesheets Whether to clear existing stylesheets
     */
    private static void applyStylesheets(Scene scene, String themeCssPath, boolean resetStylesheets) {
        if (resetStylesheets) {
            scene.getStylesheets().clear();

            // Always add the base application CSS first
            URL appCssUrl = ThemeManager.class.getResource(APPLICATION_CSS);
            if (appCssUrl == null) {
                throw new RuntimeException("Application CSS resource not found: " + APPLICATION_CSS);
            }
            scene.getStylesheets().add(appCssUrl.toExternalForm());
        } else {
            // Remove any existing theme stylesheets
            scene.getStylesheets().removeIf(css ->
                css.contains("dark-theme.css") || css.contains("light-theme.css"));
        }

        // Add the theme CSS
        URL themeCssUrl = ThemeManager.class.getResource(themeCssPath);
        if (themeCssUrl == null) {
            throw new RuntimeException("Theme CSS resource not found: " + themeCssPath);
        }
        scene.getStylesheets().add(themeCssUrl.toExternalForm());

        // If using dark theme, add the WebView-specific CSS
        if (DARK_THEME_CSS.equals(themeCssPath)) {
            URL webviewCssUrl = ThemeManager.class.getResource(WEBVIEW_DARK_CSS);
            if (webviewCssUrl != null) {
                // Add WebView CSS if not already added
                String webviewCssExternal = webviewCssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(webviewCssExternal)) {
                    scene.getStylesheets().add(webviewCssExternal);
                }
            }
        }

        logger.debug("Applied theme: {} to scene: {}", themeCssPath, scene);
    }

    /**
     * Switch the theme preference and apply to all application windows
     *
     * @param isDarkTheme Whether to use dark theme (true) or light theme (false)
     */
    public static void switchTheme(boolean isDarkTheme) {
        try {
            FrontendPreferences preferences = FrontendPreferences.getInstance();

            // Update preference
            preferences.setThemePreference(isDarkTheme ?
                FrontendPreferences.THEME_DARK :
                FrontendPreferences.THEME_LIGHT);

            // Save the preference
            preferences.savePreferences();

            // Apply to all scenes
            applyThemeToAllScenes();

            logger.info("Theme switched to: {}", isDarkTheme ? "Dark" : "Light");
        } catch (Exception e) {
            logger.error("Error switching theme", e);
        }
    }

    /**
     * Detects if the system is currently using dark mode
     *
     * @return true if system is in dark mode, false otherwise
     */
    public static boolean isSystemInDarkMode() {
        String osName = System.getProperty("os.name").toLowerCase();

        try {
            // Windows detection through registry
            if (osName.contains("win")) {
                Preferences prefs = Preferences.userRoot().node(WINDOWS_THEME_REGISTRY);
                int lightThemeValue = prefs.getInt(WINDOWS_THEME_VALUE, 1);
                return lightThemeValue == 0; // 0 means dark mode
            }

            // macOS detection (approximate/placeholder - would need JNI for actual implementation)
            else if (osName.contains("mac")) {
                // This is a placeholder. Real implementation would use NSAppearance through JNI
                // For now, we'll check the system property which some JVM implementations set
                String darkModeProperty = System.getProperty("apple.awt.application.appearance");
                return "darkAqua".equals(darkModeProperty);
            }

            // Linux/GTK detection (approximate/placeholder)
            else if (osName.contains("linux") || osName.contains("unix")) {
                // This is a simplified approach. Real implementation would check GTK theme
                // through desktop environment variables or gsettings
                // For example: gsettings get org.gnome.desktop.interface gtk-theme
                // If it contains "dark", "black", etc., it's likely dark mode

                // For simplicity, default to light mode on Linux/Unix systems
                return false;
            }
        } catch (Exception e) {
            logger.warn("Error detecting system theme: {}", e.getMessage());
        }

        // Default to light mode if we can't detect
        return false;
    }

    /**
     * Registers a listener for system theme changes (where supported)
     * This method is experimental and may not work on all platforms.
     *
     * @param scene The scene to update when system theme changes
     * @return true if registration was successful, false otherwise
     */
    public static boolean registerForSystemThemeChanges(Scene scene) {
        // This is a placeholder for future implementation
        // Proper implementation would require platform-specific native code to listen for:
        // - Windows: Registry change notifications
        // - macOS: NSDistributedNotificationCenter notifications
        // - Linux: dconf/gsettings change notifications

        logger.info("System theme change detection not fully implemented yet");
        return false;
    }
}
