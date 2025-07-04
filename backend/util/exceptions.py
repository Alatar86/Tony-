"""
Custom exceptions for Privacy-Focused Email Agent

This module defines a hierarchy of custom exceptions used throughout the application.
"""
from typing import Any, Dict, Optional


class ServiceError(Exception):
    """
    Base exception class for all service errors.

    Attributes:
        message (str): Error message
        code (int): HTTP status code to use in API responses
        details (dict): Additional details about the error
    """

    def __init__(
        self,
        message: str,
        code: int = 500,
        details: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Initialize a new ServiceError.

        Args:
            message (str): Error description
            code (int): HTTP status code (default: 500)
            details (Optional[Dict]): Additional error details
        """
        super().__init__(message)
        self.message = message
        self.code = code
        self.details = details or {}

    def to_dict(self) -> Dict[str, Any]:
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

    def __init__(
        self,
        message: str = "Authentication error",
        code: int = 401,
        details: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Initialize a new AuthError.

        Args:
            message (str): Error message
            code (int): HTTP status code (default: 401)
            details (Optional[Dict]): Additional error details
        """
        super().__init__(message, code, details)


class GmailApiError(ServiceError):
    """
    Exception raised for errors related to Gmail API interactions.
    """

    def __init__(
        self,
        message: str = "Gmail API error",
        code: int = 500,
        details: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Initialize a new GmailApiError.

        Args:
            message (str): Error message
            code (int): HTTP status code (default: 500)
            details (Optional[Dict]): Additional error details
        """
        super().__init__(message, code, details)


class OllamaError(ServiceError):
    """
    Exception raised for Ollama service errors.
    """

    def __init__(
        self,
        message: str = "Ollama service error",
        code: int = 503,
        details: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Initialize a new OllamaError.

        Args:
            message (str): Error message
            code (int): HTTP status code (default: 503)
            details (Optional[Dict]): Additional error details
        """
        super().__init__(message, code, details)


class ConfigError(ServiceError):
    """
    Exception raised for configuration errors.
    """

    def __init__(
        self,
        message: str = "Configuration error",
        code: int = 500,
        details: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Initialize a new ConfigError.

        Args:
            message (str): Error message
            code (int): HTTP status code (default: 500)
            details (Optional[Dict]): Additional error details
        """
        super().__init__(message, code, details)


class ValidationError(ServiceError):
    """
    Exception raised for validation errors.
    """

    def __init__(
        self,
        message: str = "Validation error",
        code: int = 400,
        details: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Initialize a new ValidationError.

        Args:
            message (str): Error message
            code (int): HTTP status code (default: 400)
            details (Optional[Dict]): Additional error details
        """
        super().__init__(message, code, details)


class NotFoundError(ServiceError):
    """
    Exception raised when a resource is not found.
    """

    def __init__(
        self,
        message: str = "Resource not found",
        code: int = 404,
        details: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Initialize a new NotFoundError.

        Args:
            message (str): Error message
            code (int): HTTP status code (default: 404)
            details (Optional[Dict]): Additional error details
        """
        super().__init__(message, code, details)
