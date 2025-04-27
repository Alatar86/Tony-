package com.privacyemail.security;

/**
 * Interface for secure storage services that can store and retrieve sensitive credentials.
 * Implementations should use appropriate OS-specific secure storage mechanisms
 * (e.g., Windows Credential Manager, macOS Keychain, Linux Secret Service).
 */
public interface SecureStorageService {

    /**
     * Saves a secret value with the given key.
     *
     * @param key The unique identifier for the secret
     * @param secret The secret value to store
     * @throws SecurityException If the secret cannot be saved due to security restrictions
     */
    void saveSecret(String key, String secret) throws SecurityException;

    /**
     * Loads a secret value for the given key.
     *
     * @param key The unique identifier for the secret
     * @return The stored secret value
     * @throws SecretNotFoundException If no secret exists for the given key
     * @throws SecurityException If the secret cannot be accessed due to security restrictions
     */
    String loadSecret(String key) throws SecretNotFoundException, SecurityException;

    /**
     * Removes a secret with the given key.
     *
     * @param key The unique identifier for the secret to remove
     * @throws SecurityException If the secret cannot be removed due to security restrictions
     */
    void removeSecret(String key) throws SecurityException;
}
