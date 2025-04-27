package com.privacyemail.config;

import com.privacyemail.api.ApiClient;
import com.privacyemail.api.IApiClient;
import com.privacyemail.security.JavaPreferencesStorage;
import com.privacyemail.security.PlatformSecureStorage;
import com.privacyemail.security.SecureStorageService;
import com.privacyemail.services.AuthenticationService;
import com.privacyemail.services.CredentialsService;
import com.privacyemail.services.EmailManagementService;
import com.privacyemail.services.EmailService;
import com.privacyemail.services.StatusMonitorService;
import com.privacyemail.services.IAuthenticationService;
import com.privacyemail.services.IEmailManagementService;
import com.privacyemail.services.IStatusMonitorService;
import com.privacyemail.services.ICredentialsService;
import com.privacyemail.services.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralizes application dependency initialization.
 * Responsible for creating and wiring all core services used by the application.
 */
public class ApplicationInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationInitializer.class);

    /**
     * Initializes and wires all core application dependencies.
     *
     * @return An AppContext containing all initialized services and dependencies
     */
    public static AppContext initializeDependencies() {
        logger.info("Initializing application dependencies");

        // Core configuration
        Configuration configuration = Configuration.getInstance();
        FrontendPreferences frontendPreferences = FrontendPreferences.getInstance();

        // HTTP and API client
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        IApiClient apiClient = new ApiClient(httpClient, configuration);

        // Thread pool
        ExecutorService executorService = Executors.newCachedThreadPool();

        // Storage and credentials
        SecureStorageService secureStorage;
        try {
            // Try to use the platform-native secure storage
            secureStorage = new PlatformSecureStorage();
            logger.info("Using platform native secure storage for credentials");
        } catch (Exception e) {
            // Fall back to Java Preferences if platform secure storage is not available
            logger.warn("Platform secure storage unavailable, falling back to Java Preferences: {}", e.getMessage());
            secureStorage = new JavaPreferencesStorage(CredentialsService.class);
        }
        ICredentialsService credentialsService = new CredentialsService(secureStorage);

        // Email service
        IEmailService emailService = new EmailService(apiClient);

        // Business logic services
        logger.debug("Creating business logic services");
        IAuthenticationService authenticationService = new AuthenticationService(apiClient, executorService);
        IEmailManagementService emailManagementService = new EmailManagementService(apiClient, executorService);
        IStatusMonitorService statusMonitorService = new StatusMonitorService(apiClient, executorService);

        // Create and return the context object with all dependencies
        return new AppContext(
                configuration,
                frontendPreferences,
                httpClient,
                apiClient,
                executorService,
                credentialsService,
                emailService,
                authenticationService,
                emailManagementService,
                statusMonitorService
        );
    }
}
