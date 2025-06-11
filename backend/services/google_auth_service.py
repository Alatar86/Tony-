"""
Google Authentication Service

This service handles OAuth 2.0 authentication with Google for Gmail API access.
"""

import importlib.resources
import json
import logging
import os
from typing import Any, Dict, Optional

from google.auth.exceptions import RefreshError
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow

from ..util.config_manager import ConfigurationManager
from ..util.exceptions import AuthError, ConfigError
from ..util.secure_token_storage import SecureTokenStorage

logger = logging.getLogger(__name__)


class GoogleAuthService:
    """
    Service responsible for handling OAuth 2.0 authentication with Google.
    """

    def __init__(self, config_manager: ConfigurationManager, token_storage: SecureTokenStorage) -> None:
        """
        Initialize the GoogleAuthService.

        Args:
            config_manager: ConfigurationManager instance for accessing configuration
            token_storage: SecureTokenStorage instance for managing OAuth tokens
        """
        self.config_manager = config_manager
        self.token_storage = token_storage
        self.credentials: Optional[Credentials] = None

        try:
            # Configuration values
            scopes_str = self.config_manager.get("Google", "scopes")
            self.scopes = scopes_str.split(",") if scopes_str else []

            logger.info("GoogleAuthService initialized.")
            logger.info(f"Scopes: {self.scopes}")
        except Exception as e:
            logger.error(f"Error initializing GoogleAuthService: {e}")
            raise ConfigError(
                f"Failed to initialize GoogleAuthService: {str(e)}"
            ) from e

    def get_credentials(self) -> Optional[Credentials]:
        """
        Get valid credentials for Gmail API access.

        Returns:
            Credentials: Google OAuth credentials object

        Raises:
            AuthError: If authentication fails
            ConfigError: If configuration is invalid
        """
        try:
            # Check if we already have valid credentials loaded
            if self.credentials and self.credentials.valid:
                logger.debug("Using already loaded valid credentials")
                return self.credentials

            # Try to load credentials from storage
            token_data = self.token_storage.load_token()
            if token_data:
                logger.info("Loading existing token from secure storage")
                try:
                    # Handle both string and dict formats
                    if isinstance(token_data, str):
                        token_dict = json.loads(token_data)
                    else:
                        token_dict = token_data

                    self.credentials = Credentials.from_authorized_user_info(
                        token_dict,
                        self.scopes,
                    )
                except Exception as e:
                    logger.error(f"Error parsing token data: {e}")
                    raise AuthError(f"Failed to parse stored token: {str(e)}") from e

            # If credentials are expired but we have a refresh token, refresh them
            if (
                self.credentials
                and self.credentials.expired
                and self.credentials.refresh_token
            ):
                try:
                    logger.info("Refreshing expired token")
                    self.credentials.refresh(Request())
                    # Save the refreshed credentials
                    self.token_storage.save_token(self.credentials.to_json())
                except RefreshError as e:
                    logger.error(f"Token refresh failed: {e}")
                    logger.info("Will start new OAuth flow")
                    self.credentials = None
                except Exception as e:
                    logger.error(f"Unexpected error during token refresh: {e}")
                    raise AuthError(f"Token refresh failed: {str(e)}") from e

            # If no valid credentials exist, run the OAuth flow
            if not self.credentials or not self.credentials.valid:
                logger.info("No valid credentials available. Authentication required.")
                return None

            return self.credentials

        except (AuthError, ConfigError):
            # Re-raise custom exceptions
            raise
        except Exception as e:
            logger.error(f"Authentication failed: {e}")
            raise AuthError(f"Authentication failed: {str(e)}") from e

    def initiate_auth_flow(self) -> bool:
        """
        Explicitly trigger the OAuth 2.0 authorization flow.

        Returns:
            bool: True if flow completed successfully, False otherwise

        Raises:
            AuthError: If authentication flow fails
            ConfigError: If client secrets file is missing or invalid
        """
        try:
            self._run_auth_flow()
            return self.credentials is not None and self.credentials.valid
        except (AuthError, ConfigError):
            # Re-raise custom exceptions
            raise
        except Exception as e:
            logger.error(f"OAuth flow failed: {e}")
            raise AuthError(f"OAuth flow failed: {str(e)}") from e

    def check_auth_status(self) -> bool:
        """
        Check if we have valid authentication.

        Returns:
            bool: True if authenticated with valid credentials, False otherwise
        """
        try:
            credentials = self.get_credentials()
            return credentials is not None and credentials.valid
        except Exception as e:
            logger.error(f"Auth status check failed: {e}")
            return False

    def _run_auth_flow(self) -> None:
        """
        Run the OAuth 2.0 authorization flow to get new credentials.

        Raises:
            ConfigError: If client secrets file is missing or invalid
            AuthError: If the OAuth flow fails
        """
        try:
            # Try to load client secrets from environment variables first
            client_secrets_config = self._load_client_secrets_from_env()

            # If we have client secrets from environment variables, use them
            if client_secrets_config:
                logger.info("Using client secrets from environment variables")
                flow = InstalledAppFlow.from_client_config(
                    client_secrets_config,
                    self.scopes,
                    redirect_uri="http://localhost:0/oauth2callback",
                )
            else:
                # Fall back to package resources if environment variables not set
                logger.warning(
                    "Environment variables for client secrets not found, "
                    "falling back to package resources",
                )
                try:
                    # Python 3.9+ way:
                    # traverses(<package>).joinpath(<resource>).open(<mode>)
                    resource_path = importlib.resources.files(
                        "backend.resources",
                    ).joinpath("client_secret.json")

                    with resource_path.open("r") as client_secrets_stream:
                        logger.info(
                            "Loading client secrets from package resource: "
                            "backend/resources/client_secret.json",
                        )
                        client_secrets_config = json.load(client_secrets_stream)
                        flow = InstalledAppFlow.from_client_config(
                            client_secrets_config,
                            self.scopes,
                            redirect_uri="http://localhost:0/oauth2callback",
                        )
                except FileNotFoundError as e:
                    error_msg = (
                        "Bundled client_secret.json resource not found in "
                        "backend.resources package."
                    )
                    logger.error(error_msg)
                    raise ConfigError(error_msg) from e
                except Exception as e:
                    error_msg = f"Error loading/parsing bundled client_secret.json: {e}"
                    logger.error(error_msg)
                    raise ConfigError(error_msg) from e

            # Configure the flow with custom parameters
            flow.oauth2session.redirect_uri = "http://localhost:0/oauth2callback"

            # Start the local server with improved user experience
            logger.info("Starting OAuth flow with local redirect server")

            # Create a success page with better UX
            success_message = """
            <html>
            <head>
                <title>Authentication Successful</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .success { color: #28a745; font-size: 24px; margin-bottom: 20px; }
                    .info { color: #6c757d; font-size: 16px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="success">✓ Authentication Successful!</div>
                    <div class="info">
                        You have successfully authorized the Email Agent to access your Gmail account.
                        <br><br>
                        You can now close this tab and return to the application.
                    </div>
                </div>
            </body>
            </html>
            """

            # Run the OAuth flow with the local server
            self.credentials = flow.run_local_server(
                port=0,  # Use any available port
                authorization_prompt_message="",
                success_message=success_message,
                open_browser=True,
            )

            # Save the credentials to secure storage
            if self.credentials:
                self.token_storage.save_token(self.credentials.to_json())
                logger.info("OAuth flow completed successfully and credentials saved")
            else:
                raise AuthError("OAuth flow completed but no credentials were obtained")

        except Exception as e:
            error_msg = f"OAuth flow failed: {str(e)}"
            logger.error(error_msg)
            raise AuthError(error_msg) from e

    def _load_client_secrets_from_env(self) -> Optional[Dict[str, Any]]:
        """
        Load client secrets from environment variables.

        Returns:
            dict: Client secrets configuration or None if not available

        Expected environment variables:
            GOOGLE_CLIENT_ID: OAuth client ID
            GOOGLE_CLIENT_SECRET: OAuth client secret
        """
        client_id = os.getenv("GOOGLE_CLIENT_ID")
        client_secret = os.getenv("GOOGLE_CLIENT_SECRET")

        if not client_id or not client_secret:
            logger.debug("Google client credentials not found in environment variables")
            return None

        logger.info("Found Google client credentials in environment variables")
        return {
            "installed": {
                "client_id": client_id,
                "client_secret": client_secret,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                "redirect_uris": ["http://localhost"],
            }
        }
