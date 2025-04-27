package com.privacyemail.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.prefs.Preferences;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JavaPreferencesStorage
 */
class JavaPreferencesStorageTest {

    private JavaPreferencesStorage storage;
    private String testKey;
    private String testSecret;

    @Mock
    private Preferences mockPrefs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a test instance with actual preferences
        storage = new JavaPreferencesStorage(JavaPreferencesStorageTest.class);

        // Generate unique test key for each test to avoid conflicts
        testKey = "test_key_" + UUID.randomUUID().toString();
        testSecret = "test_secret_value";
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        try {
            storage.removeSecret(testKey);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void saveAndLoadSecret_Success() throws SecretNotFoundException {
        // Act - Save the secret
        storage.saveSecret(testKey, testSecret);

        // Act - Load the secret
        String loadedSecret = storage.loadSecret(testKey);

        // Assert
        assertEquals(testSecret, loadedSecret, "Loaded secret should match the saved one");
    }

    @Test
    void loadSecret_NonExistentKey_ThrowsException() {
        // Act & Assert
        assertThrows(SecretNotFoundException.class, () -> {
            storage.loadSecret("non_existent_key_" + UUID.randomUUID());
        }, "Should throw SecretNotFoundException for non-existent key");
    }

    @Test
    void removeSecret_RemovesExistingKey() throws SecretNotFoundException {
        // Arrange
        storage.saveSecret(testKey, testSecret);

        // Verify it exists first
        assertNotNull(storage.loadSecret(testKey));

        // Act
        storage.removeSecret(testKey);

        // Assert
        assertThrows(SecretNotFoundException.class, () -> {
            storage.loadSecret(testKey);
        }, "Secret should be removed and not found");
    }

    @Test
    void saveSecret_NullOrEmptyKey_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            storage.saveSecret(null, testSecret);
        }, "Should throw IllegalArgumentException for null key");

        assertThrows(IllegalArgumentException.class, () -> {
            storage.saveSecret("", testSecret);
        }, "Should throw IllegalArgumentException for empty key");
    }

    @Test
    void saveSecret_NullSecret_SavesEmptyString() throws SecretNotFoundException {
        // Act
        storage.saveSecret(testKey, null);

        // Assert
        String loadedSecret = storage.loadSecret(testKey);
        assertEquals("", loadedSecret, "Null secret should be saved as empty string");
    }

    @Test
    void mockTest_WithMockedPreferences() throws SecretNotFoundException {
        // Arrange
        JavaPreferencesStorage mockStorage = new JavaPreferencesStorage(mockPrefs);

        // Setup mock behavior
        when(mockPrefs.absolutePath()).thenReturn("/mock/path");
        when(mockPrefs.get(eq(testKey), isNull())).thenReturn(testSecret);

        // Act
        mockStorage.saveSecret(testKey, testSecret);
        String result = mockStorage.loadSecret(testKey);

        // Assert
        assertEquals(testSecret, result);

        // Verify
        verify(mockPrefs).put(testKey, testSecret);
        try {
            verify(mockPrefs).flush();
        } catch (Exception e) {
            fail("Exception should not be thrown in mock verification");
        }
    }
}
