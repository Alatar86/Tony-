package com.privacyemail.config;

import com.privacyemail.api.IApiClient;
import com.privacyemail.services.IAuthenticationService;
import com.privacyemail.services.IEmailManagementService;
import com.privacyemail.services.IStatusMonitorService;
import com.privacyemail.services.ICredentialsService;
import com.privacyemail.services.IEmailService;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;

/**
 * Container for all core application dependencies.
 * This record holds references to all initialized services and components
 * that are shared throughout the application.
 */
public record AppContext(
        Configuration configuration,
        FrontendPreferences frontendPreferences,
        HttpClient httpClient,
        IApiClient apiClient,
        ExecutorService executorService,
        ICredentialsService credentialsService,
        IEmailService emailService,
        IAuthenticationService authenticationService,
        IEmailManagementService emailManagementService,
        IStatusMonitorService statusMonitorService
) {
    // Add any utility methods for the context here if needed
}
