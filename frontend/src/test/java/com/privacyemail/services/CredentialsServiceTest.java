package com.privacyemail.services;

import com.privacyemail.security.SecretNotFoundException;
import com.privacyemail.security.SecureStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CredentialsService class
 */
class CredentialsServiceTest {

    @Mock
    private SecureStorageService mockSecureStorage;

    private CredentialsService credentialsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        credentialsService = new CredentialsService(mockSecureStorage);
    }

    @Test
    void saveApiKey_ValidInput_DelegatesToSecureStorage() {
        // Arrange
        String serviceName = "test-service";
        String apiKey = "test-api-key";

        // Act
        credentialsService.saveApiKey(serviceName, apiKey);

        // Assert
        verify(mockSecureStorage).saveSecret(serviceName, apiKey);
    }

    @Test
    void saveApiKey_NullApiKey_SavesEmptyString() {
        // Arrange
        String serviceName = "test-service";
        String apiKey = null;

        // Act
        credentialsService.saveApiKey(serviceName, apiKey);

        // Assert
        verify(mockSecureStorage).saveSecret(serviceName, "");
    }

    @Test
    void saveApiKey_InvalidServiceName_DoesNotCallStorage() {
        // Arrange
        String invalidServiceName = "";
        String apiKey = "test-api-key";

        // Act
        credentialsService.saveApiKey(invalidServiceName, apiKey);

        // Assert
        verifyNoInteractions(mockSecureStorage);
    }

    @Test
    void saveApiKey_SecurityException_HandlesExceptionGracefully() {
        // Arrange
        String serviceName = "test-service";
        String apiKey = "test-api-key";
        doThrow(new SecurityException("Test security exception")).when(mockSecureStorage).saveSecret(anyString(), anyString());

        // Act - should not throw exception
        credentialsService.saveApiKey(serviceName, apiKey);

        // Assert - verify the method was called and exception was handled
        verify(mockSecureStorage).saveSecret(serviceName, apiKey);
    }

    @Test
    void loadApiKey_ValidKey_ReturnsStoredValue() throws SecretNotFoundException {
        // Arrange
        String serviceName = "test-service";
        String expectedApiKey = "test-api-key";
        when(mockSecureStorage.loadSecret(serviceName)).thenReturn(expectedApiKey);

        // Act
        String result = credentialsService.loadApiKey(serviceName);

        // Assert
        assertEquals(expectedApiKey, result);
        verify(mockSecureStorage).loadSecret(serviceName);
    }

    @Test
    void loadApiKey_KeyNotFound_ReturnsNull() throws SecretNotFoundException {
        // Arrange
        String serviceName = "non-existent-service";
        when(mockSecureStorage.loadSecret(serviceName)).thenThrow(new SecretNotFoundException(serviceName));

        // Act
        String result = credentialsService.loadApiKey(serviceName);

        // Assert
        assertNull(result);
        verify(mockSecureStorage).loadSecret(serviceName);
    }

    @Test
    void loadApiKey_SecurityException_ReturnsNull() throws SecretNotFoundException {
        // Arrange
        String serviceName = "test-service";
        when(mockSecureStorage.loadSecret(serviceName)).thenThrow(new SecurityException("Test security exception"));

        // Act
        String result = credentialsService.loadApiKey(serviceName);

        // Assert
        assertNull(result);
        verify(mockSecureStorage).loadSecret(serviceName);
    }

    @Test
    void loadApiKey_InvalidServiceName_ReturnsNull() {
        // Arrange
        String invalidServiceName = "";

        // Act
        String result = credentialsService.loadApiKey(invalidServiceName);

        // Assert
        assertNull(result);
        verifyNoInteractions(mockSecureStorage);
    }

    @Test
    void removeApiKey_ValidKey_DelegatesToSecureStorage() {
        // Arrange
        String serviceName = "test-service";

        // Act
        credentialsService.removeApiKey(serviceName);

        // Assert
        verify(mockSecureStorage).removeSecret(serviceName);
    }

    @Test
    void removeApiKey_InvalidServiceName_DoesNotCallStorage() {
        // Arrange
        String invalidServiceName = "";

        // Act
        credentialsService.removeApiKey(invalidServiceName);

        // Assert
        verifyNoInteractions(mockSecureStorage);
    }

    @Test
    void removeApiKey_SecurityException_HandlesExceptionGracefully() {
        // Arrange
        String serviceName = "test-service";
        doThrow(new SecurityException("Test security exception")).when(mockSecureStorage).removeSecret(anyString());

        // Act - should not throw exception
        credentialsService.removeApiKey(serviceName);

        // Assert - verify the method was called and exception was handled
        verify(mockSecureStorage).removeSecret(serviceName);
    }
}
