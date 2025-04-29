"""
GoogleAuthService for Privacy-Focused Email Agent

This service handles OAuth 2.0 authentication with Google Gmail API.
It manages the authentication flow, token acquisition, storage, and refresh.
"""

import importlib.resources
import json
import logging
import os

from google.auth.exceptions import RefreshError
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow

from ..util.exceptions import AuthError, ConfigError

# Configure logger
logger = logging.getLogger(__name__)


class GoogleAuthService:
    """
    Service responsible for handling OAuth 2.0 authentication with Google.
    """

    def __init__(self, config_manager, token_storage):
        """
        Initialize the GoogleAuthService.

        Args:
            config_manager: ConfigurationManager instance for accessing configuration
            token_storage: SecureTokenStorage instance for managing OAuth tokens
        """
        self.config_manager = config_manager
        self.token_storage = token_storage
        self.credentials = None

        try:
            # Configuration values
            self.scopes = self.config_manager.get("Google", "scopes").split(",")

            logger.info("GoogleAuthService initialized.")
            logger.info(f"Scopes: {self.scopes}")
        except Exception as e:
            logger.error(f"Error initializing GoogleAuthService: {e}")
            raise ConfigError(
                f"Failed to initialize GoogleAuthService: {str(e)}"
            ) from e

    def get_credentials(self):
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

    def initiate_auth_flow(self):
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

    def check_auth_status(self):
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

    def _run_auth_flow(self):
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
                    error_msg = ("Bundled client_secret.json resource not found in "
                                "backend.resources package.")
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
                    body {
                        font-family: Arial, sans-serif;
                        text-align: center;
                        padding: 40px;
                        background-color: #f7f9fc;
                    }
                    .container {
                        background-color: white;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                        padding: 30px;
                        max-width: 500px;
                        margin: 0 auto;
                    }
                    h1 { color: #066adb; }
                    .success-icon {
                        color: #4CAF50;
                        font-size: 48px;
                    }
                    .message {
                        margin: 20px 0;
                        color: #333;
                    }
                    .close-button {
                        background-color: #066adb;
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        border-radius: 4px;
                        cursor: pointer;
                        font-size: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="success-icon">✓</div>
                    <h1>Authentication Successful!</h1>
                    <p class="message">
                        You have successfully authenticated with Google.
                    </p>
                    <p>You can now close this window and return to the application.</p>
                    <button class="close-button" onclick="window.close()">
                        Close Window
                    </button>
                    <script>
                        // Auto-close after 5 seconds
                        setTimeout(function() {
                            window.close();
                        }, 5000);
                    </script>
                </div>
            </body>
            </html>
            """

            # Run the flow with the custom success message
            self.credentials = flow.run_local_server(
                port=0,  # Use any available port
                # Always ask for consent to ensure we get refresh tokens
                prompt="consent",  
                success_message=success_message,
                open_browser=True,
                authorization_prompt_message=(
                    "Please authorize the application in your browser"
                ),
                timeout_seconds=120,  # 2 minute timeout for the whole flow
            )

            # Save the credentials for future use
            if self.credentials:
                logger.info("OAuth flow completed successfully - saving new token")
                self.token_storage.save_token(self.credentials.to_json())
            else:
                raise AuthError("OAuth flow completed but no credentials were obtained")

        except ConfigError:
            # Re-raise custom exceptions
            raise
        except Exception as e:
            logger.exception(
                f"Unhandled exception during OAuth flow: {e}",
            )  # Use logger.exception to include stack trace
            raise AuthError(
                f"An unexpected error occurred during OAuth flow: {str(e)}",
            ) from e

    def _load_client_secrets_from_env(self):
        """
        Load client secrets from environment variables.

        Tries two approaches:
        1. Load JSON content directly from GOOGLE_CLIENT_SECRET_JSON_CONTENT
        2. Load from a file path specified in GOOGLE_CLIENT_SECRET_JSON_PATH

        Returns:
            dict: Client secrets configuration or None if not found
        """
        # Try to load JSON content directly from environment variable
        json_content = os.environ.get("GOOGLE_CLIENT_SECRET_JSON_CONTENT")
        if json_content:
            try:
                logger.info(
                    "Loading client secrets from GOOGLE_CLIENT_SECRET_JSON_CONTENT",
                )
                return json.loads(json_content)
            except json.JSONDecodeError as e:
                logger.error(
                    f"Error parsing GOOGLE_CLIENT_SECRET_JSON_CONTENT: {e}"
                )
                raise ConfigError(
                    f"Invalid JSON in GOOGLE_CLIENT_SECRET_JSON_CONTENT: {e}",
                ) from e

        # Try to load from file path specified in environment variable
        json_path = os.environ.get("GOOGLE_CLIENT_SECRET_JSON_PATH")
        if json_path:
            try:
                logger.info(
                    "Loading client secrets from path in "
                    f"GOOGLE_CLIENT_SECRET_JSON_PATH: {json_path}",
                )
                with open(json_path, "r") as f:
                    return json.load(f)
            except FileNotFoundError as e:
                logger.error(f"Client secrets file not found at path: {json_path}")
                raise ConfigError(
                    f"Client secrets file not found at path: {json_path}"
                ) from e
            except json.JSONDecodeError as e:
                logger.error(f"Error parsing client secrets file at {json_path}: {e}")
                raise ConfigError(
                    f"Invalid JSON in client secrets file at {json_path}: {e}",
                ) from e
            except Exception as e:
                logger.error(f"Error reading client secrets file at {json_path}: {e}")
                raise ConfigError(
                    f"Error reading client secrets file: {e}"
                ) from e

        # If neither environment variable is set, return None
        return None
