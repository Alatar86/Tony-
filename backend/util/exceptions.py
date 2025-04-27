"""
Custom exceptions for Privacy-Focused Email Agent

This module defines a hierarchy of custom exceptions used throughout the application.
"""


class ServiceError(Exception):
    """
    Base exception class for all service errors.

    Attributes:
        message (str): Error message
        code (int): HTTP status code to use in API responses
        details (dict): Additional details about the error
    """

    def __init__(self, message, code=500, details=None):
        """
        Initialize a new ServiceError.

        Args:
            message (str): Error message
            code (int): HTTP status code to use in API responses (default: 500)
            details (dict): Additional details about the error (default: None)
        """
        super().__init__(message)
        self.message = message
        self.code = code
        self.details = details or {}

    def to_dict(self):
        """
        Convert the exception to a dictionary for JSON serialization.

        Returns:
            dict: Dictionary representation of the error
        """
        result = {
            "error": self.message,
            "code": self.code,
        }

        if self.details:
            result["details"] = self.details

        return result


class AuthError(ServiceError):
    """
    Exception raised for authentication and authorization errors.
    """

    def __init__(self, message="Authentication error", code=401, details=None):
        """
        Initialize a new AuthError.

        Args:
            message (str): Error message (default: "Authentication error")
            code (int): HTTP status code (default: 401)
            details (dict): Additional details about the error (default: None)
        """
        super().__init__(message, code, details)


class GmailApiError(ServiceError):
    """
    Exception raised for errors related to Gmail API interactions.
    """

    def __init__(self, message="Gmail API error", code=500, details=None):
        """
        Initialize a new GmailApiError.

        Args:
            message (str): Error message (default: "Gmail API error")
            code (int): HTTP status code (default: 500)
            details (dict): Additional details about the error (default: None)
        """
        super().__init__(message, code, details)


class OllamaError(ServiceError):
    """
    Exception raised for errors related to Ollama API interactions.
    """

    def __init__(self, message="Ollama service error", code=503, details=None):
        """
        Initialize a new OllamaError.

        Args:
            message (str): Error message (default: "Ollama service error")
            code (int): HTTP status code (default: 503)
            details (dict): Additional details about the error (default: None)
        """
        super().__init__(message, code, details)


class ConfigError(ServiceError):
    """
    Exception raised for configuration-related errors.
    """

    def __init__(self, message="Configuration error", code=500, details=None):
        """
        Initialize a new ConfigError.

        Args:
            message (str): Error message (default: "Configuration error")
            code (int): HTTP status code (default: 500)
            details (dict): Additional details about the error (default: None)
        """
        super().__init__(message, code, details)


class ValidationError(ServiceError):
    """
    Exception raised for input validation errors.
    """

    def __init__(self, message="Validation error", code=400, details=None):
        """
        Initialize a new ValidationError.

        Args:
            message (str): Error message (default: "Validation error")
            code (int): HTTP status code (default: 400)
            details (dict): Additional details about the error (default: None)
        """
        super().__init__(message, code, details)


class NotFoundError(ServiceError):
    """
    Exception raised when a requested resource is not found.
    """

    def __init__(self, message="Resource not found", code=404, details=None):
        """
        Initialize a new NotFoundError.

        Args:
            message (str): Error message (default: "Resource not found")
            code (int): HTTP status code (default: 404)
            details (dict): Additional details about the error (default: None)
        """
        super().__init__(message, code, details)
