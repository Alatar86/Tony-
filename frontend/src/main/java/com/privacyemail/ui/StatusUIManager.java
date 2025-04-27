package com.privacyemail.ui;

import com.google.inject.Inject;
import com.privacyemail.api.IApiClient;
import com.privacyemail.models.ApiError;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.StatusResponse;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
// import java.io.IOException; // Not directly used

/**
 * Manages checking backend status and updating relevant UI elements.
 */
public class StatusUIManager {

    private static final Logger logger = LoggerFactory.getLogger(StatusUIManager.class);

    private final IApiClient apiClient;
    private final ExecutorService executorService;
    private final Runnable onAuthenticatedAction; // Action to run when first authenticated
    private final WindowManager windowManager; // Use WindowManager for alerts

    // UI Components to update
    private final Label gmailStatusLabel;
    private final Label aiStatusLabel;
    private final Button loginButton;
    private final Button refreshButton;
    private final Button suggestButton;
    private final Label statusMessageLabel;
    private final ProgressIndicator globalProgress;

    private boolean firstCheckComplete = false;

    @Inject
    public StatusUIManager(IApiClient apiClient,
                           ExecutorService executorService,
                           Runnable onAuthenticatedAction,
                           WindowManager windowManager, // Inject WindowManager
                           Label gmailStatusLabel,
                           Label aiStatusLabel,
                           Button loginButton,
                           Button refreshButton,
                           Button suggestButton,
                           Label statusMessageLabel,
                           ProgressIndicator globalProgress) {
        this.apiClient = apiClient;
        this.executorService = executorService;
        this.onAuthenticatedAction = onAuthenticatedAction;
        this.windowManager = windowManager; // Store WindowManager
        this.gmailStatusLabel = gmailStatusLabel;
        this.aiStatusLabel = aiStatusLabel;
        this.loginButton = loginButton;
        this.refreshButton = refreshButton;
        this.suggestButton = suggestButton;
        this.statusMessageLabel = statusMessageLabel;
        this.globalProgress = globalProgress;
    }

    /**
     * Performs the initial status check when the application starts.
     */
    public void performInitialStatusCheck() {
        logger.info("Performing initial backend status check...");
        logger.debug("Components state: " +
                  "gmailStatusLabel=" + (gmailStatusLabel != null ? "initialized" : "null") + ", " +
                  "aiStatusLabel=" + (aiStatusLabel != null ? "initialized" : "null") + ", " +
                  "loginButton=" + (loginButton != null ? "initialized" : "null") + ", " +
                  "refreshButton=" + (refreshButton != null ? "initialized" : "null") + ", " +
                  "suggestButton=" + (suggestButton != null ? "initialized" : "null") + ", " +
                  "statusMessageLabel=" + (statusMessageLabel != null ? "initialized" : "null") + ", " +
                  "globalProgress=" + (globalProgress != null ? "initialized" : "null"));
        checkBackendStatus(true);
    }

    /**
     * Checks the status of backend services and updates the UI.
     * @param isInitialCheck If true, runs the onAuthenticatedAction upon success.
     */
    private void checkBackendStatus(boolean isInitialCheck) {
        logger.info("checkBackendStatus called with isInitialCheck={}", isInitialCheck);
        showStatusMessage("Checking backend status...", true);

        Task<ApiResult<StatusResponse>> task = new Task<>() {
            private boolean wasAuthenticated = false;
            private String finalAiServiceStatus = "unknown";

            @Override
            protected ApiResult<StatusResponse> call() throws Exception {
                logger.debug("Starting task.call() to check backend status");
                try {
                    logger.debug("Calling apiClient.checkAuthStatus()");
                    ApiResult<Boolean> authResult = apiClient.checkAuthStatus();
                    if (authResult != null && authResult.isSuccess()) {
                        wasAuthenticated = authResult.getData();
                        logger.debug("Auth check result: authenticated={}", wasAuthenticated);
                    } else {
                        logger.warn("Auth check failed during status check: {}",
                            authResult != null ? authResult.getErrorMessage() : "null result");
                    }
                    logger.debug("Calling apiClient.getBackendStatus()");
                    return apiClient.getBackendStatus();
                } catch (Exception e) {
                    logger.error("Error checking backend status in background task", e);
                    ApiError error = new ApiError("Failed to connect to backend: " + e.getMessage(), 500, null);
                    return ApiResult.failure(error);
                }
            }

            @Override
            protected void succeeded() {
                logger.debug("Task succeeded callback invoked");
                clearStatusMessage();
                ApiResult<StatusResponse> result = getValue();
                boolean isAuthenticatedNow = false;

                if (result != null && result.isSuccess()) {
                    StatusResponse status = result.getData();
                    if (status != null) {
                        isAuthenticatedNow = status.isGmail_authenticated();
                        finalAiServiceStatus = status.getLocal_ai_service_status();
                        logger.info("Backend status check result: Gmail auth={}, AI service={}",
                                   isAuthenticatedNow, finalAiServiceStatus);
                    } else {
                        logger.warn("Backend returned null status response. Re-checking auth status directly.");
                        // Fallback: Re-check auth directly if status is null (shouldn't happen ideally)
                        try {
                            ApiResult<Boolean> authFallback = apiClient.checkAuthStatus();
                            isAuthenticatedNow = authFallback != null && authFallback.isSuccess() && authFallback.getData();
                            logger.debug("Auth fallback result: authenticated={}", isAuthenticatedNow);
                        } catch (Exception e) { logger.error("Fallback auth check failed", e); }
                        finalAiServiceStatus = "error"; // Assume AI error if status response was null
                    }
                } else {
                    String errorMsg = (result != null && result.getError() != null) ?
                                      result.getError().toString() : "Unknown error (null result)";
                    logger.error("Failed to get backend status: {}. Checking auth status directly.", errorMsg);
                    // Fallback: Check auth directly if status call failed
                     try {
                         ApiResult<Boolean> authFallback = apiClient.checkAuthStatus();
                         isAuthenticatedNow = authFallback != null && authFallback.isSuccess() &&
                                             (authFallback.getData() != null && authFallback.getData());
                         logger.debug("Auth fallback result: authenticated={}", isAuthenticatedNow);
                     } catch (Exception e) { logger.error("Fallback auth check failed", e); }
                    finalAiServiceStatus = "error";
                    // Use WindowManager for alert
                    windowManager.showAlert(Alert.AlertType.ERROR, "Connection Error",
                        result != null ? result.getErrorMessage() : "Failed to connect to backend server");
                }

                logger.debug("About to update UI with auth status={} and aiStatus={}", isAuthenticatedNow, finalAiServiceStatus);
                updateAuthStatus(isAuthenticatedNow);
                updateAiServiceStatus(finalAiServiceStatus);

                // Trigger initial action only on the first successful authenticated check
                if (isInitialCheck && isAuthenticatedNow && !firstCheckComplete) {
                     firstCheckComplete = true;
                     logger.info("Initial authentication successful, running onAuthenticatedAction.");
                     if (onAuthenticatedAction != null) {
                         logger.debug("Calling onAuthenticatedAction");
                         onAuthenticatedAction.run();
                     } else {
                         logger.warn("onAuthenticatedAction is null, cannot run initial action");
                     }
                } else if (isInitialCheck) {
                     logger.info("Initial auth check completed but authentication state is: {} or firstCheckComplete: {}",
                             isAuthenticatedNow, firstCheckComplete);
                }
            }

            @Override
            protected void failed() {
                logger.error("Backend status check task failed", getException());
                clearStatusMessage();
                updateAuthStatus(false);
                updateAiServiceStatus("error");
                 // Use WindowManager for alert
                windowManager.showAlert(Alert.AlertType.ERROR, "Connection Error",
                        "Could not connect to the backend service: " + getException().getMessage());
            }
        };
        logger.debug("Submitting status check task to executor service");
        executorService.submit(task);
    }

    /**
     * Update the authentication status in the UI.
     * (Moved from MainWindowController)
     */
    private void updateAuthStatus(boolean authenticated) {
        logger.debug("updateAuthStatus called with authenticated={}", authenticated);
        Platform.runLater(() -> {
            logger.debug("Inside Platform.runLater for updateAuthStatus");
            if (gmailStatusLabel != null) {
                 gmailStatusLabel.setText("Gmail: " + (authenticated ? "Authenticated" : "Not Authenticated"));
                 logger.debug("Updated gmailStatusLabel text");
            } else {
                 logger.warn("gmailStatusLabel is null, can't update authentication status display");
            }
            if (loginButton != null) {
                 loginButton.setDisable(authenticated);
                 // Optionally change text back if needed
                 // loginButton.setText(authenticated ? "Sign Out" : "Login");
                 logger.debug("Updated loginButton disabled state to {}", authenticated);
            } else {
                 logger.warn("loginButton is null, can't update button state");
            }
            if (refreshButton != null) {
                refreshButton.setDisable(!authenticated);
                logger.debug("Updated refreshButton disabled state to {}", !authenticated);
            } else {
                 logger.warn("refreshButton is null, can't update button state");
            }
        });
    }

    /**
     * Update AI service status indicator.
     * (Moved from MainWindowController)
     *
     * @param status The status string from the backend
     */
    private void updateAiServiceStatus(String status) {
        logger.debug("updateAiServiceStatus called with status={}", status);
        Platform.runLater(() -> {
            logger.debug("Inside Platform.runLater for updateAiServiceStatus");
            if (aiStatusLabel != null) {
                boolean isOnline = "online".equalsIgnoreCase(status) ||
                                 "active".equalsIgnoreCase(status) ||
                                 "running".equalsIgnoreCase(status) ||
                                 "connected".equalsIgnoreCase(status);

                if (isOnline) {
                    aiStatusLabel.setText("AI Service: Online");
                    aiStatusLabel.setStyle("-fx-text-fill: green;");
                    logger.debug("Updated aiStatusLabel to online state");
                     if (suggestButton != null) {
                         suggestButton.setDisable(false); // Re-enable if online
                         suggestButton.setTooltip(null); // Clear tooltip
                         logger.debug("Enabled suggestButton and cleared tooltip");
                     } else {
                         logger.warn("suggestButton is null, can't update button state");
                     }
                } else {
                    aiStatusLabel.setText("AI Service: Offline");
                    aiStatusLabel.setStyle("-fx-text-fill: red;");
                    logger.debug("Updated aiStatusLabel to offline state");
                    if (suggestButton != null) {
                        suggestButton.setDisable(true);
                        suggestButton.setTooltip(new Tooltip("AI service is offline. Check settings."));
                        logger.debug("Disabled suggestButton and set tooltip");
                    } else {
                         logger.warn("suggestButton is null, can't update button state");
                    }
                }
                logger.info("Updated AI service status: {}, isOnline={}", status, isOnline);
            } else {
                logger.warn("aiStatusLabel is null, can't update AI service status display");
            }
        });
    }

    // --- Make public for use by other controllers/managers ---
    public void showStatusMessage(String message, boolean showProgress) {
        Platform.runLater(() -> {
            if (statusMessageLabel != null) {
                statusMessageLabel.setText(message);
            }
            if (globalProgress != null) {
                globalProgress.setVisible(showProgress);
            }
        });
    }

    public void clearStatusMessage() {
        Platform.runLater(() -> {
            if (statusMessageLabel != null) {
                statusMessageLabel.setText("");
            }
            if (globalProgress != null) {
                globalProgress.setVisible(false);
            }
        });
    }
}
