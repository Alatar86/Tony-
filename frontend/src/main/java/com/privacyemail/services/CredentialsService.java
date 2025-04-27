package com.privacyemail.services;

import com.google.inject.Inject;
import com.privacyemail.security.SecretNotFoundException;
import com.privacyemail.security.SecureStorageService;
import com.privacyemail.security.JavaPreferencesStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for storing and retrieving sensitive credentials like API keys.
 *
 * This implementation uses a SecureStorageService interface for the actual storage mechanisms,
 * allowing for different storage backends depending on the platform and security requirements.
 *
 * <h2>Security Notice</h2>
 * <p>
 * By default, this service uses JavaPreferencesStorage which stores data in Java Preferences API.
 * This implementation provides basic security but is NOT suitable for storing highly sensitive
 * information in production environments as it lacks strong encryption and can be vulnerable
 * to local system attacks.
 * </p>
 * <p>
 * For production use with sensitive information, consider implementing a more secure
 * SecureStorageService that uses platform-specific secure storage mechanisms such as:
 * <ul>
 *   <li>Windows Credential Manager</li>
 *   <li>macOS Keychain</li>
 *   <li>Linux Secret Service API / gnome-keyring</li>
 *   <li>Hardware Security Modules (HSMs)</li>
 * </ul>
 * </p>
 *
 * <h3>Dependency Architecture</h3>
 * <p>
 * This service follows the Dependency Inversion Principle by depending on the SecureStorageService
 * interface rather than concrete implementations. This provides several benefits:
 * <ul>
 *   <li>Platform-specific storage implementations can be easily swapped</li>
 *   <li>Mock implementations can be injected for testing</li>
 *   <li>Storage security can be upgraded without changing client code</li>
 * </ul>
 * </p>
 */
public class CredentialsService implements ICredentialsService {

    private static final Logger logger = LoggerFactory.getLogger(CredentialsService.class);

    // The secure storage implementation used for storing credentials
    private final SecureStorageService secureStorage;

    /**
     * Creates a CredentialsService with a specified secure storage implementation.
     *
     * This constructor allows you to provide your own SecureStorageService implementation,
     * which is the recommended approach for production environments requiring
     * higher security levels.
     *
     * @param secureStorage The secure storage implementation to use
     */
    @Inject
    public CredentialsService(SecureStorageService secureStorage) {
        this.secureStorage = secureStorage;
        logger.info("CredentialsService initialized with secure storage: {}",
                    secureStorage.getClass().getSimpleName());
    }

    /**
     * Default constructor that uses JavaPreferencesStorage for backward compatibility.
     *
     * <p><strong>Security Warning:</strong> This constructor uses JavaPreferencesStorage
     * which provides only basic security. It is suitable for development and testing,
     * but NOT recommended for storing sensitive information in production environments.</p>
     *
     * <p>For production use, prefer the constructor that accepts a more secure
     * SecureStorageService implementation, such as one that integrates with the
     * operating system's secure credential storage.</p>
     *
     * <p>Example with a hypothetical more secure implementation:</p>
     * <pre>
     * // For Windows
     * CredentialsService credService = new CredentialsService(new WindowsCredentialManagerStorage());
     *
     * // For macOS
     * CredentialsService credService = new CredentialsService(new MacOSKeychainStorage());
     *
     * // For Linux
     * CredentialsService credService = new CredentialsService(new LinuxSecretServiceStorage());
     * </pre>
     */
    public CredentialsService() {
        // Use JavaPreferencesStorage with the CredentialsService class for backward compatibility
        this(new JavaPreferencesStorage(CredentialsService.class));
    }

    /**
     * Saves an API key for a given service name.
     * Replaces any existing key for the same service name.
     *
     * @param serviceName The unique name of the service (e.g., "openai", "google_oauth_refresh_token").
     * @param apiKey      The API key or credential to store.
     */
    public void saveApiKey(String serviceName, String apiKey) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            logger.error("Service name cannot be null or empty.");
            // Consider throwing IllegalArgumentException
            return;
        }
        if (apiKey == null) {
            // Allow storing null/empty string explicitly? For now, log warning.
            logger.warn("Saving null API key for service: {}", serviceName);
            // Decide if we should perhaps remove the key instead?
            // secureStorage.removeSecret(serviceName); return;
        }

        try {
            secureStorage.saveSecret(serviceName, apiKey == null ? "" : apiKey);
            logger.info("API key saved for service: {}", serviceName);
        } catch (SecurityException e) {
            logger.error("SecurityException while saving API key for service: {}", serviceName, e);
            // Handle exception appropriately (e.g., show error to user)
        }
    }

    /**
     * Loads an API key for a given service name.
     *
     * @param serviceName The unique name of the service.
     * @return The stored API key, or null if not found or an error occurred.
     */
    public String loadApiKey(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            logger.error("Service name cannot be null or empty.");
            return null;
        }

        try {
            String apiKey = secureStorage.loadSecret(serviceName);
            logger.debug("API key loaded successfully for service: {}", serviceName);
            return apiKey;
        } catch (SecretNotFoundException e) {
            logger.info("No API key found for service: {}", serviceName);
            return null;
        } catch (SecurityException e) {
            logger.error("SecurityException while loading API key for service: {}", serviceName, e);
            return null;
        }
    }

    /**
     * Removes an API key for a given service name.
     *
     * @param serviceName The unique name of the service.
     */
    public void removeApiKey(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            logger.error("Service name cannot be null or empty for removal.");
            return;
        }
        try {
            secureStorage.removeSecret(serviceName);
            logger.info("API key removed for service: {}", serviceName);
        } catch (SecurityException e) {
            logger.error("SecurityException while removing API key for service: {}", serviceName, e);
        }
    }
}
