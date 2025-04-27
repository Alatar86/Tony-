"""
Unit tests for GoogleAuthService

This module contains tests for the GoogleAuthService class, focusing on
mocking external dependencies to ensure isolated testing.
"""

import json
import os
import unittest
from unittest.mock import patch, mock_open, MagicMock

from backend.services.google_auth_service import GoogleAuthService
from backend.util.exceptions import ConfigError


class TestGoogleAuthService(unittest.TestCase):
    """Test cases for GoogleAuthService"""

    def setUp(self):
        """Set up test fixtures"""
        # Create mock objects for dependencies
        self.mock_config_manager = MagicMock()
        self.mock_config_manager.get.return_value = "https://mail.google.com/,https://www.googleapis.com/auth/gmail.modify"
        
        self.mock_token_storage = MagicMock()
        
        # Create the service instance with mocked dependencies
        self.service = GoogleAuthService(self.mock_config_manager, self.mock_token_storage)

    def tearDown(self):
        """Tear down test fixtures"""
        # Clean up any environment variables we might have set
        if "GOOGLE_CLIENT_SECRET_JSON_CONTENT" in os.environ:
            del os.environ["GOOGLE_CLIENT_SECRET_JSON_CONTENT"]
        if "GOOGLE_CLIENT_SECRET_JSON_PATH" in os.environ:
            del os.environ["GOOGLE_CLIENT_SECRET_JSON_PATH"]

    @patch.dict(os.environ, {"GOOGLE_CLIENT_SECRET_JSON_CONTENT": '{"web": {"client_id": "test_id", "client_secret": "test_secret"}}'})
    def test_load_client_secrets_from_env_content(self):
        """Test loading client secrets from environment variable content"""
        # Call the method under test
        result = self.service._load_client_secrets_from_env()
        
        # Assert the expected result
        self.assertIsNotNone(result)
        self.assertEqual(result['web']['client_id'], 'test_id')
        self.assertEqual(result['web']['client_secret'], 'test_secret')

    @patch.dict(os.environ, {"GOOGLE_CLIENT_SECRET_JSON_CONTENT": 'invalid_json'})
    def test_load_client_secrets_from_env_invalid_json_content(self):
        """Test loading client secrets with invalid JSON content"""
        # Assert that the method raises the expected exception
        with self.assertRaises(ConfigError):
            self.service._load_client_secrets_from_env()

    @patch.dict(os.environ, {"GOOGLE_CLIENT_SECRET_JSON_PATH": "/path/to/secrets.json"})
    @patch("builtins.open", new_callable=mock_open, read_data='{"web": {"client_id": "file_id", "client_secret": "file_secret"}}')
    def test_load_client_secrets_from_env_path(self, mock_file):
        """Test loading client secrets from a file path specified in environment variable"""
        # Call the method under test
        result = self.service._load_client_secrets_from_env()
        
        # Assert the file was opened with the correct path
        mock_file.assert_called_once_with("/path/to/secrets.json", "r")
        
        # Assert the expected result
        self.assertIsNotNone(result)
        self.assertEqual(result['web']['client_id'], 'file_id')
        self.assertEqual(result['web']['client_secret'], 'file_secret')

    @patch.dict(os.environ, {"GOOGLE_CLIENT_SECRET_JSON_PATH": "/path/to/secrets.json"})
    @patch("builtins.open", side_effect=FileNotFoundError())
    def test_load_client_secrets_from_env_file_not_found(self, mock_file):
        """Test handling of missing client secrets file"""
        # Assert that the method raises the expected exception
        with self.assertRaises(ConfigError):
            self.service._load_client_secrets_from_env()

    @patch.dict(os.environ, {"GOOGLE_CLIENT_SECRET_JSON_PATH": "/path/to/secrets.json"})
    @patch("builtins.open", new_callable=mock_open, read_data='invalid_json')
    def test_load_client_secrets_from_env_invalid_json_file(self, mock_file):
        """Test handling of invalid JSON in client secrets file"""
        # Assert that the method raises the expected exception
        with self.assertRaises(ConfigError):
            self.service._load_client_secrets_from_env()

    def test_load_client_secrets_from_env_no_env_vars(self):
        """Test behavior when no environment variables are set"""
        # Call the method under test
        result = self.service._load_client_secrets_from_env()
        
        # Assert the expected result
        self.assertIsNone(result)


if __name__ == '__main__':
    unittest.main() 