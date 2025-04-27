package com.privacyemail.ui;

import com.google.inject.Inject;
import com.privacyemail.config.FrontendPreferences;
import com.privacyemail.config.ThemeManager;
import com.privacyemail.models.ApiError;
import com.privacyemail.models.EmailDetails;
import com.privacyemail.util.EmailContentRenderer;
import com.privacyemail.util.WebViewHelper;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the UI components displaying the details of a selected email.
 */
public class EmailDetailViewManager {

    private static final Logger logger = LoggerFactory.getLogger(EmailDetailViewManager.class);

    // UI Components
    private final Label subjectLabel;
    private final Label fromLabel;
    private final Label dateLabel;
    private final WebView emailBodyView;
    private final Button replyButton;
    private final Button suggestButton;
    private final Button archiveButton;
    private final Button deleteButton;
    private final Button markReadButton;
    private final Button markUnreadButton;

    // Dependencies
    private final FrontendPreferences frontendPreferences;

    @Inject
    public EmailDetailViewManager(Label subjectLabel, Label fromLabel, Label dateLabel, WebView emailBodyView,
                                Button replyButton, Button suggestButton, Button archiveButton, Button deleteButton,
                                Button markReadButton, Button markUnreadButton,
                                FrontendPreferences frontendPreferences) {
        this.subjectLabel = subjectLabel;
        this.fromLabel = fromLabel;
        this.dateLabel = dateLabel;
        this.emailBodyView = emailBodyView;
        this.replyButton = replyButton;
        this.suggestButton = suggestButton;
        this.archiveButton = archiveButton;
        this.deleteButton = deleteButton;
        this.markReadButton = markReadButton;
        this.markUnreadButton = markUnreadButton;
        this.frontendPreferences = frontendPreferences;
    }

    /**
     * Display email details in the UI.
     * (Moved from MainWindowController)
     *
     * @param details Email details to display
     */
    public void displayDetails(EmailDetails details) {
        if (details == null) {
            logger.warn("Attempted to display null email details.");
            clearDetails(); // Clear view if details are null
            return;
        }

        logger.debug("Displaying details for email ID: {}", details.getId());

        // Update the detail labels
        subjectLabel.setText(details.getSubject());
        fromLabel.setText(details.getFromAddress());
        dateLabel.setText(details.getDate());

        // Load the email content in the WebView based on user preferences
        if (emailBodyView != null) {
            boolean isHtml = details.isHtml();
            String content = details.getBody();

            try {
                // Determine theme status
                boolean isDarkTheme = ThemeManager.isSystemInDarkMode() ||
                                     FrontendPreferences.THEME_DARK.equals(frontendPreferences.getThemePreference());
                String imageLoadingPref = frontendPreferences.getImageLoadingPreference();

                // Render content using helper class
                if (isHtml) {
                    content = EmailContentRenderer.renderHtmlContent(content, isDarkTheme, imageLoadingPref);
                } else {
                    content = EmailContentRenderer.renderPlainTextContent(content, isDarkTheme);
                }

                // Use helper to load content into WebView
                WebViewHelper.loadContent(emailBodyView, content);

            } catch (Exception e) {
                logger.error("Error preparing email content for display: {}", e.getMessage(), e);
                String safeContent = details.getBody() != null ? details.getBody().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim() : "";
                String fallbackHtml = "<html><body><p style='color: #666;'><i>Error rendering message. Simplified content:</i></p><p>" + safeContent + "</p></body></html>";
                WebViewHelper.loadContent(emailBodyView, fallbackHtml);
            }
        }

        // Enable action buttons
        updateButtonState(true);
    }

    /**
     * Handle errors when loading email details, updating the UI.
     * (Moved from MainWindowController)
     *
     * @param error The API error that occurred
     */
    public void displayError(ApiError error) {
        logger.error("Displaying error in detail view: {}", error);
        subjectLabel.setText("Error loading email");
        fromLabel.setText("");
        dateLabel.setText("");

        if (emailBodyView != null) {
            String errorMessage = (error != null) ? error.getUserFriendlyMessage() : "An unknown error occurred.";
            String errorHtml = "<div style='color: red; padding: 20px;'><h3>Error Loading Email</h3><p>" + errorMessage + "</p></div>";
            WebViewHelper.loadContent(emailBodyView, errorHtml);
        }

        // Disable action buttons
        updateButtonState(false);
    }

    /**
     * Clear email detail view UI components.
     * (Moved from MainWindowController)
     */
    public void clearDetails() {
        logger.debug("Clearing email detail view.");
        Platform.runLater(() -> {
            if (subjectLabel != null) subjectLabel.setText("--");
            if (fromLabel != null) fromLabel.setText("--");
            if (dateLabel != null) dateLabel.setText("--");

            if (emailBodyView != null) {
                WebViewHelper.loadContent(emailBodyView, "<html><body><p>Select an email to view its content.</p></body></html>");
            }

            updateButtonState(false); // Disable buttons when cleared
        });
    }

    /**
     * Update the state of action buttons based on whether details are displayed.
     * (Moved from MainWindowController)
     *
     * @param enabled Whether buttons should be enabled
     */
    private void updateButtonState(boolean enabled) {
        // Ensure UI updates happen on the JavaFX Application Thread
        Platform.runLater(() -> {
            if (replyButton != null) replyButton.setDisable(!enabled);
            if (suggestButton != null) suggestButton.setDisable(!enabled);
            if (archiveButton != null) archiveButton.setDisable(!enabled);
            if (deleteButton != null) deleteButton.setDisable(!enabled);
            if (markReadButton != null) markReadButton.setDisable(!enabled);
            if (markUnreadButton != null) markUnreadButton.setDisable(!enabled);
        });
    }
}
