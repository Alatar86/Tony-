package com.privacyemail.ui;

import com.google.inject.Inject;
import com.privacyemail.api.IApiClient;
import com.privacyemail.models.ApiError;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.EmailMetadata;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Manages the email list view, including data loading and cell rendering.
 */
public class EmailListViewManager {

    private static final Logger logger = LoggerFactory.getLogger(EmailListViewManager.class);

    private final ListView<EmailMetadata> emailListView;
    private final ProgressIndicator emailListProgress;
    private final Button refreshButton; // To disable/enable during refresh
    private final IApiClient apiClient;
    private final ExecutorService executorService;
    private final ObservableList<EmailMetadata> emailList = FXCollections.observableArrayList();
    private final WindowManager windowManager; // Use WindowManager for alerts

    @Inject
    public EmailListViewManager(ListView<EmailMetadata> emailListView,
                                ProgressIndicator emailListProgress,
                                Button refreshButton,
                                IApiClient apiClient,
                                ExecutorService executorService,
                                WindowManager windowManager) {
        this.emailListView = emailListView;
        this.emailListProgress = emailListProgress;
        this.refreshButton = refreshButton;
        this.apiClient = apiClient;
        this.executorService = executorService;
        this.windowManager = windowManager; // Store WindowManager

        setupListView();
    }

    private void setupListView() {
        if (emailListView != null) {
            emailListView.setItems(emailList);
            emailListView.setCellFactory(listView -> new EmailListCell());
            // Selection listener remains in MainWindowController
        } else {
            logger.error("ListView passed to EmailListViewManager is null!");
        }
    }

    /**
     * Refreshes the email list for the specified label ID.
     * @param labelId The label ID (e.g., "INBOX").
     */
    public void refreshEmails(String labelId) {
        logger.info("EmailListViewManager.refreshEmails called for label '{}' on thread: {}", labelId, Thread.currentThread().getName());

        logger.info("Refreshing email list for label ID: {}", labelId);

        // Show loading indicators
        Platform.runLater(() -> {
            if (emailListProgress != null) emailListProgress.setVisible(true);
            if (refreshButton != null) refreshButton.setDisable(true);
            // Consider adding a status message update via callback if needed
        });

        Task<ApiResult<List<EmailMetadata>>> refreshTask = new Task<>() {
            @Override
            protected ApiResult<List<EmailMetadata>> call() throws Exception {
                return apiClient.getEmailList(labelId);
            }
        };

        refreshTask.setOnSucceeded(event -> {
            ApiResult<List<EmailMetadata>> result = refreshTask.getValue();
            Platform.runLater(() -> {
                // Hide loading indicators
                if (emailListProgress != null) emailListProgress.setVisible(false);
                if (refreshButton != null) refreshButton.setDisable(false);

                if (result.isSuccess()) {
                    List<EmailMetadata> newData = result.getData();
                    emailList.clear();
                    emailList.addAll(newData != null ? newData : FXCollections.observableArrayList());

                    if (emailListView != null) {
                        emailListView.getSelectionModel().clearSelection();
                    }
                    logger.info("Email list updated successfully for label: {}. Count: {}", labelId, emailList.size());
                } else {
                    logger.error("Failed to refresh email list for label {}: {}", labelId, result.getError());
                    windowManager.showAlert(Alert.AlertType.ERROR, "Refresh Error", "Could not fetch emails: " + result.getErrorMessage());
                }
            });
        });

        refreshTask.setOnFailed(event -> {
            Throwable exception = refreshTask.getException();
            logger.error("Exception refreshing email list for label " + labelId, exception);
            Platform.runLater(() -> {
                // Hide loading indicators
                 if (emailListProgress != null) emailListProgress.setVisible(false);
                 if (refreshButton != null) refreshButton.setDisable(false);
                windowManager.showAlert(Alert.AlertType.ERROR, "Refresh Error", "An unexpected error occurred while fetching emails: " + exception.getMessage());
            });
        });

        executorService.submit(refreshTask);
    }

    /**
     * Update the read/unread status of an email in the list view UI.
     * (Moved from MainWindowController)
     *
     * @param emailId The ID of the email to update
     * @param unread Whether the email should be marked as unread
     */
    public void updateEmailReadStatus(String emailId, boolean unread) {
        // Find the email in the list
        for (EmailMetadata email : emailList) {
            if (email.getId().equals(emailId)) {
                // Update the model data
                email.setUnread(unread);

                // Refresh the list view to reflect the change in the specific cell
                // We need to ensure this happens on the JavaFX Application Thread
                Platform.runLater(() -> {
                    if (emailListView != null) {
                        // Brute-force refresh (less efficient but simple)
                        // emailListView.refresh();

                        // More targeted refresh (if possible and needed)
                        // Find the index and potentially update just that cell/row
                        int index = emailList.indexOf(email);
                        if (index != -1) {
                             // Force redraw of the cell - Note: ListView.refresh() is often sufficient
                             // and simpler than trying to manually update nodes.
                             emailListView.refresh();
                             logger.info("Refreshed ListView for email read status update: {}", emailId);
                        }
                    }
                });

                logger.info("Updated EmailMetadata read status: {}, unread={}", emailId, unread);
                break; // Exit loop once found
            }
        }
    }

    /**
     * Custom ListCell for rendering emails in the list with read/unread styling.
     * (Moved from MainWindowController)
     */
    private static class EmailListCell extends ListCell<EmailMetadata> {
        private final VBox vbox = new VBox(2); // Tighter spacing
        private final Label subjectLabel = new Label();
        private final Label senderLabel = new Label();
        private final Label previewLabel = new Label();
        private final HBox headerBox = new HBox(5); // Container for subject & sender
        private final Label unreadIndicator = new Label("●"); // Bullet character as unread indicator

        public EmailListCell() {
            subjectLabel.setWrapText(true);
            subjectLabel.setMaxWidth(300);

            senderLabel.setWrapText(true);
            senderLabel.setMaxWidth(300);
            senderLabel.getStyleClass().add("email-sender");
            senderLabel.setStyle("-fx-font-size: 12px;");

            previewLabel.setWrapText(true);
            previewLabel.setMaxWidth(400);
            previewLabel.setMaxHeight(40);
            previewLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");

            unreadIndicator.setStyle("-fx-text-fill: #0078D7; -fx-font-size: 14px;");
            unreadIndicator.setPadding(new Insets(0, 5, 0, 0));

            headerBox.getChildren().addAll(unreadIndicator, subjectLabel);
            vbox.getChildren().addAll(headerBox, senderLabel, previewLabel);
            vbox.setPadding(new Insets(8, 5, 8, 5));
            vbox.setStyle("-fx-border-color: transparent transparent derive(-fx-background, -10%) transparent; -fx-border-width: 0 0 1 0;");
            getStyleClass().add("email-list-cell");
        }

        @Override
        protected void updateItem(EmailMetadata item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                subjectLabel.setText(item.getSubject());
                String sender = item.getFromAddress();
                if (sender.contains("<")) {
                    String name = sender.substring(0, sender.indexOf("<")).trim();
                    if (!name.isEmpty()) {
                        sender = name;
                    } else {
                        String email = sender.substring(sender.indexOf("<") + 1, sender.indexOf(">"));
                        sender = email;
                    }
                }
                senderLabel.setText("From: " + sender);
                String preview = "Click to view email content"; // Placeholder
                previewLabel.setText(preview);

                if (item.isUnread()) {
                    subjectLabel.setStyle("-fx-font-weight: bold;");
                    senderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    getStyleClass().add("unread-email");
                    unreadIndicator.setVisible(true);
                    setStyle("-fx-background-color: rgba(135, 206, 250, 0.3); -fx-border-color: #0078D7 transparent transparent transparent; -fx-border-width: 0 0 0 4px;");
                } else {
                    subjectLabel.setStyle("-fx-font-weight: normal;");
                    senderLabel.setStyle("-fx-font-size: 12px;");
                    getStyleClass().remove("unread-email");
                    unreadIndicator.setVisible(false);
                    setStyle("");
                }
                setGraphic(vbox);
            }
        }
    }
}
