package com.privacyemail.ui;

import com.privacyemail.api.PluginManager;
import com.privacyemail.config.AppContext;
import com.privacyemail.config.ApplicationInitializer;
import com.privacyemail.config.UserPreferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Added Imports for Services
import com.privacyemail.config.Configuration;
import com.privacyemail.services.ICredentialsService;
import com.privacyemail.config.FrontendPreferences;
import com.privacyemail.services.IEmailService;
import com.privacyemail.services.AuthenticationService;
import com.privacyemail.services.EmailManagementService;
import com.privacyemail.services.StatusMonitorService;
import com.privacyemail.security.JavaPreferencesStorage;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import java.time.Duration;

import com.privacyemail.controllers.MainWindowController;
import com.privacyemail.models.EmailMetadata;
import com.privacyemail.ui.UIManagerFactory;

/**
 * Main application class for the Privacy Email client.
 * This class handles application initialization, scene loading,
 * and plugin system startup.
 */
public class MainApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    private static final String APP_TITLE = "Privacy Email";
    private static final String MAIN_FXML = "/com/privacyemail/views/MainWindow.fxml";
    private static final String APP_ICON = "/com/privacyemail/icons/app_icon.png";

    private UserPreferences userPreferences;

    /**
     * Application entry point
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        logger.info("Initializing application");
        userPreferences = UserPreferences.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize plugin system directly with PluginManager
            logger.info("Initializing plugin system");
            PluginManager.getInstance().initializePlugins();

            // Load main scene
            Parent root = loadMainScene();
            Scene scene = createMainScene(root);

            // Configure primary stage
            configureStage(primaryStage, scene);

            // Show the application window
            primaryStage.show();
            logger.info("Application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorAndExit("Application Error",
                    "Failed to start application: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            // Clean up plugin resources
            logger.info("Shutting down plugin system");
            PluginManager.getInstance().shutdownPlugins();

            logger.info("Application shutdown complete");
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        } finally {
            Platform.exit();
        }
    }

    /**
     * Load the main scene from FXML
     * @return the loaded scene graph
     * @throws IOException if the FXML file cannot be loaded
     */
    private Parent loadMainScene() throws IOException {
        logger.info("Loading main scene...");

        // Initialize all dependencies with the centralized ApplicationInitializer
        // This provides a clean, maintainable way to create and wire all services
        // rather than instantiating them directly here
        AppContext appContext = ApplicationInitializer.initializeDependencies();

        // Create FXMLLoader without controller to load the FXML first
        logger.debug("Creating FXMLLoader...");
        FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));

        // First create all the managers with null or minimal implementations
        logger.debug("Creating WindowManager...");
        WindowManager windowManager = UIManagerFactory.createWindowManager(appContext.apiClient());

        // Create proxy implementations with null UI references
        // JavaFX presents a challenge for dependency injection because:
        // 1. Controllers need dependency injection before they're constructed
        // 2. UI components they depend on aren't available until after FXML loading
        // 3. We need a two-phase initialization to resolve this circular dependency
        logger.debug("Creating proxy UI managers with null references...");
        EmailListViewManager emailListViewManagerProxy = null;
        FolderNavigationManager folderNavigationManagerProxy = null;
        EmailDetailViewManager emailDetailViewManagerProxy = null;
        StatusUIManager statusUIManagerProxy = null;

        // Create the controller with null managers initially and the business logic services
        logger.debug("Creating MainWindowController with service dependencies...");
        MainWindowController controller = new MainWindowController(
            appContext.apiClient(),
            appContext.emailService(),
            appContext.executorService(),
            appContext.configuration(),
            appContext.frontendPreferences(),
            appContext.credentialsService(),
            appContext.httpClient(),
            appContext.authenticationService(),
            appContext.emailManagementService(),
            appContext.statusMonitorService()
        );

        // Set the controller BEFORE loading the FXML
        loader.setController(controller);

        // Now load the FXML with the controller set
        logger.debug("Loading FXML...");
        Parent root = loader.load();
        logger.info("FXML loaded successfully");

        // Get FXML references to UI elements from the loaded root
        logger.debug("Getting UI element references from loaded FXML...");
        @SuppressWarnings("unchecked")
        javafx.scene.control.ListView<EmailMetadata> emailListView =
            (javafx.scene.control.ListView<EmailMetadata>) loader.getNamespace().get("emailListView");
        javafx.scene.control.ProgressIndicator emailListProgress = (javafx.scene.control.ProgressIndicator) loader.getNamespace().get("emailListProgress");
        javafx.scene.control.Button refreshButton = (javafx.scene.control.Button) loader.getNamespace().get("refreshButton");
        javafx.scene.control.Button suggestButton = (javafx.scene.control.Button) loader.getNamespace().get("suggestButton");
        javafx.scene.control.Button loginButton = (javafx.scene.control.Button) loader.getNamespace().get("loginButton");
        javafx.scene.control.Button replyButton = (javafx.scene.control.Button) loader.getNamespace().get("replyButton");
        javafx.scene.control.Button archiveButton = (javafx.scene.control.Button) loader.getNamespace().get("archiveButton");
        javafx.scene.control.Button deleteButton = (javafx.scene.control.Button) loader.getNamespace().get("deleteButton");
        javafx.scene.control.Button markReadButton = (javafx.scene.control.Button) loader.getNamespace().get("markReadButton");
        javafx.scene.control.Button markUnreadButton = (javafx.scene.control.Button) loader.getNamespace().get("markUnreadButton");
        javafx.scene.control.Label statusMessageLabel = (javafx.scene.control.Label) loader.getNamespace().get("statusMessageLabel");
        javafx.scene.control.Label gmailStatusLabel = (javafx.scene.control.Label) loader.getNamespace().get("gmailStatusLabel");
        javafx.scene.control.Label aiStatusLabel = (javafx.scene.control.Label) loader.getNamespace().get("aiStatusLabel");
        javafx.scene.control.Label folderTitleLabel = (javafx.scene.control.Label) loader.getNamespace().get("folderTitleLabel");
        javafx.scene.control.Label subjectLabel = (javafx.scene.control.Label) loader.getNamespace().get("subjectLabel");
        javafx.scene.control.Label fromLabel = (javafx.scene.control.Label) loader.getNamespace().get("fromLabel");
        javafx.scene.control.Label dateLabel = (javafx.scene.control.Label) loader.getNamespace().get("dateLabel");
        javafx.scene.control.ProgressIndicator globalProgress = (javafx.scene.control.ProgressIndicator) loader.getNamespace().get("globalProgress");
        javafx.scene.control.ProgressIndicator emailDetailProgress = (javafx.scene.control.ProgressIndicator) loader.getNamespace().get("emailDetailProgress");
        javafx.scene.web.WebView emailBodyView = (javafx.scene.web.WebView) loader.getNamespace().get("emailBodyView");
        javafx.scene.layout.HBox inboxFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("inboxFolder");
        javafx.scene.layout.HBox starredFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("starredFolder");
        javafx.scene.layout.HBox sentFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("sentFolder");
        javafx.scene.layout.HBox draftsFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("draftsFolder");
        javafx.scene.layout.HBox archiveFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("archiveFolder");
        javafx.scene.layout.HBox spamFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("spamFolder");
        javafx.scene.layout.HBox trashFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("trashFolder");
        javafx.scene.layout.HBox workFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("workFolder");
        javafx.scene.layout.HBox personalFolder = (javafx.scene.layout.HBox) loader.getNamespace().get("personalFolder");

        // Create the real managers with the loaded UI elements
        // Create folder map for folder navigation
        logger.debug("Creating folder mapping for navigation...");
        Map<String, javafx.scene.layout.HBox> folderMap = new HashMap<>();
        folderMap.put("INBOX", inboxFolder);
        folderMap.put("STARRED", starredFolder);
        folderMap.put("SENT", sentFolder);
        folderMap.put("DRAFT", draftsFolder);
        folderMap.put("ARCHIVE", archiveFolder);
        folderMap.put("SPAM", spamFolder);
        folderMap.put("TRASH", trashFolder);
        folderMap.put("WORK", workFolder);
        folderMap.put("PERSONAL", personalFolder);

        // Replace the proxy managers with real implementations using the UIManagerFactory
        // We'll use reflection to replace the fields in the controller
        try {
            // Create real managers using the factory
            logger.info("Creating real UI managers with UI elements...");
            EmailListViewManager realEmailListViewManager = UIManagerFactory.createEmailListViewManager(
                emailListView,
                emailListProgress,
                refreshButton,
                appContext.apiClient(),
                appContext.executorService(),
                windowManager
            );

            com.privacyemail.ui.FolderNavigationManager realFolderNavigationManager = UIManagerFactory.createFolderNavigationManager(
                folderTitleLabel,
                folderMap,
                (selectedLabelId) -> {
                    realEmailListViewManager.refreshEmails(selectedLabelId);
                }
            );

            EmailDetailViewManager realEmailDetailViewManager = UIManagerFactory.createEmailDetailViewManager(
                subjectLabel,
                fromLabel,
                dateLabel,
                emailBodyView,
                replyButton,
                suggestButton,
                archiveButton,
                deleteButton,
                markReadButton,
                markUnreadButton,
                appContext.frontendPreferences()
            );

            // Create the status UI manager with the real onAuthenticatedAction that refreshes the inbox
            logger.debug("Creating StatusUIManager with real UI elements...");
            StatusUIManager realStatusUIManager = UIManagerFactory.createStatusUIManager(
                appContext.apiClient(),
                appContext.executorService(),
                () -> {
                    realEmailListViewManager.refreshEmails("INBOX");
                    realFolderNavigationManager.selectFolder("INBOX");
                },
                windowManager,
                gmailStatusLabel,
                aiStatusLabel,
                loginButton,
                refreshButton,
                suggestButton,
                statusMessageLabel,
                globalProgress
            );

            // Use reflection to replace the null manager fields in the controller with real ones
            // This technique is used because:
            // 1. The controller is already constructed with the business logic dependencies
            // 2. We now need to inject the UI-dependent managers after FXML loading
            // 3. We don't want to expose setters that would break encapsulation
            // 4. This approach avoids complex external dependency injection frameworks
            logger.debug("Using reflection to inject real UI managers into controller fields...");

            java.lang.reflect.Field emailListViewManagerField = MainWindowController.class.getDeclaredField("emailListViewManager");
            emailListViewManagerField.setAccessible(true);
            emailListViewManagerField.set(controller, realEmailListViewManager);

            java.lang.reflect.Field folderNavigationManagerField = MainWindowController.class.getDeclaredField("folderNavigationManager");
            folderNavigationManagerField.setAccessible(true);
            folderNavigationManagerField.set(controller, realFolderNavigationManager);

            java.lang.reflect.Field emailDetailViewManagerField = MainWindowController.class.getDeclaredField("emailDetailViewManager");
            emailDetailViewManagerField.setAccessible(true);
            emailDetailViewManagerField.set(controller, realEmailDetailViewManager);

            java.lang.reflect.Field statusUIManagerField = MainWindowController.class.getDeclaredField("statusUIManager");
            statusUIManagerField.setAccessible(true);
            statusUIManagerField.set(controller, realStatusUIManager);

        } catch (Exception e) {
            logger.error("Error replacing proxy managers with real implementations", e);
        }

        return root;
    }

    /**
     * Create the main scene with appropriate styling
     * @param root the root node of the scene graph
     * @return the configured scene
     */
    private Scene createMainScene(Parent root) {
        Scene scene = new Scene(root, 1024, 768);

        // Apply theme based on user preferences
        String theme = userPreferences.getTheme();
        if (UserPreferences.THEME_DARK.equals(theme)) {
            scene.getStylesheets().add("/com/privacyemail/css/dark-theme.css");
        } else {
            scene.getStylesheets().add("/com/privacyemail/css/light-theme.css");
        }

        return scene;
    }

    /**
     * Configure the primary stage
     * @param stage the primary stage
     * @param scene the main scene
     */
    private void configureStage(Stage stage, Scene scene) {
        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);

        // Set application icon
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream(APP_ICON)));
        } catch (Exception e) {
            logger.warn("Could not load application icon", e);
        }
    }

    /**
     * Show an error dialog and exit the application
     * @param title the dialog title
     * @param message the error message
     */
    private void showErrorAndExit(String title, String message) {
        // In a real application, this would show a proper error dialog
        logger.error("{}: {}", title, message);
        Platform.exit();
    }
}
