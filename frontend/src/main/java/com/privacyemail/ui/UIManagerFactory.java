package com.privacyemail.ui;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.privacyemail.api.IApiClient;
import com.privacyemail.config.FrontendPreferences;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

/**
 * Factory class responsible for creating UI manager instances.
 * This centralizes the creation logic and simplifies the application startup.
 */
public class UIManagerFactory {

    /**
     * Creates a new StatusUIManager instance with all required dependencies.
     *
     * @param apiClient The API client for backend communication
     * @param executorService Executor service for background tasks
     * @param onAuthenticatedAction Action to run when first authenticated
     * @param windowManager Window manager for UI operations
     * @param gmailStatusLabel Label showing Gmail connection status
     * @param aiStatusLabel Label showing AI service status
     * @param loginButton Login button to enable/disable
     * @param refreshButton Refresh button to enable/disable
     * @param suggestButton Suggest button to enable/disable
     * @param statusMessageLabel Label for status messages
     * @param globalProgress Global progress indicator
     * @return A new StatusUIManager instance
     */
    public static StatusUIManager createStatusUIManager(
            IApiClient apiClient,
            ExecutorService executorService,
            Runnable onAuthenticatedAction,
            WindowManager windowManager,
            Label gmailStatusLabel,
            Label aiStatusLabel,
            Button loginButton,
            Button refreshButton,
            Button suggestButton,
            Label statusMessageLabel,
            ProgressIndicator globalProgress) {

        return new StatusUIManager(
                apiClient,
                executorService,
                onAuthenticatedAction,
                windowManager,
                gmailStatusLabel,
                aiStatusLabel,
                loginButton,
                refreshButton,
                suggestButton,
                statusMessageLabel,
                globalProgress
        );
    }

    /**
     * Creates a new EmailListViewManager instance with all required dependencies.
     *
     * @param emailListView ListView for displaying emails
     * @param emailListProgress Progress indicator for email loading
     * @param refreshButton Refresh button to enable/disable
     * @param apiClient The API client for backend communication
     * @param executorService Executor service for background tasks
     * @param windowManager Window manager for UI operations
     * @return A new EmailListViewManager instance
     */
    public static EmailListViewManager createEmailListViewManager(
            ListView<com.privacyemail.models.EmailMetadata> emailListView,
            ProgressIndicator emailListProgress,
            Button refreshButton,
            IApiClient apiClient,
            ExecutorService executorService,
            WindowManager windowManager) {

        return new EmailListViewManager(
                emailListView,
                emailListProgress,
                refreshButton,
                apiClient,
                executorService,
                windowManager
        );
    }

    /**
     * Creates a new EmailDetailViewManager instance with all required dependencies.
     *
     * @param subjectLabel Label for email subject
     * @param fromLabel Label for email sender
     * @param dateLabel Label for email date
     * @param emailBodyView WebView for email content
     * @param replyButton Reply button
     * @param suggestButton Suggest button
     * @param archiveButton Archive button
     * @param deleteButton Delete button
     * @param markReadButton Mark read button
     * @param markUnreadButton Mark unread button
     * @param frontendPreferences User preferences
     * @return A new EmailDetailViewManager instance
     */
    public static EmailDetailViewManager createEmailDetailViewManager(
            Label subjectLabel,
            Label fromLabel,
            Label dateLabel,
            WebView emailBodyView,
            Button replyButton,
            Button suggestButton,
            Button archiveButton,
            Button deleteButton,
            Button markReadButton,
            Button markUnreadButton,
            FrontendPreferences frontendPreferences) {

        return new EmailDetailViewManager(
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
                frontendPreferences
        );
    }

    /**
     * Creates a new FolderNavigationManager instance with all required dependencies.
     *
     * @param folderTitleLabel Label for the folder title
     * @param folderMap Map of folder labels to UI components
     * @param onFolderSelectedAction Action to perform when a folder is selected
     * @return A new FolderNavigationManager instance
     */
    public static com.privacyemail.ui.FolderNavigationManager createFolderNavigationManager(
            Label folderTitleLabel,
            Map<String, HBox> folderMap,
            Consumer<String> onFolderSelectedAction) {

        return new com.privacyemail.ui.FolderNavigationManager(
                folderTitleLabel,
                folderMap,
                onFolderSelectedAction
        );
    }

    /**
     * Creates a new WindowManager instance with all required dependencies.
     *
     * @param apiClient The API client for backend communication
     * @return A new WindowManager instance
     */
    public static WindowManager createWindowManager(IApiClient apiClient) {
        return new WindowManager(apiClient);
    }
}
