package com.privacyemail.ui;

import com.google.inject.Inject;
import com.privacyemail.api.IApiClient;
import com.privacyemail.controllers.ComposeController;
import com.privacyemail.controllers.SettingsController;
import com.privacyemail.controllers.SuggestionsController;
import com.privacyemail.models.EmailDetails;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles loading and displaying secondary application windows.
 */
public class WindowManager {

    private static final Logger logger = LoggerFactory.getLogger(WindowManager.class);

    private IApiClient apiClient;
    private Window ownerWindow; // Optional: Main window to set as owner for modals

    @Inject
    public WindowManager(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Sets the owner window for modal dialogs created by this manager.
     * @param ownerWindow The owner window.
     */
    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    /**
     * Shows the Compose Email window for a new email.
     */
    public void showComposeWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/privacyemail/views/ComposeWindow.fxml"));
            Parent root = loader.load();

            ComposeController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Compose Email");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (ownerWindow != null) {
                 stage.initOwner(ownerWindow);
            }
            stage.setScene(new Scene(root));

            controller.setApiClient(this.apiClient);
            controller.setStage(stage);
            controller.setupAsNewEmail();

            stage.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load ComposeWindow.fxml", e);
            showAlert(Alert.AlertType.ERROR, "Load Error", "Could not open the compose window.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred opening compose window", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred.");
        }
    }

    /**
     * Shows the Settings window.
     * @param showAiTab True to navigate directly to the AI settings tab.
     */
    public void showSettingsWindow(boolean showAiTab) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/privacyemail/views/SettingsWindow.fxml"));

            // Use Controller Factory for Dependency Injection
            loader.setControllerFactory(controllerClass -> {
                if (controllerClass == SettingsController.class) {
                    SettingsController controller = new SettingsController();
                    controller.setApiClient(this.apiClient); // Inject ApiClient
                    return controller;
                }
                try {
                    return controllerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create controller: " + controllerClass, e);
                }
            });

            Parent root = loader.load();
            SettingsController settingsController = loader.getController();

            if (showAiTab) {
                settingsController.showAiSettings();
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (ownerWindow != null) {
                 stage.initOwner(ownerWindow);
            }
            stage.setTitle("Settings");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load Settings window: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open settings window.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred opening settings window", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred opening settings: " + e.getMessage());
        }
    }

    /**
     * Shows the Suggestions window.
     * @param suggestions List of suggestion strings.
     * @param originalEmail Email being replied to.
     * @param selectionCallback Callback executed when a suggestion is selected.
     */
    public void showSuggestionsWindow(List<String> suggestions, EmailDetails originalEmail, Consumer<String> selectionCallback) {
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/privacyemail/views/SuggestionsWindow.fxml"));
            Parent root = loader.load();
            SuggestionsController controller = loader.getController();

            // Pass data and callback to the suggestions controller
            controller.setSuggestions(suggestions, originalEmail, selectionCallback);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }
            stage.setTitle("Reply Suggestions");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMinWidth(600);
            stage.setMinHeight(450);
            stage.setWidth(650);
            stage.setHeight(500);

            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to load Suggestions window", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open suggestions window.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred opening suggestions window", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open suggestions window: " + e.getMessage());
        }
    }

    /**
     * Shows the Compose window initialized for replying to an email.
     * @param originalEmail The email being replied to.
     * @param suggestion The selected suggestion text (can be empty for standard reply).
     */
     public void showReplyWindow(EmailDetails originalEmail, String suggestion) {
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/privacyemail/views/ComposeWindow.fxml"));
            Parent root = loader.load();
            ComposeController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Compose Reply");
            stage.initModality(Modality.WINDOW_MODAL); // Keep on top
             if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMinWidth(700);
            stage.setMinHeight(600);
            stage.setWidth(800);
            stage.setHeight(650);

            controller.setApiClient(this.apiClient);
            controller.setStage(stage);
            controller.initializeForReply(originalEmail, suggestion);

            stage.show();
            stage.toFront();
            stage.requestFocus();

        } catch (IOException e) {
            logger.error("Error loading Compose window for reply", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Compose window: " + e.getMessage());
        } catch (Exception e) {
             logger.error("Error launching compose for reply", e);
             showAlert(Alert.AlertType.ERROR, "Error", "Could not open reply window: " + e.getMessage());
        }
    }

    // Helper to show alerts (could be moved to a dedicated AlertUtil)
    /* private */ public void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
             if (ownerWindow != null) {
                alert.initOwner(ownerWindow);
            }
            alert.showAndWait();
        });
    }

    public void setApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }
}
