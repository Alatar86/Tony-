package com.privacyemail.services;

import com.google.inject.Inject;
import com.privacyemail.api.IApiClient;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.StatusResponse;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Service responsible for monitoring backend and service status.
 */
public class StatusMonitorService implements IStatusMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(StatusMonitorService.class);

    private final IApiClient apiClient;
    private final ExecutorService executorService;

    /**
     * Constructs a StatusMonitorService with the required dependencies.
     *
     * @param apiClient The API client for backend communication
     * @param executorService The executor service for asynchronous tasks
     */
    @Inject
    public StatusMonitorService(IApiClient apiClient, ExecutorService executorService) {
        this.apiClient = apiClient;
        this.executorService = executorService;
    }

    /**
     * Checks the status of the backend services.
     *
     * @return A Task that returns an ApiResult<StatusResponse> with the status information
     */
    public Task<ApiResult<StatusResponse>> checkBackendStatus() {
        logger.info("Checking backend status");

        Task<ApiResult<StatusResponse>> task = new Task<>() {
            @Override
            protected ApiResult<StatusResponse> call() throws Exception {
                boolean isAuthenticated = false;

                try {
                    // First check authentication status
                    ApiResult<Boolean> authResult = apiClient.checkAuthStatus();
                    if (authResult.isSuccess()) {
                        isAuthenticated = authResult.getData();
                    } else {
                        // Authentication check failed
                        logger.warn("Auth check failed: {}", authResult.getErrorMessage());
                    }

                    // Then check overall status
                    ApiResult<StatusResponse> statusResult = apiClient.getBackendStatus();

                    // If status request succeeds, update the local auth value in the result's data
                    if (statusResult.isSuccess() && statusResult.getData() != null) {
                        StatusResponse updatedResponse = statusResult.getData();
                        // Use our auth check result, which might be more accurate
                        updatedResponse.setGmail_authenticated(isAuthenticated);
                    }

                    return statusResult;
                } catch (Exception e) {
                    logger.error("Error checking backend status", e);
                    throw e;
                }
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Gets the current status of backend services.
     *
     * @return A Task that returns an ApiResult<StatusResponse> with the status information
     */
    public Task<ApiResult<StatusResponse>> getBackendStatus() {
        logger.info("Getting backend status");

        Task<ApiResult<StatusResponse>> task = new Task<>() {
            @Override
            protected ApiResult<StatusResponse> call() throws Exception {
                return apiClient.getBackendStatus();
            }
        };

        executorService.submit(task);
        return task;
    }
}
