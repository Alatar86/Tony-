package com.privacyemail.security;

import com.microsoft.credentialstorage.SecretStore;
import com.microsoft.credentialstorage.StorageProvider;
import com.microsoft.credentialstorage.StorageProvider.SecureOption;
import com.microsoft.credentialstorage.model.StoredCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SecureStorageService that uses the Microsoft credential-secure-storage-for-java
 * library to leverage native OS credential stores:
 * - Windows: Windows Credential Manager
 * - macOS: Keychain
 * - Linux: Secret Service API / Gnome Keyring / KWallet
 *
 * This implementation provides enhanced security by utilizing platform-native secure storage
 * mechanisms which typically offer encryption at rest and access controls.
 */
public class PlatformSecureStorage implements SecureStorageService {

    private static final Logger logger = LoggerFactory.getLogger(PlatformSecureStorage.class);
    private final SecretStore<StoredCredential> secretStore;
    private static final String DUMMY_USERNAME = "app-secret"; // Username field required by the StoredCredential

    /**
     * Creates a PlatformSecureStorage instance that uses the appropriate OS-specific
     * credential storage mechanism.
     *
     * @throws SecurityException if no suitable secure storage provider is available
     */
    public PlatformSecureStorage() throws SecurityException {
        try {
            // Get the appropriate credential storage for the current platform
            // The 'true' parameter allows fallback to less secure options if necessary
            this.secretStore = StorageProvider.getCredentialStorage(true, SecureOption.REQUIRED);

            if (this.secretStore == null) {
                String errorMsg = "No suitable secure storage provider found for this platform";
                logger.error(errorMsg);
                throw new SecurityException(errorMsg);
            }

            logger.info("PlatformSecureStorage initialized with provider: {}",
                      this.secretStore.getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Failed to initialize secure storage provider", e);
            throw new SecurityException("Failed to initialize secure storage provider: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a PlatformSecureStorage with a specific secret store.
     * Primarily intended for testing or specialized use cases.
     *
     * @param secretStore The secret store to use
     */
    public PlatformSecureStorage(SecretStore<StoredCredential> secretStore) {
        if (secretStore == null) {
            throw new IllegalArgumentException("Secret store cannot be null");
        }
        this.secretStore = secretStore;
        logger.info("PlatformSecureStorage initialized with custom secret store: {}",
                  secretStore.getClass().getSimpleName());
    }

    @Override
    public void saveSecret(String key, String secret) throws SecurityException {
        if (key == null || key.trim().isEmpty()) {
            logger.error("Key cannot be null or empty");
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        // Handle null or empty secret by removing the key
        if (secret == null || secret.isEmpty()) {
            logger.info("Removing secret for key due to null/empty value: {}", key);
            try {
                // Delete the credential using the secret store
                secretStore.delete(key);
            } catch (Exception e) {
                // Ignore exceptions if the key doesn't exist
                logger.debug("Key didn't exist when trying to remove null/empty secret: {}", key);
            }
            return;
        }

        try {
            // Create a StoredCredential with the dummy username and the secret as password
            StoredCredential credential = new StoredCredential(DUMMY_USERNAME, secret.toCharArray());

            // Store the credential using the secret store
            secretStore.add(key, credential);
            logger.info("Secret saved for key: {}", key);

            // Clear the credential for security (zeros out the char[] to minimize exposure)
            credential.clear();
        } catch (Exception e) {
            logger.error("Failed to save secret for key: {}", key, e);
            throw new SecurityException("Failed to save secret: " + e.getMessage(), e);
        }
    }

    @Override
    public String loadSecret(String key) throws SecretNotFoundException, SecurityException {
        if (key == null || key.trim().isEmpty()) {
            logger.error("Key cannot be null or empty");
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        try {
            // Read the credential using the secret store
            StoredCredential credential = secretStore.get(key);

            if (credential != null) {
                // Extract the secret from the password field
                String secret = new String(credential.getPassword());
                logger.debug("Secret loaded successfully for key: {}", key);

                // Clear the credential for security
                credential.clear();

                return secret;
            } else {
                // Return empty string for non-existent keys to match handling of null/empty secrets
                logger.info("No secret found for key: {}", key);
                return "";
            }
        } catch (Exception e) {
            // If the exception indicates the key wasn't found, return empty string
            if (e.getMessage() != null && (
                    e.getMessage().contains("Element not found") ||
                    e.getMessage().contains("not found"))) {
                logger.info("No secret found for key: {}", key);
                return "";
            }

            logger.error("Failed to load secret for key: {}", key, e);
            throw new SecurityException("Failed to load secret: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeSecret(String key) throws SecurityException {
        if (key == null || key.trim().isEmpty()) {
            logger.error("Key cannot be null or empty for removal");
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        try {
            // Delete the credential using the secret store
            secretStore.delete(key);
            logger.info("Secret removed for key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to remove secret for key: {}", key, e);
            throw new SecurityException("Failed to remove secret: " + e.getMessage(), e);
        }
    }
}
