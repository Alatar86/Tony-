"""
SecureTokenStorage for Privacy-Focused Email Agent

This utility class provides secure storage for OAuth tokens.
Uses keyring for OS integration when available,
with fallback to file-based storage.
"""

import json
import logging
import os
from pathlib import Path

logger = logging.getLogger(__name__)


class SecureTokenStorage:
    """
    Manages secure storage of OAuth tokens, using keyring when available.
    """

    def __init__(self, config_manager):
        """
        Initialize the SecureTokenStorage.

        Args:
            config_manager: ConfigurationManager instance for accessing configuration
        """
        self.config_manager = config_manager
        self.token_cache_path = config_manager.get("Google", "token_cache_path")

        # Service name for keyring
        self.service_name = "PrivacyFocusedEmailAgent"

        # Check if keyring is available
        self.use_keyring = False
        try:
            import keyring

            self.keyring = keyring
            self.use_keyring = True
            logger.info("Using keyring for secure token storage")
        except ImportError:
            logger.warning("Keyring not available, falling back to file storage")
            # Ensure token directory exists
            os.makedirs(os.path.dirname(self.token_cache_path), exist_ok=True)

    def save_token(self, token_data):
        """
        Save token data securely.

        Args:
            token_data: Token data (dict or JSON string)

        Returns:
            bool: True if saved successfully, False otherwise
        """
        try:
            # Convert to JSON string if dict
            if isinstance(token_data, dict):
                token_data = json.dumps(token_data)

            if self.use_keyring:
                # Store in OS keyring
                self.keyring.set_password(self.service_name, "oauth_token", token_data)
            else:
                # Store in file
                token_file = Path(self.token_cache_path) / "token.json"
                with open(token_file, "w") as f:
                    f.write(token_data)
                logger.debug(f"Token saved to file: {token_file}")

            logger.info("Token saved successfully")
            return True
        except Exception as e:
            logger.error(f"Failed to save token: {e}")
            return False

    def load_token(self):
        """
        Load token data from secure storage.

        Returns:
            dict: Token data dictionary or None if not found/invalid
        """
        try:
            token_data = None

            if self.use_keyring:
                # Retrieve from OS keyring
                token_data = self.keyring.get_password(self.service_name, "oauth_token")
            else:
                # Retrieve from file
                token_file = Path(self.token_cache_path) / "token.json"
                if os.path.exists(token_file):
                    with open(token_file, "r") as f:
                        token_data = f.read()

            # Parse JSON string to dict if we got a string
            if token_data and isinstance(token_data, str):
                return json.loads(token_data)

            return token_data
        except Exception as e:
            logger.error(f"Failed to load token: {e}")
            return None
