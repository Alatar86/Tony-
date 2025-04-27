package com.privacyemail.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlatformSecureStorage implementation.
 *
 * Note: These tests interact with the actual system credential store.
 * In real-world tests, it might be better to use a mock secret store.
 */
public class PlatformSecureStorageTest {

    private PlatformSecureStorage secureStorage;
    private static final String TEST_KEY_PREFIX = "com.privacyemail.test.";
    private String testKey;

    @BeforeEach
    public void setUp() throws Exception {
        secureStorage = new PlatformSecureStorage();
        // Generate a unique test key to avoid race conditions in concurrent tests
        testKey = TEST_KEY_PREFIX + java.util.UUID.randomUUID().toString();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up the test key, ignoring any exceptions if the key doesn't exist
        try {
            secureStorage.removeSecret(testKey);
        } catch (Exception e) {
            // Ignore cleanup failure
        }
    }

    @Test
    public void testSaveAndLoadSecret() throws Exception {
        // Arrange
        String testSecret = "testSecret";

        // Act
        secureStorage.saveSecret(testKey, testSecret);
        String retrievedSecret = secureStorage.loadSecret(testKey);

        // Assert
        assertEquals(testSecret, retrievedSecret, "Retrieved secret should match saved secret");
    }

    @Test
    public void testSaveAndRemoveSecret() throws Exception {
        // Arrange
        String testSecret = "testSecret";

        // Act
        secureStorage.saveSecret(testKey, testSecret);
        secureStorage.removeSecret(testKey);

        // Assert
        String retrievedSecret = secureStorage.loadSecret(testKey);
        assertEquals("", retrievedSecret, "Retrieved secret should be empty string after removal");
    }

    @Test
    public void testSaveNullSecret() throws Exception {
        // Act
        secureStorage.saveSecret(testKey, null);
        String retrievedSecret = secureStorage.loadSecret(testKey);

        // Assert
        assertEquals("", retrievedSecret, "Retrieved secret should be empty string when null was saved");
    }

    @Test
    public void testLoadNonExistentSecret() throws Exception {
        // Act & Assert
        String retrievedSecret = secureStorage.loadSecret(testKey + ".nonexistent");
        assertEquals("", retrievedSecret, "Retrieved non-existent secret should be empty string");
    }

    @Test
    public void testNullOrEmptyKey() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            secureStorage.saveSecret(null, "test");
        }, "Null key should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            secureStorage.saveSecret("", "test");
        }, "Empty key should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            secureStorage.loadSecret(null);
        }, "Null key should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            secureStorage.removeSecret(null);
        }, "Null key should throw IllegalArgumentException");
    }

    @Test
    public void testSpecialCharacters() throws Exception {
        // Arrange
        String testSecret = "!@#$%^&*()_+=-[]{}|;':,./<>?";

        // Act
        secureStorage.saveSecret(testKey, testSecret);
        String retrievedSecret = secureStorage.loadSecret(testKey);

        // Assert
        assertEquals(testSecret, retrievedSecret, "Special characters should be preserved");
    }
}
