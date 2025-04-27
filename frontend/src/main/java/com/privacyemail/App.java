package com.privacyemail;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.privacyemail.config.AppModule;
import com.privacyemail.config.Configuration;
import com.privacyemail.config.ThemeManager;
import com.privacyemail.config.FrontendPreferences;

/**
 * Main application class for the Privacy-Focused Email Agent frontend.
 */
public class App extends Application {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static Scene mainScene; // Store a reference to the main scene

    /**
     * Get the main application scene
     * @return The main application scene
     */
    public static Scene getMainScene() {
        return mainScene;
    }

    /**
     * Start method called by the JavaFX runtime to initialize and display the application UI.
     *
     * This method handles:
     * 1. Application dependency initialization through Guice, which centralizes
     *    the creation and wiring of all core services.
     *
     * 2. UI components are now handled through direct controller initialization
     *    in the MainWindowController's initialize() method.
     *
     * @param primaryStage The primary stage provided by the JavaFX runtime
     * @throws IOException If an error occurs during FXML loading
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            logger.info("Starting Privacy-Focused Email Agent Application");

            // Force dark theme for WebView - these properties override content styling
            System.setProperty("javafx.web.page.background", "#2b2b2b");
            System.setProperty("javafx.web.document.background", "#2b2b2b");
            System.setProperty("javafx.web.document.foreground", "#bbbbbb");
            System.setProperty("javafx.web.view.document.background", "#2b2b2b");

            // Set WebView user agent for consistent rendering
            System.setProperty("javafx.web.useragent", "Privacy-Focused Email Agent");

            // Initialize Guice injector for dependency injection
            Injector injector = Guice.createInjector(AppModule.create());

            // Get Configuration and FrontendPreferences directly from Guice
            Configuration configuration = injector.getInstance(Configuration.class);
            FrontendPreferences frontendPreferences = injector.getInstance(FrontendPreferences.class);

            // Load window configuration
            int windowWidth = configuration.getIntProperty("ui.windowWidth", 1200);
            int windowHeight = configuration.getIntProperty("ui.windowHeight", 800);
            String appTitle = configuration.getProperty("ui.title", "Privacy-Focused Email Agent");

            // Set theme preference to dark or system if not already set
            // This ensures we don't start with a white background
            if (frontendPreferences.getThemePreference().equals(FrontendPreferences.THEME_LIGHT)) {
                boolean systemInDarkMode = ThemeManager.isSystemInDarkMode();
                if (systemInDarkMode) {
                    frontendPreferences.setThemePreference(FrontendPreferences.THEME_SYSTEM);
                    frontendPreferences.savePreferences();
                    logger.info("Updated theme preference to system (dark mode detected)");
                }
            }

            // Load the main window FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/privacyemail/views/MainWindow.fxml"));

            // Set standard controller factory to use Guice for all dependencies
            loader.setControllerFactory(injector::getInstance);

            // Load the FXML file
            Parent root = loader.load();

            // Create the scene
            mainScene = new Scene(root, windowWidth, windowHeight);

            // Register the main scene with ThemeManager
            ThemeManager.registerScene(mainScene);

            // Determine initial theme based on preferences
            String themePref = frontendPreferences.getThemePreference();
            boolean initialDarkMode;

            if (FrontendPreferences.THEME_SYSTEM.equals(themePref)) {
                initialDarkMode = ThemeManager.isSystemInDarkMode();
                logger.info("Applying initial theme based on system setting (dark={})", initialDarkMode);
            } else {
                initialDarkMode = FrontendPreferences.THEME_DARK.equals(themePref);
                logger.info("Applying initial theme based on preference: {}", themePref);
            }

            // If not using system theme, set the theme preference explicitly
            if (!FrontendPreferences.THEME_SYSTEM.equals(themePref)) {
                // This will update the preference and apply to all scenes
                ThemeManager.switchTheme(initialDarkMode);
            } else {
                // For system theme, just apply to the current scene
                ThemeManager.applyTheme(mainScene, true);
            }

            // Configure and show the stage
            primaryStage.setTitle(appTitle);
            primaryStage.setScene(mainScene);
            primaryStage.show();

            // Save window size on close
            primaryStage.setOnCloseRequest(e -> {
                // We would typically save window size here for the next launch
                logger.info("Application closing, window size: {}x{}",
                    (int) primaryStage.getWidth(), (int) primaryStage.getHeight());
            });

            logger.info("Application UI initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Application stopping - performing cleanup");
        // ApplicationInitializer.shutdown() will be called automatically by the JVM shutdown hook
        super.stop();
    }

    /**
     * Main entry point for the JavaFX application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
