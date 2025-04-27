package com.privacyemail.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.privacyemail.api.ApiClient;
import com.privacyemail.api.IApiClient;
import com.privacyemail.security.JavaPreferencesStorage;
import com.privacyemail.security.PlatformSecureStorage;
import com.privacyemail.security.SecureStorageService;
import com.privacyemail.services.AuthenticationService;
import com.privacyemail.services.CredentialsService;
import com.privacyemail.services.EmailManagementService;
import com.privacyemail.services.EmailService;
import com.privacyemail.services.IAuthenticationService;
import com.privacyemail.services.ICredentialsService;
import com.privacyemail.services.IEmailManagementService;
import com.privacyemail.services.IEmailService;
import com.privacyemail.services.IStatusMonitorService;
import com.privacyemail.services.StatusMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Guice module for configuring application dependencies.
 */
public class AppModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(AppModule.class);

    // Optional: Add a private constructor to prevent instantiation
    private AppModule() {}

    @Override
    protected void configure() {
        // --- Service Bindings ---

        // Binding interfaces to their concrete implementations as singletons.
        // Guice will create one instance of each implementation and reuse it.

        bind(IApiClient.class)
            .to(ApiClient.class)
            .in(Scopes.SINGLETON);

        bind(IAuthenticationService.class)
            .to(AuthenticationService.class)
            .in(Scopes.SINGLETON);

        bind(ICredentialsService.class)
            .to(CredentialsService.class)
            .in(Scopes.SINGLETON);

        bind(IEmailService.class)
            .to(EmailService.class)
            .in(Scopes.SINGLETON);

        bind(IEmailManagementService.class)
            .to(EmailManagementService.class)
            .in(Scopes.SINGLETON);

        bind(IStatusMonitorService.class)
            .to(StatusMonitorService.class)
            .in(Scopes.SINGLETON);

        // UI Manager bindings removed as requested

        // Reminder: We will handle Configuration, FrontendPreferences, HttpClient,
        // ExecutorService, and PlatformSecureStorage using @Provides methods next.
    }

    /**
     * Provides the singleton Configuration instance using the same approach as ApplicationInitializer.
     */
    @Provides
    @Singleton
    Configuration provideConfiguration() {
        logger.info("Providing Configuration instance");
        // From ApplicationInitializer: Configuration configuration = Configuration.getInstance();
        return Configuration.getInstance();
    }

    /**
     * Provides the ExecutorService (thread pool) instance using the same approach as ApplicationInitializer.
     */
    @Provides
    @Singleton
    ExecutorService provideExecutorService() {
        logger.info("Creating thread pool (ExecutorService)");
        // From ApplicationInitializer: ExecutorService executorService = Executors.newCachedThreadPool();
        return Executors.newCachedThreadPool();
    }

    /**
     * Provides the FrontendPreferences instance using the same approach as ApplicationInitializer.
     */
    @Provides
    @Singleton
    FrontendPreferences provideFrontendPreferences() {
        logger.info("Providing FrontendPreferences instance");
        // From ApplicationInitializer: FrontendPreferences frontendPreferences = FrontendPreferences.getInstance();
        return FrontendPreferences.getInstance();
    }

    /**
     * Provides the HttpClient instance using the same approach as ApplicationInitializer.
     */
    @Provides
    @Singleton
    HttpClient provideHttpClient() {
        logger.info("Creating HttpClient instance");
        // From ApplicationInitializer:
        // HttpClient httpClient = HttpClient.newBuilder()
        //         .version(HttpClient.Version.HTTP_1_1)
        //         .connectTimeout(Duration.ofSeconds(15))
        //         .build();
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Provides the SecureStorageService instance using the same approach as ApplicationInitializer.
     * Uses PlatformSecureStorage with fallback to JavaPreferencesStorage if platform secure storage is not available.
     */
    @Provides
    @Singleton
    SecureStorageService provideSecureStorage() {
        logger.info("Creating SecureStorageService instance");
        // From ApplicationInitializer:
        // SecureStorageService secureStorage;
        // try {
        //     // Try to use the platform-native secure storage
        //     secureStorage = new PlatformSecureStorage();
        //     logger.info("Using platform native secure storage for credentials");
        // } catch (Exception e) {
        //     // Fall back to Java Preferences if platform secure storage is not available
        //     logger.warn("Platform secure storage unavailable, falling back to Java Preferences: {}", e.getMessage());
        //     secureStorage = new JavaPreferencesStorage(CredentialsService.class);
        // }

        try {
            // Try to use the platform-native secure storage
            SecureStorageService secureStorage = new PlatformSecureStorage();
            logger.info("Using platform native secure storage for credentials");
            return secureStorage;
        } catch (Exception e) {
            // Fall back to Java Preferences if platform secure storage is not available
            logger.warn("Platform secure storage unavailable, falling back to Java Preferences: {}", e.getMessage());
            return new JavaPreferencesStorage(CredentialsService.class);
        }
    }

    // Optional static factory method if needed later
    public static AppModule create() {
       return new AppModule();
    }
}
