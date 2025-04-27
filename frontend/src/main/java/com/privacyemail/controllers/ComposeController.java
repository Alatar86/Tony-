package com.privacyemail.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.privacyemail.api.IApiClient;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.EmailDetails;
import com.privacyemail.models.ApiError;
import com.privacyemail.models.ConfigData;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.lang.StringBuilder;
import java.net.http.HttpResponse;

/**
 * Controller for the email compose window.
 */
public class ComposeController {

    private static final Logger logger = LoggerFactory.getLogger(ComposeController.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @FXML private BorderPane composePane;
    @FXML private TextField toField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyArea;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    private Stage stage;
    private IApiClient apiClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String replyToId; // Optional message ID if this is a reply
    private ProgressIndicator progressIndicator;

    /**
     * Sets the ApiClient instance to be used by this controller.
     * Should be called by the controller that creates this window.
     * @param apiClient The ApiClient instance.
     */
    public void setApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Initialize the controller.
     */
    public void initialize() {
        logger.info("Initializing ComposeController");

        // Initialize progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setMaxSize(40, 40);

        // Add the progress indicator to the BorderPane as a centered overlay
        if (composePane != null) {
            // Create a container for the progress indicator
            javafx.scene.layout.StackPane progressOverlay = new javafx.scene.layout.StackPane(progressIndicator);
            progressOverlay.setMouseTransparent(true); // Allow clicks to pass through to elements below

            // Add it as top layer in the BorderPane
            composePane.setTop(
                new javafx.scene.layout.StackPane(
                    composePane.getTop(),
                    progressOverlay
                )
            );
        }
    }

    /**
     * Set the stage reference.
     *
     * @param stage The compose window stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;

        // Setup stage close handler to clean up resources
        stage.setOnCloseRequest(event -> {
            executorService.shutdown();
        });
    }

    /**
     * Set up this compose window as a reply to another email.
     *
     * @param originalEmail The email being replied to
     * @param messageId The message ID of the original email
     */
    public void setupAsReply(String toAddress, String subject, String messageId) {
        if (toAddress != null && !toAddress.isEmpty()) {
            toField.setText(toAddress);
        }

        if (subject != null && !subject.isEmpty()) {
            if (!subject.startsWith("Re:")) {
                subjectField.setText("Re: " + subject);
            } else {
                subjectField.setText(subject);
            }
        }

        // Fetch and set body/signature asynchronously
        // We need the original EmailDetails for this, adjust method signature or how it's called
        // Let's assume initializeForReply handles the body setup now.
        // If setupAsReply is still used elsewhere, it needs rework.
        logger.warn("setupAsReply method might need rework depending on usage context without EmailDetails.");
        this.replyToId = messageId;
    }

    /**
     * Initialize this controller for a reply with a selected suggestion.
     *
     * @param originalEmail The email being replied to
     * @param suggestion The selected suggestion text
     */
    public void initializeForReply(EmailDetails originalEmail, String suggestion) {
        // Set the To field from the original sender
        toField.setText(originalEmail.getFromAddress());

        // Set subject with "Re:" prefix if not already present
        String subject = originalEmail.getSubject();
        if (subject != null && !subject.isEmpty()) {
            if (!subject.startsWith("Re:")) {
                subjectField.setText("Re: " + subject);
            } else {
                subjectField.setText(subject);
            }
        }

        // Store the original message ID for the reply
        this.replyToId = originalEmail.getId();

        // Build the body asynchronously to fetch signature without blocking UI
        buildReplyBodyAsync(originalEmail, suggestion);
    }

    /**
     * Asynchronously builds the reply body, fetching the signature.
     */
    private void buildReplyBodyAsync(EmailDetails originalEmail, String suggestion) {
        // Show progress/disable send while building
        setUIState(false); // Disable UI while fetching signature / building body

        Task<String> buildBodyTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Fetch signature in background
                String signature = fetchSignatureInBackground();

                // Format the reply body with suggestion and quoted original message
                StringBuilder bodyText = new StringBuilder();
                String cleanSuggestion = cleanSuggestionText(suggestion);
                bodyText.append(cleanSuggestion).append("\n\n");

                // Add signature if fetched
                if (!signature.isEmpty()) {
                    bodyText.append(signature); // Signature includes separators
                }

                // Add separator and original email info
                bodyText.append("\n\n---\n");
                String dateString = originalEmail.getDate();
                bodyText.append("On ").append(dateString).append(", ");
                bodyText.append(originalEmail.getFromAddress()).append(" wrote:\n\n");

                // Add quoted original email body
                String[] originalBodyLines = originalEmail.getBody().split("\n");
                for (String line : originalBodyLines) {
                    bodyText.append("> ").append(line).append("\n");
                }
                return bodyText.toString();
            }

            @Override
            protected void succeeded() {
                String finalBody = getValue();
                Platform.runLater(() -> {
                    bodyArea.setText(finalBody);
                    setUIState(true); // Re-enable UI
                    bodyArea.requestFocus(); // Focus body after population
                    bodyArea.positionCaret(suggestion.length() + 2); // Position after suggestion
                });
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to build reply body: {}", exception.getMessage(), exception);
                Platform.runLater(() -> {
                    // Show error and re-enable UI
                    showAlert(Alert.AlertType.ERROR, "Error Preparing Reply",
                              "Could not prepare the reply body: " + exception.getMessage());
                    setUIState(true);
                });
            }
        };

        executorService.submit(buildBodyTask);
    }

    /**
     * Fetches the user's email signature from configuration settings in the background.
     * Includes standard separator.
     *
     * @return The formatted user's signature or empty string if retrieval fails.
     */
    private String fetchSignatureInBackground() {
        try {
            logger.info("Fetching user configuration (including signature) in background");
            // Call fetchConfiguration() which gets all config, including signature
            ApiResult<ConfigData> result = apiClient.fetchConfiguration();

            // Add null check for result before calling isSuccess()
            if (result == null) {
                logger.warn("Auth check failed during configuration fetch: null result");
                return "";
            }

            if (result.isSuccess() && result.getData() != null) {
                ConfigData configData = result.getData();
                String signature = null;
                if (configData.user() != null) { // Check if UserConfig is present
                    signature = configData.user().signature(); // Access signature via nested record
                }

                if (signature != null && !signature.trim().isEmpty()) {
                    String formattedSignature = "\n\n-- \n" + signature.trim();
                    logger.info("Successfully retrieved signature from config in background.");
                    return formattedSignature;
                } else {
                    logger.info("Retrieved empty or null signature from config in background.");
                }
            } else {
                logger.warn("Failed to retrieve configuration (signature) in background: {}",
                           result.isSuccess() ? "Empty data" : result.getErrorMessage());
            }
        } catch (Exception e) {
            // Log the exception that occurred during the background fetch
            logger.error("Error retrieving configuration (signature) in background task: {}", e.getMessage(), e);
        }
        return ""; // Return empty string on failure
    }

    /**
     * Clean the suggestion text by removing any instructional text and formatting.
     *
     * @param suggestion The raw suggestion text
     * @return Cleaned suggestion text
     */
    private String cleanSuggestionText(String suggestion) {
        if (suggestion == null || suggestion.isEmpty()) {
            return "";
        }

        // Remove any quotes if they wrap the entire text
        if (suggestion.startsWith("\"") && suggestion.endsWith("\"")) {
            suggestion = suggestion.substring(1, suggestion.length() - 1);
        }

        // Remove common instructional prefixes
        suggestion = suggestion.replaceAll("(?i)^\\s*Based on the email,\\s*(?:here are|I've prepared)\\s*(?:three|some|a few|\\d+)\\s*(?:brief|short|concise|possible|sample|example|distinct|different)?\\s*(?:reply|response|message|email)?\\s*suggestions:?\\s*", "");
        suggestion = suggestion.replaceAll("(?i)^\\s*Here are\\s*(?:three|some|a few|\\d+)\\s*(?:brief|short|concise|possible|sample|example|distinct|different)?\\s*(?:reply|response|message|email)?\\s*suggestions:?\\s*", "");
        suggestion = suggestion.replaceAll("(?i)^\\s*I've prepared\\s*(?:three|some|a few|\\d+)\\s*(?:brief|short|concise|possible|sample|example|distinct|different)?\\s*(?:reply|response|message|email)?\\s*suggestions:?\\s*", "");
        suggestion = suggestion.replaceAll("(?i)^\\s*(?:Here are|I've prepared|Below are|These are)\\s*(?:some|a few|several|various|different)?\\s*(?:options|replies|responses|samples|examples|drafts|approaches|ways)\\s*(?:for [^:]+|to respond|to reply)?:?\\s*", "");

        // Remove numbered or bulleted prefixes
        suggestion = suggestion.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");
        suggestion = suggestion.replaceAll("(?m)^\\s*[•\\*-]\\s+", "");

        // Remove option/suggestion style prefixes
        suggestion = suggestion.replaceAll("(?i)^\\s*(?:Option|Suggestion|Reply|Response|Alternative)\\s+\\d+\\s*:\\s*", "");

        // Remove specific instructional text within the suggestion
        suggestion = suggestion.replaceAll("(?i)I would (suggest|recommend) (responding|replying) with:?\\s*", "");
        suggestion = suggestion.replaceAll("(?i)^(Here is|This is|Below is) (a|my|an|the) (suggested|possible|sample|draft|proposed) (response|reply):?\\s*", "");
        suggestion = suggestion.replaceAll("(?i)you (can|could|might|may) (say|respond|reply|write):?\\s*", "");

        // Remove other instructional text sections
        suggestion = suggestion.replaceAll("(?is)Based on the provided HTML email.*?sections:", "");
        suggestion = suggestion.replaceAll("(?is)Section \\d+:.*?Content", "");
        suggestion = suggestion.replaceAll("(?is)Please provide the specific content.*?to\\.", "");
        suggestion = suggestion.replaceAll("(?is)Example replies:.*?to\\.", "");
        suggestion = suggestion.replaceAll("(?is)\\* For.*?\"\\[link\\]\"\\.", "");
        suggestion = suggestion.replaceAll("(?is)Please specify which section.*?to\\.", "");
        suggestion = suggestion.replaceAll("(?i)select one of the following suggested replies:?\\s*", "");

        // Remove any "Option X:" or "Reply X:" prefixes
        suggestion = suggestion.replaceAll("(?i)\\*\\*Option \\d+:\\*\\*\\s*", "");
        suggestion = suggestion.replaceAll("(?i)\\*\\*Reply \\d+:\\*\\*\\s*", "");

        // Remove markdown formatting
        suggestion = suggestion.replaceAll("\\*\\*(.+?)\\*\\*", "$1"); // Bold
        suggestion = suggestion.replaceAll("\\*(.+?)\\*", "$1");       // Italic
        suggestion = suggestion.replaceAll("__(.+?)__", "$1");         // Underline
        suggestion = suggestion.replaceAll("```.*?```", "");           // Code blocks

        // Remove "..." placeholders
        suggestion = suggestion.replaceAll("\\.{3,}", "");

        // Remove excess newlines that might appear after cleaning
        suggestion = suggestion.replaceAll("(\\n\\s*){2,}", "\n\n");

        return suggestion.trim();
    }

    /**
     * Pre-fill the message body with a suggestion.
     *
     * @param suggestion The suggestion text to use
     */
    public void setMessageBodyText(String suggestion) {
        if (suggestion != null && !suggestion.isEmpty()) {
            bodyArea.setText(suggestion);
            bodyArea.positionCaret(suggestion.length());
        }
    }

    /**
     * Handle the Send button action.
     *
     * @param event The action event
     */
    @FXML
    private void handleSendAction(ActionEvent event) {
        logger.info("Send button clicked.");

        // Basic validation
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText().trim();

        if (to.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Recipient", "Please enter a recipient email address.");
            return;
        }

        if (!isValidEmail(to)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        if (subject.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Subject", "Please enter a subject for your email.");
            return;
        }

        // Send email
        sendEmail(to, subject, body);
    }

    /**
     * Set up this compose window as a new email (not a reply).
     */
    public void setupAsNewEmail() {
        // Clear any previous content
        toField.setText("");
        subjectField.setText("");
        bodyArea.setText(""); // Clear body first
        this.replyToId = null;

        // Fetch and set signature asynchronously
        fetchAndSetSignatureAsync();
    }

    /**
     * Asynchronously fetches the signature and sets it in the body area.
     */
    private void fetchAndSetSignatureAsync() {
        setUIState(false); // Disable UI while fetching
        Task<String> signatureTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return fetchSignatureInBackground();
            }

            @Override
            protected void succeeded() {
                String signature = getValue();
                Platform.runLater(() -> {
                    logger.info("fetchAndSetSignatureAsync succeeded. Signature fetched: [" + signature + "]"); // Log fetched value
                    if (signature != null && !signature.isEmpty()) { // Added null check just in case
                        logger.info("Setting bodyArea text with signature.");
                        bodyArea.setText(signature);
                        logger.info("bodyArea text set. Current text: [" + bodyArea.getText() + "]"); // Log after setting
                        bodyArea.positionCaret(0);
                    } else {
                        logger.info("Signature was null or empty, not setting bodyArea text.");
                    }
                    setUIState(true); // Re-enable UI
                    toField.requestFocus(); // Focus To field for new email
                });
            }

            @Override
            protected void failed() {
                 Throwable exception = getException();
                 logger.error("Failed to fetch signature for new email: {}", exception.getMessage(), exception);
                 Platform.runLater(() -> {
                     showAlert(Alert.AlertType.WARNING, "Signature Error",
                               "Could not load your email signature.");
                     setUIState(true);
                 });
            }
        };
        executorService.submit(signatureTask);
    }

    /**
     * Send the email through the backend API.
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body
     */
    private void sendEmail(String to, String subject, String body) {
        // Check if apiClient was injected
        if (this.apiClient == null) {
            logger.error("ApiClient is null in ComposeController. Cannot send email.");
            showAlert(Alert.AlertType.ERROR, "Internal Error", "Cannot send email due to an internal configuration error.");
            return;
        }

        // Disable UI elements and show progress
        setUIState(false);

        // Create task for sending email
        Task<ApiResult<Map<String, Object>>> sendTask = new Task<>() {
            @Override
            protected ApiResult<Map<String, Object>> call() throws Exception {
                // Call the API to send the email
                if (replyToId != null && !replyToId.isEmpty()) {
                    return apiClient.sendEmail(to, subject, body, replyToId);
                } else {
                    return apiClient.sendEmail(to, subject, body);
                }
            }

            @Override
            protected void succeeded() {
                ApiResult<Map<String, Object>> result = getValue();

                if (result.isSuccess()) {
                    Map<String, Object> data = result.getData();
                    if (Boolean.TRUE.equals(data.get("success"))) {
                        // Show success message
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Email Sent",
                                     "Your email has been sent successfully.");

                            // Close the compose window
                            closeWindow();
                        });
                    } else {
                        // Show error message (unlikely case, but handle it)
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Send Failed",
                                     "Failed to send the email: " + data.get("message"));

                            // Re-enable UI
                            setUIState(true);
                        });
                    }
                } else {
                    // Show API error message
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Send Failed",
                                 result.getErrorMessage());

                        // Re-enable UI
                        setUIState(true);
                    });
                }
            }

            @Override
            protected void failed() {
                // Show error message
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Send Failed",
                             "An error occurred while sending the email: " + getException().getMessage());

                    // Re-enable UI
                    setUIState(true);
                });
            }
        };

        // Execute the task
        executorService.submit(sendTask);
    }

    /**
     * Handle the Discard button action.
     *
     * @param event The action event
     */
    @FXML
    private void handleDiscardAction(ActionEvent event) {
        // If there's content, confirm discard
        if (!toField.getText().trim().isEmpty() ||
            !subjectField.getText().trim().isEmpty() ||
            !bodyArea.getText().trim().isEmpty()) {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Discard Email");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to discard this email?");

            alert.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    logger.info("Discarding compose window");
                    closeWindow();
                }
            });
        } else {
            // No content, just close
            logger.info("Closing empty compose window");
            closeWindow();
        }
    }

    /**
     * Handle the Cancel button action.
     *
     * @param event The action event
     */
    @FXML
    private void handleCancelButtonAction(ActionEvent event) {
        logger.info("Compose cancelled by user");
        closeWindow();
    }

    /**
     * Set the enabled state of UI elements.
     *
     * @param enabled True to enable UI elements, false to disable
     */
    private void setUIState(boolean enabled) {
        Platform.runLater(() -> {
            toField.setDisable(!enabled);
            subjectField.setDisable(!enabled);
            bodyArea.setDisable(!enabled);
            sendButton.setDisable(!enabled);
            cancelButton.setDisable(!enabled);

            // Show/hide progress indicator
            progressIndicator.setVisible(!enabled);
        });
    }

    /**
     * Validate an email address format.
     *
     * @param email The email address to validate
     * @return True if the email format appears valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Close the compose window.
     */
    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Show an alert dialog.
     *
     * @param type The alert type
     * @param title The alert title
     * @param message The alert message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
