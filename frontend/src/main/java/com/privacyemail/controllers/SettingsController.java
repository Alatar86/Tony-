package com.privacyemail.controllers;

import com.privacyemail.api.IApiClient;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.ConfigData;
import com.privacyemail.config.FrontendPreferences;
import com.privacyemail.config.ThemeManager;
import com.privacyemail.config.Configuration;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private IApiClient apiClient;
    private FrontendPreferences frontendPreferences;
    private boolean apiSettingsChanged = false;
    private boolean appearanceSettingsChanged = false;
    private boolean generalSettingsChanged = false;
    private boolean accountSettingsChanged = false;

    @FXML private TabPane settingsTabPane;
    @FXML private TextField ollamaUrlField;
    @FXML private TextField ollamaModelField;
    @FXML private TextField maxEmailsField;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private ComboBox<String> imageLoadComboBox;
    @FXML private ComboBox<String> logLevelComboBox;
    @FXML private Button applyButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private ProgressIndicator settingsProgress;
    @FXML private Label statusLabel;
    @FXML private TextArea signatureTextArea;
    @FXML private CheckBox enableAiSuggestionsToggle;
    @FXML private Label accountStatusLabel;

    // Original values to detect changes
    private String originalTheme;
    private String originalImageLoading;
    private String originalOllamaUrl;
    private String originalOllamaModel;
    private String originalMaxEmails;
    private String originalSignature;
    private boolean originalAiSuggestionsEnabled;

    @FXML
    public void initialize() {
        this.frontendPreferences = FrontendPreferences.getInstance();

        logger.info("Settings Window Initialized. Loading settings...");

        // Check if apiClient is injected before loading
        if (this.apiClient == null) {
            logger.error("ApiClient was not injected into SettingsController!");
            showAlert(AlertType.ERROR, "Initialization Error", "Cannot load settings due to an internal error.");
            // Disable save/apply buttons if client is missing
            setButtonsDisabled(true);
            return; // Stop initialization if client is missing
        }

        // Initialize status label
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        // Setup ComboBoxes
        setupComboBoxes();

        // Initialize AI suggestions toggle
        if (enableAiSuggestionsToggle != null) {
            enableAiSuggestionsToggle.setSelected(frontendPreferences.isAiSuggestionsEnabled());
        }

        // Load settings
        loadSettings();

        // Store original values
        storeOriginalValues();

        // Set up change listeners to enable/disable apply button
        setupChangeListeners();
    }

    private void setupComboBoxes() {
        // Setup theme combo box
        if (themeComboBox != null) {
            themeComboBox.getItems().addAll(
                FrontendPreferences.THEME_LIGHT,
                FrontendPreferences.THEME_DARK,
                FrontendPreferences.THEME_SYSTEM
            );
            // Set the current value from preferences
            themeComboBox.setValue(frontendPreferences.getThemePreference());
        }

        // Setup image loading combo box
        if (imageLoadComboBox != null) {
            imageLoadComboBox.getItems().addAll(
                FrontendPreferences.IMAGE_LOADING_ALWAYS,
                FrontendPreferences.IMAGE_LOADING_ASK,
                FrontendPreferences.IMAGE_LOADING_NEVER
            );
            // Set the current value from preferences
            imageLoadComboBox.setValue(frontendPreferences.getImageLoadingPreference());
        }

        // Setup log level combo box
        if (logLevelComboBox != null) {
            logLevelComboBox.getItems().addAll(
                "ERROR",
                "WARN",
                "INFO",
                "DEBUG",
                "TRACE"
            );
            // Default to INFO level
            logLevelComboBox.setValue("INFO");
        }
    }

    private void storeOriginalValues() {
        originalTheme = themeComboBox != null ? themeComboBox.getValue() : "";
        originalImageLoading = imageLoadComboBox != null ? imageLoadComboBox.getValue() : "";
        originalOllamaUrl = ollamaUrlField != null ? ollamaUrlField.getText() : "";
        originalOllamaModel = ollamaModelField != null ? ollamaModelField.getText() : "";
        originalMaxEmails = maxEmailsField != null ? maxEmailsField.getText() : "";
        originalSignature = signatureTextArea != null ? signatureTextArea.getText() : "";
        originalAiSuggestionsEnabled = enableAiSuggestionsToggle != null && enableAiSuggestionsToggle.isSelected();
    }

    private void setupChangeListeners() {
        // Theme changes
        if (themeComboBox != null) {
            themeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                appearanceSettingsChanged = !newVal.equals(originalTheme);
                updateButtonState();
            });
        }

        // Image loading changes
        if (imageLoadComboBox != null) {
            imageLoadComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                appearanceSettingsChanged = appearanceSettingsChanged || !newVal.equals(originalImageLoading);
                updateButtonState();
            });
        }

        // API URL changes
        if (ollamaUrlField != null) {
            ollamaUrlField.textProperty().addListener((obs, oldVal, newVal) -> {
                apiSettingsChanged = apiSettingsChanged || !newVal.equals(originalOllamaUrl);
                updateButtonState();
            });
        }

        // Model name changes
        if (ollamaModelField != null) {
            ollamaModelField.textProperty().addListener((obs, oldVal, newVal) -> {
                apiSettingsChanged = apiSettingsChanged || !newVal.equals(originalOllamaModel);
                updateButtonState();
            });
        }

        // Max emails changes - track with generalSettingsChanged instead of apiSettingsChanged
        if (maxEmailsField != null) {
            maxEmailsField.textProperty().addListener((obs, oldVal, newVal) -> {
                generalSettingsChanged = generalSettingsChanged || !newVal.equals(originalMaxEmails);
                updateButtonState();
            });
        }

        // Signature changes
        if (signatureTextArea != null) {
            signatureTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
                accountSettingsChanged = accountSettingsChanged || !newVal.equals(originalSignature);
                updateButtonState();
            });
        }

        // AI suggestions toggle changes
        if (enableAiSuggestionsToggle != null) {
            enableAiSuggestionsToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                apiSettingsChanged = apiSettingsChanged || (newVal != originalAiSuggestionsEnabled);
                updateButtonState();
            });
        }

        updateButtonState(); // Initial check
    }

    private void updateButtonState() {
        applyButton.setDisable(!apiSettingsChanged && !appearanceSettingsChanged &&
                               !generalSettingsChanged && !accountSettingsChanged);
    }

    /**
     * Show a status message and progress indicator
     */
    private void showStatus(String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
            }
            if (settingsProgress != null) {
                settingsProgress.setVisible(true);
            }
        });
    }

    /**
     * Clear the status message and hide the progress indicator
     */
    private void clearStatus() {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("");
            }
            if (settingsProgress != null) {
                settingsProgress.setVisible(false);
            }
        });
    }

    private void loadSettings() {
        // Ensure apiClient is not null before proceeding
        if (this.apiClient == null) {
             logger.error("Cannot load settings: ApiClient is null.");
             return; // Or show error
        }
        // Show loading indicator
        showStatus("Loading settings...");

        Task<ApiResult<ConfigData>> loadTask = new Task<>() {
            @Override
            protected ApiResult<ConfigData> call() throws Exception {
                return apiClient.getConfig();
            }
        };

        loadTask.setOnSucceeded(event -> {
            ApiResult<ConfigData> result = loadTask.getValue();

            // Hide loading indicator
            clearStatus();

            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    ConfigData config = result.getData();
                    if (config != null) {
                       if (config.ollama() != null) {
                          ollamaUrlField.setText(config.ollama().api_base_url());
                          ollamaModelField.setText(config.ollama().model_name());
                       }
                       if (config.app() != null) {
                           // Convert int to String for TextField
                           maxEmailsField.setText(String.valueOf(config.app().max_emails_fetch()));
                       }
                       if (config.user() != null) {
                           // Load signature - handle null gracefully
                           String signature = config.user().signature();
                           if (signature != null) {
                               signatureTextArea.setText(signature);
                           } else {
                               signatureTextArea.setText("");
                           }
                           logger.debug("Loaded signature with length: {}",
                                       signature != null ? signature.length() : 0);
                       } else {
                           logger.warn("User config section is null or missing");
                           signatureTextArea.setText("");
                       }
                       logger.info("Settings loaded successfully.");

                       // Load AI Suggestions state from local preferences
                       if (enableAiSuggestionsToggle != null) {
                           enableAiSuggestionsToggle.setSelected(frontendPreferences.isAiSuggestionsEnabled());
                       }

                       // Reset change tracking after loading
                       storeOriginalValues();
                       setupChangeListeners();
                       apiSettingsChanged = false;
                       appearanceSettingsChanged = false;
                       generalSettingsChanged = false;
                       accountSettingsChanged = false;
                       updateButtonState();
                    } else {
                         logger.error("Received null config data from API.");
                         showAlert(AlertType.ERROR, "Load Error", "Failed to retrieve settings data.");
                    }
                } else {
                    logger.error("Failed to load settings: {}", result.getError());
                    showAlert(AlertType.ERROR, "Load Error", "Failed to load settings: " +
                             (result.getError() != null ? result.getError().getUserFriendlyMessage() : "Unknown error"));
                }
            });
        });

        loadTask.setOnFailed(event -> {
            Throwable exception = loadTask.getException();
            logger.error("Exception loading settings", exception);

            // Hide loading indicator
            clearStatus();

            Platform.runLater(() -> showAlert(AlertType.ERROR, "Load Error",
                    "An unexpected error occurred while loading settings: " + exception.getMessage()));
        });

        // Run the task on a background thread
        new Thread(loadTask).start();
    }

    @FXML
    private void handleApplyButtonAction() {
        logger.info("Apply button clicked.");

        // Get the selected tab to determine which settings to apply
        Tab selectedTab = settingsTabPane.getSelectionModel().getSelectedItem();
        String tabId = selectedTab.getText();

        if (tabId.equals("General")) {
            boolean allValid = true;

            // Apply general settings
            if (generalSettingsChanged) {
                if (validateGeneralSettings()) {
                    saveGeneralSettings();
                } else {
                    allValid = false;
                }
            }

            // Apply appearance settings (theme)
            if (appearanceSettingsChanged) {
                if (saveAppearanceSettings()) {
                    showStatus("Appearance settings applied successfully");
                    appearanceSettingsChanged = false;
                    updateButtonState();
                } else {
                    allValid = false;
                }
            }

            if (allValid) {
                showStatus("General settings applied successfully");
            }
        } else if (tabId.equals("Reading")) {
            // Only apply appearance settings
            if (appearanceSettingsChanged) {
                if (saveAppearanceSettings()) {
                    showStatus("Reading settings applied successfully");
                    appearanceSettingsChanged = false;
                    updateButtonState();
                }
            }
        } else if (tabId.equals("AI")) {
            // Save AI settings
            if (apiSettingsChanged) {
                if (validateOllamaSettings()) {
                    saveApiSettings();
                }
            }

            // Save AI suggestions toggle state
            if (enableAiSuggestionsToggle != null) {
                saveAiSuggestionsToggle();
            }
        } else if (tabId.equals("Account")) {
            // Save signature
            if (accountSettingsChanged && signatureTextArea != null) {
                saveSignature();
            }
        } else if (tabId.equals("Advanced")) {
            // Handle Advanced tab settings (logging)
            String selectedLogLevel = logLevelComboBox.getValue();
            if (selectedLogLevel != null) {
                logger.info("Applying log level setting: {}", selectedLogLevel);
                // Note: actual implementation of log level change would require
                // a backend endpoint or local logging configuration
                showStatus("Advanced settings applied successfully");
            }
        }
    }

    @FXML
    private void handleSaveButtonAction() {
        logger.info("Save button clicked.");

        boolean allValid = true;

        // Always save appearance settings (they don't need complex validation)
        saveAppearanceSettings();

        // Validate and save general settings if changed
        if (generalSettingsChanged) {
            if (validateGeneralSettings()) {
                saveGeneralSettings();
            } else {
                allValid = false;
                // Switch to General tab to show validation errors
                settingsTabPane.getSelectionModel().select(0); // General tab
            }
        }

        // Validate API settings only if they've been changed
        if (apiSettingsChanged) {
            if (validateOllamaSettings()) {
                saveApiSettings();
            } else {
                allValid = false;
                // Switch to AI tab to show validation errors
                int aiTabIndex = -1;
                for (int i = 0; i < settingsTabPane.getTabs().size(); i++) {
                    if ("AI".equals(settingsTabPane.getTabs().get(i).getText())) {
                        aiTabIndex = i;
                        break;
                    }
                }
                if (aiTabIndex >= 0) {
                    settingsTabPane.getSelectionModel().select(aiTabIndex);
                } else {
                    logger.warn("Could not find AI tab for selection");
                }
            }
        }

        // Save Account settings if changed (signature)
        if (accountSettingsChanged && signatureTextArea != null) {
            saveSignature();
        }

        // Save AI suggestions toggle state
        if (enableAiSuggestionsToggle != null) {
            saveAiSuggestionsToggle();
        }

        // If everything was valid and saved, close the window
        if (allValid) {
            showStatus("All settings saved successfully");

            // Close window with slight delay to show success message
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(this::closeWindow);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Save appearance settings without validation
     * @return true if successful
     */
    private boolean saveAppearanceSettings() {
        try {
            // Get selected theme
            String selectedTheme = themeComboBox.getValue();
            if (selectedTheme == null || !frontendPreferences.isValidTheme(selectedTheme)) {
                logger.warn("Invalid theme selected: {}", selectedTheme);
                showAlert(Alert.AlertType.WARNING, "Invalid Selection", "Please select a valid theme");
                return false;
            }

            // Get selected image loading preference
            String selectedImageLoading = imageLoadComboBox.getValue();
            if (selectedImageLoading == null || !frontendPreferences.isValidImageLoading(selectedImageLoading)) {
                logger.warn("Invalid image loading preference selected: {}", selectedImageLoading);
                showAlert(Alert.AlertType.WARNING, "Invalid Selection", "Please select a valid image loading preference");
                return false;
            }

            // Save values to preferences
            frontendPreferences.setThemePreference(selectedTheme);
            frontendPreferences.setImageLoadingPreference(selectedImageLoading);

            // Save to disk
            frontendPreferences.savePreferences();

            // Apply theme changes immediately
            ThemeManager.applyThemeToAllScenes();

            logger.info("Appearance settings saved successfully. Theme: {}, Image Loading: {}",
                    selectedTheme, selectedImageLoading);

            // Store new original values
            originalTheme = selectedTheme;
            originalImageLoading = selectedImageLoading;
            appearanceSettingsChanged = false;

            return true;
        } catch (Exception e) {
            logger.error("Error saving appearance settings", e);
            showAlert(Alert.AlertType.ERROR, "Save Error", "An error occurred while saving appearance settings: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate general settings
     * @return true if valid
     */
    private boolean validateGeneralSettings() {
        boolean validationPassed = true;
        StringBuilder validationErrors = new StringBuilder("Please correct the following issues:\n");

        // Validate maxEmailsField - must be a positive integer
        String maxEmailsText = maxEmailsField.getText().trim();
        int maxEmails = 0; // Initialize with default value
        try {
            maxEmails = Integer.parseInt(maxEmailsText);
            if (maxEmails <= 0) {
                validationErrors.append("• Max Emails Fetch Count must be a positive number\n");
                validationPassed = false;
                highlightInvalidField(maxEmailsField, true);
            } else if (maxEmails > 500) {
                // Optional: Add an upper limit to prevent performance issues
                validationErrors.append("• Max Emails Fetch Count cannot exceed 500\n");
                validationPassed = false;
                highlightInvalidField(maxEmailsField, true);
            } else {
                highlightInvalidField(maxEmailsField, false);
            }
        } catch (NumberFormatException e) {
            validationErrors.append("• Max Emails Fetch Count must be a valid number\n");
            validationPassed = false;
            highlightInvalidField(maxEmailsField, true);
        }

        // If validation failed, show error alert
        if (!validationPassed) {
            showAlert(AlertType.ERROR, "Validation Error", validationErrors.toString());
        }

        return validationPassed;
    }

    /**
     * Validate Ollama API settings
     * @return true if valid
     */
    private boolean validateOllamaSettings() {
        boolean validationPassed = true;
        StringBuilder validationErrors = new StringBuilder("Please correct the following issues:\n");

        // Validate ollamaUrlField - must be a valid HTTP or HTTPS URL
        String ollamaUrl = ollamaUrlField.getText().trim();
        if (ollamaUrl.isEmpty()) {
            validationErrors.append("• Ollama URL cannot be empty\n");
            validationPassed = false;
            highlightInvalidField(ollamaUrlField, true);
        } else if (!ollamaUrl.startsWith("http://") && !ollamaUrl.startsWith("https://")) {
            validationErrors.append("• Ollama URL must start with 'http://' or 'https://'\n");
            validationPassed = false;
            highlightInvalidField(ollamaUrlField, true);
        } else {
            highlightInvalidField(ollamaUrlField, false);
        }

        // Validate ollamaModelField - cannot be empty
        String ollamaModel = ollamaModelField.getText().trim();
        if (ollamaModel.isEmpty()) {
            validationErrors.append("• Ollama Model Name cannot be empty\n");
            validationPassed = false;
            highlightInvalidField(ollamaModelField, true);
        } else {
            highlightInvalidField(ollamaModelField, false);
        }

        // If validation failed, show error alert
        if (!validationPassed) {
            showAlert(AlertType.ERROR, "Validation Error", validationErrors.toString());
        }

        return validationPassed;
    }

    /**
     * Save general settings to backend
     */
    private boolean saveGeneralSettings() {
        if (this.apiClient == null) return false;
        try {
            // Validate first
            if (!validateGeneralSettings()) {
                return false;
            }

            // Get current configuration
            ApiResult<ConfigData> configResult = apiClient.getConfig();
            if (!configResult.isSuccess() || configResult.getData() == null) {
                logger.error("Failed to get current config for saving general settings");
                showAlert(AlertType.ERROR, "Save Error", "Could not retrieve current configuration");
                return false;
            }

            ConfigData currentConfig = configResult.getData();

            // Create new AppConfig with updated max emails
            int maxEmails = Integer.parseInt(maxEmailsField.getText().trim());
            ConfigData.AppConfig appConfig = new ConfigData.AppConfig(maxEmails);

            // Keep the current Ollama config and User config
            ConfigData.OllamaConfig ollamaConfig = currentConfig.ollama();
            ConfigData.UserConfig userConfig = currentConfig.user();

            // Create new ConfigData with updated AppConfig
            ConfigData newConfigData = new ConfigData(ollamaConfig, appConfig, userConfig);

            // Save to backend
            ApiResult<Boolean> saveResult = apiClient.saveConfig(newConfigData);

            if (saveResult.isSuccess() && saveResult.getData()) {
                logger.info("General settings saved successfully, Max Emails: {}", maxEmails);

                // Update original value
                originalMaxEmails = maxEmailsField.getText().trim();
                generalSettingsChanged = false;
                updateButtonState();
                showStatus("General settings saved successfully");
                return true;
            } else {
                logger.error("Failed to save general settings: {}", saveResult.getError());
                showAlert(AlertType.ERROR, "Save Error", "Failed to save general settings: " +
                         (saveResult.getError() != null ? saveResult.getError().getUserFriendlyMessage() : "Unknown error"));
                return false;
            }
        } catch (Exception e) {
            logger.error("Error saving general settings", e);
            showAlert(AlertType.ERROR, "Save Error", "An error occurred while saving general settings: " + e.getMessage());
            return false;
        }
    }

    /**
     * Save API settings to backend
     */
    private void saveApiSettings() {
        if (this.apiClient == null) return;
        // Disable buttons during save and show loading indicator
        setButtonsDisabled(true);
        showStatus("Saving API settings...");

        String ollamaUrl = ollamaUrlField.getText().trim();
        String ollamaModel = ollamaModelField.getText().trim();

        // Get current max emails setting to preserve it
        int maxEmails;
        try {
            maxEmails = Integer.parseInt(maxEmailsField.getText().trim());
        } catch (NumberFormatException e) {
            maxEmails = 50; // Default if invalid
        }

        ConfigData.OllamaConfig ollamaConfig = new ConfigData.OllamaConfig(ollamaUrl, ollamaModel);
        ConfigData.AppConfig appConfig = new ConfigData.AppConfig(maxEmails);

        // Get current user config to preserve signature
        ConfigData.UserConfig userConfig = new ConfigData.UserConfig("");
        try {
            ApiResult<ConfigData> configResult = apiClient.getConfig();
            if (configResult.isSuccess() && configResult.getData() != null && configResult.getData().user() != null) {
                userConfig = configResult.getData().user();
            }
        } catch (Exception e) {
            logger.warn("Could not retrieve current user config, using default", e);
        }

        ConfigData newConfigData = new ConfigData(ollamaConfig, appConfig, userConfig);

        Task<ApiResult<Boolean>> saveTask = new Task<>() {
            @Override
            protected ApiResult<Boolean> call() throws Exception {
                return apiClient.saveConfig(newConfigData);
            }
        };

        saveTask.setOnSucceeded(event -> {
            ApiResult<Boolean> result = saveTask.getValue();

            // Hide loading indicator
            clearStatus();

            Platform.runLater(() -> {
                setButtonsDisabled(false); // Re-enable buttons
                if (result.isSuccess() && result.getData()) {
                    logger.info("API settings saved successfully.");
                    // Update original values
                    originalOllamaUrl = ollamaUrl;
                    originalOllamaModel = ollamaModel;
                    apiSettingsChanged = false;
                    updateButtonState();

                    // Show success confirmation
                    showStatus("API settings saved successfully");
                } else {
                    logger.error("Failed to save API settings: {}", result.getError());
                    showAlert(AlertType.ERROR, "Save Error", "Failed to save API settings: " +
                             (result.getError() != null ? result.getError().getUserFriendlyMessage() : "Unknown error"));
                }
            });
        });

        saveTask.setOnFailed(event -> {
            Throwable exception = saveTask.getException();
            logger.error("Exception saving API settings", exception);

            // Hide loading indicator
            clearStatus();

            Platform.runLater(() -> {
                setButtonsDisabled(false); // Re-enable buttons
                showAlert(AlertType.ERROR, "Save Error",
                        "An unexpected error occurred while saving API settings: " + exception.getMessage());
            });
        });

        // Run the task on a background thread
        new Thread(saveTask).start();
    }

    @FXML
    private void handleCancelButtonAction() {
        logger.info("Cancel button clicked.");
        closeWindow();
    }

    private void closeWindow() {
        if (saveButton != null && saveButton.getScene() != null) {
            Stage stage = (Stage) saveButton.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        } else {
             logger.warn("Could not get stage to close settings window.");
        }
    }

    private void setButtonsDisabled(boolean disabled) {
        if (applyButton != null) applyButton.setDisable(disabled);
        if (saveButton != null) saveButton.setDisable(disabled);
        if (cancelButton != null) cancelButton.setDisable(disabled);
    }

    private void showAlert(AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        // Set owner if possible to center on the settings window
        if (saveButton != null && saveButton.getScene() != null && saveButton.getScene().getWindow() != null) {
            alert.initOwner(saveButton.getScene().getWindow());
        }
        alert.showAndWait();
    }

    /**
     * Highlights a field to indicate validation errors by setting a CSS style
     *
     * @param field The TextField to highlight
     * @param isInvalid true to apply error styling, false to remove it
     */
    private void highlightInvalidField(TextField field, boolean isInvalid) {
        Platform.runLater(() -> {
            if (isInvalid) {
                // Apply error styling
                field.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            } else {
                // Remove error styling
                field.setStyle("");
            }
        });
    }

    // Add a method to select the AI tab from outside
    public void showAiSettings() {
        // Check if the tab pane is initialized
        if (settingsTabPane != null) {
            // Find the AI tab by name
            for (Tab tab : settingsTabPane.getTabs()) {
                if ("AI".equals(tab.getText())) {
                    // Select the AI tab
                    settingsTabPane.getSelectionModel().select(tab);
                    logger.info("Showing AI settings tab");
                    return;
                }
            }
            logger.warn("AI tab not found");
        } else {
            logger.warn("Tab pane not initialized");
        }
    }

    /**
     * Save the user's email signature
     */
    private boolean saveSignature() {
        if (this.apiClient == null) return false;
        try {
            logger.info("Saving signature...");

            // Get signature text (handle null)
            String signature = signatureTextArea.getText();
            if (signature == null) {
                signature = "";
            }

            // Using the dedicated API method to save signature
            ApiResult<Boolean> result = apiClient.saveUserSignature(signature);

            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                logger.info("Signature saved successfully");
                originalSignature = signature;
                accountSettingsChanged = false;
                updateButtonState();
                showStatus("Signature saved successfully");
                return true;
            } else {
                logger.error("Failed to save signature: {}",
                    result.isSuccess() ? "API returned false" : result.getErrorMessage());

                showAlert(AlertType.ERROR, "Save Error",
                    "Failed to save signature: " +
                    (result.getError() != null ? result.getError().getUserFriendlyMessage() : "Unknown error"));

                return false;
            }
        } catch (Exception e) {
            logger.error("Error saving signature", e);
            showAlert(AlertType.ERROR, "Save Error", "An error occurred while saving signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Save AI suggestions toggle state
     */
    private boolean saveAiSuggestionsToggle() {
        try {
            boolean isEnabled = enableAiSuggestionsToggle.isSelected();

            // Save to local preferences
            frontendPreferences.setAiSuggestionsEnabled(isEnabled);
            frontendPreferences.savePreferences();

            // Update original value
            originalAiSuggestionsEnabled = isEnabled;

            logger.info("AI suggestions toggle saved: {}", isEnabled);
            return true;
        } catch (Exception e) {
            logger.error("Error saving AI suggestions toggle", e);
            showAlert(AlertType.ERROR, "Save Error", "Failed to save AI suggestions setting: " + e.getMessage());
            return false;
        }
    }

    // Add setter for dependency injection
    public void setApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }
}
