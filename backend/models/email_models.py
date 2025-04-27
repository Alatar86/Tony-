"""
Email Models for Privacy-Focused Email Agent

This module defines data models for working with email data.
"""

from dataclasses import dataclass
from typing import List


@dataclass
class EmailMetadata:
    """
    Model for email message metadata.
    Used in list views and for API responses.
    """

    id: str
    subject: str
    from_address: str
    date: str

    @classmethod
    def from_dict(cls, data: dict) -> "EmailMetadata":
        """
        Create an EmailMetadata instance from a dictionary.

        Args:
            data (dict): Dictionary containing metadata

        Returns:
            EmailMetadata: New instance
        """
        return cls(
            id=data["id"],
            subject=data.get("subject", "(No subject)"),
            from_address=data.get("from", "(Unknown sender)"),
            date=data.get("date", "(Unknown date)"),
        )


@dataclass
class EmailDetails:
    """
    Model for full email message details.
    Used in detail views and for API responses.
    """

    id: str
    subject: str
    from_address: str
    to_address: str
    date: str
    body: str

    @classmethod
    def from_dict(cls, data: dict) -> "EmailDetails":
        """
        Create an EmailDetails instance from a dictionary.

        Args:
            data (dict): Dictionary containing email details

        Returns:
            EmailDetails: New instance
        """
        return cls(
            id=data["id"],
            subject=data.get("subject", "(No subject)"),
            from_address=data.get("from", "(Unknown sender)"),
            to_address=data.get("to", "(Unknown recipient)"),
            date=data.get("date", "(Unknown date)"),
            body=data.get("body", "(No content)"),
        )


@dataclass
class SuggestionResponse:
    """
    Model for AI-generated reply suggestions.
    Used in suggestion views and for API responses.
    """

    suggestions: List[str]

    @classmethod
    def from_dict(cls, data: dict) -> "SuggestionResponse":
        """
        Create a SuggestionResponse instance from a dictionary.

        Args:
            data (dict): Dictionary containing suggestions

        Returns:
            SuggestionResponse: New instance
        """
        return cls(
            suggestions=data.get("suggestions", []),
        )
