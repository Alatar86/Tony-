package com.privacyemail.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Implementation of SecureStorageService that uses Java Preferences API.
 *
 * WARNING: This implementation does NOT provide strong encryption by default.
 * It relies on OS-level permissions and is primarily for obscurity,
 * not robust security against targeted attacks. Use only as a temporary
 * or backward compatibility solution.
 */
public class JavaPreferencesStorage implements SecureStorageService {

    private static final Logger logger = LoggerFactory.getLogger(JavaPreferencesStorage.class);

    private final Preferences prefs;

    /**
     * Creates a storage service using Java Preferences for the specified class.
     *
     * @param storageClass The class to associate the preferences node with
     */
    public JavaPreferencesStorage(Class<?> storageClass) {
        this.prefs = Preferences.userNodeForPackage(storageClass);
        logger.info("JavaPreferencesStorage initialized. Storage node: {}", prefs.absolutePath());
    }

    /**
     * Creates a storage service using a specific preferences node.
     *
     * @param preferencesNode The preferences node to use for storage
     */
    public JavaPreferencesStorage(Preferences preferencesNode) {
        this.prefs = preferencesNode;
        logger.info("JavaPreferencesStorage initialized with custom node: {}", prefs.absolutePath());
    }

    @Override
    public void saveSecret(String key, String secret) throws SecurityException {
        if (key == null || key.trim().isEmpty()) {
            logger.error("Key cannot be null or empty");
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        if (secret == null) {
            logger.warn("Saving null secret for key: {}", key);
            secret = ""; // Store empty string if secret is null
        }

        try {
            prefs.put(key, secret);
            prefs.flush(); // Ensure changes are written to persistent storage
            logger.info("Secret saved for key: {}", key);
        } catch (SecurityException e) {
            logger.error("SecurityException while saving secret for key: {}", key, e);
            throw e;
        } catch (BackingStoreException e) {
            logger.error("BackingStoreException while saving secret for key: {}", key, e);
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
            String secret = prefs.get(key, null); // Returns null if key doesn't exist
            if (secret != null) {
                logger.debug("Secret loaded successfully for key: {}", key);
                return secret;
            } else {
                logger.info("No secret found for key: {}", key);
                throw new SecretNotFoundException(key);
            }
        } catch (SecurityException e) {
            logger.error("SecurityException while loading secret for key: {}", key, e);
            throw e;
        }
    }

    @Override
    public void removeSecret(String key) throws SecurityException {
        if (key == null || key.trim().isEmpty()) {
            logger.error("Key cannot be null or empty for removal");
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        try {
            prefs.remove(key);
            prefs.flush();
            logger.info("Secret removed for key: {}", key);
        } catch (SecurityException e) {
            logger.error("SecurityException while removing secret for key: {}", key, e);
            throw e;
        } catch (BackingStoreException e) {
            logger.error("BackingStoreException while removing secret for key: {}", key, e);
            throw new SecurityException("Failed to remove secret: " + e.getMessage(), e);
        }
    }
}
