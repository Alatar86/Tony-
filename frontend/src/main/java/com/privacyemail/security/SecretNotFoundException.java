package com.privacyemail.security;

/**
 * Exception thrown when a requested secret cannot be found in secure storage.
 */
public class SecretNotFoundException extends Exception {

    /**
     * Constructs a new SecretNotFoundException with the specified key.
     *
     * @param key The key for the secret that was not found
     */
    public SecretNotFoundException(String key) {
        super("Secret not found for key: " + key);
    }

    /**
     * Constructs a new SecretNotFoundException with a custom message.
     *
     * @param message The detail message
     */
    public SecretNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
