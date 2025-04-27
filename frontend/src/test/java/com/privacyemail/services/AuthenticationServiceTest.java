package com.privacyemail.services;

import com.privacyemail.api.ApiClient;
import com.privacyemail.models.ApiError;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.StatusResponse;
import javafx.concurrent.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AuthenticationService} class.
 */
class AuthenticationServiceTest {

    @Mock
    private ApiClient mockApiClient;

    @Mock
    private ExecutorService mockExecutorService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure the mock executor service to execute submitted tasks synchronously
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null; // submit() return value is ignored in the service
        }).when(mockExecutorService).submit(any(Runnable.class));

        authenticationService = new AuthenticationService(mockApiClient, mockExecutorService);
    }

    @Test
    void initiateLogin_WhenApiSuccess_ReturnsSuccessfulTask() throws Exception {
        // Arrange
        ApiResult<Boolean> successResult = ApiResult.success(true);
        when(mockApiClient.initiateLogin()).thenReturn(successResult);

        // Act
        Task<ApiResult<Boolean>> task = authenticationService.initiateLogin();
        ApiResult<Boolean> result = task.get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockApiClient).initiateLogin();
    }

    @Test
    void initiateLogin_WhenApiFailure_ReturnsFailedTask() throws Exception {
        // Arrange
        ApiError error = ApiError.networkError("Network down");
        ApiResult<Boolean> failureResult = ApiResult.failure(error);
        when(mockApiClient.initiateLogin()).thenReturn(failureResult);

        // Act
        Task<ApiResult<Boolean>> task = authenticationService.initiateLogin();
        ApiResult<Boolean> result = task.get();

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(error, result.getError());
        verify(mockApiClient).initiateLogin();
    }

    @Test
    void verifyAuthenticationStatus_WhenSuccessful_UpdatesGmailAuthenticatedFlag() throws Exception {
        // Arrange
        ApiResult<Boolean> authStatus = ApiResult.success(true);
        StatusResponse backendStatus = new StatusResponse(false, "active"); // initial value false – should be overridden
        ApiResult<StatusResponse> statusResult = ApiResult.success(backendStatus);

        when(mockApiClient.checkAuthStatus()).thenReturn(authStatus);
        when(mockApiClient.getBackendStatus()).thenReturn(statusResult);

        // Act
        Task<ApiResult<StatusResponse>> task = authenticationService.verifyAuthenticationStatus();
        ApiResult<StatusResponse> result = task.get();

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isGmail_authenticated(), "gmail_authenticated should be true after orchestration");
        verify(mockApiClient).checkAuthStatus();
        verify(mockApiClient).getBackendStatus();
    }

    @Test
    void verifyAuthenticationStatus_WhenBackendStatusFailure_ReturnsFailedTask() throws Exception {
        // Arrange
        ApiResult<Boolean> authStatus = ApiResult.success(true);
        ApiError error = ApiError.serverError();
        ApiResult<StatusResponse> statusFailure = ApiResult.failure(error);

        when(mockApiClient.checkAuthStatus()).thenReturn(authStatus);
        when(mockApiClient.getBackendStatus()).thenReturn(statusFailure);

        // Act
        Task<ApiResult<StatusResponse>> task = authenticationService.verifyAuthenticationStatus();
        ApiResult<StatusResponse> result = task.get();

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(error, result.getError());
        verify(mockApiClient).checkAuthStatus();
        verify(mockApiClient).getBackendStatus();
    }

    @Test
    void checkAuthStatus_WhenApiSuccess_ReturnsSuccessfulTask() throws Exception {
        // Arrange
        ApiResult<Boolean> successResult = ApiResult.success(false);
        when(mockApiClient.checkAuthStatus()).thenReturn(successResult);

        // Act
        Task<ApiResult<Boolean>> task = authenticationService.checkAuthStatus();
        ApiResult<Boolean> result = task.get();

        // Assert
        assertTrue(result.isSuccess());
        assertFalse(result.getData());
        verify(mockApiClient).checkAuthStatus();
    }

    @Test
    void checkAuthStatus_WhenApiFailure_ReturnsFailedTask() throws Exception {
        // Arrange
        ApiError error = ApiError.serverError();
        ApiResult<Boolean> failureResult = ApiResult.failure(error);
        when(mockApiClient.checkAuthStatus()).thenReturn(failureResult);

        // Act
        Task<ApiResult<Boolean>> task = authenticationService.checkAuthStatus();
        ApiResult<Boolean> result = task.get();

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(error, result.getError());
        verify(mockApiClient).checkAuthStatus();
    }
}
