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
 * Service responsible for handling authentication-related functionality.
 */
public class AuthenticationService implements IAuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final IApiClient apiClient;
    private final ExecutorService executorService;

    /**
     * Constructs an AuthenticationService with the required dependencies.
     *
     * @param apiClient The API client for backend communication
     * @param executorService The executor service for asynchronous tasks
     */
    @Inject
    public AuthenticationService(IApiClient apiClient, ExecutorService executorService) {
        this.apiClient = apiClient;
        this.executorService = executorService;
    }

    /**
     * Initiates the login process.
     *
     * @return A Task that returns an ApiResult<Boolean> indicating success or failure
     */
    public Task<ApiResult<Boolean>> initiateLogin() {
        logger.info("Initiating login process");

        Task<ApiResult<Boolean>> task = new Task<>() {
            @Override
            protected ApiResult<Boolean> call() throws Exception {
                return apiClient.initiateLogin();
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Verifies the authentication status.
     *
     * @return A Task that returns an ApiResult<StatusResponse> with authentication details
     */
    public Task<ApiResult<StatusResponse>> verifyAuthenticationStatus() {
        logger.info("Verifying authentication status");

        Task<ApiResult<StatusResponse>> task = new Task<>() {
            @Override
            protected ApiResult<StatusResponse> call() throws Exception {
                // First check authentication status
                ApiResult<Boolean> authResult = apiClient.checkAuthStatus();

                // Then get overall backend status
                ApiResult<StatusResponse> statusResult = apiClient.getBackendStatus();

                // If status request succeeds, update the auth value from our first check
                if (statusResult.isSuccess() && statusResult.getData() != null) {
                    StatusResponse updatedResponse = statusResult.getData();
                    // If auth check succeeded, use its value
                    if (authResult.isSuccess()) {
                        updatedResponse.setGmail_authenticated(authResult.getData());
                    }
                }

                return statusResult;
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Checks if the user is authenticated with Gmail.
     *
     * @return A Task that returns an ApiResult<Boolean> indicating auth status
     */
    public Task<ApiResult<Boolean>> checkAuthStatus() {
        logger.info("Checking authentication status");

        Task<ApiResult<Boolean>> task = new Task<>() {
            @Override
            protected ApiResult<Boolean> call() throws Exception {
                return apiClient.checkAuthStatus();
            }
        };

        executorService.submit(task);
        return task;
    }
}
