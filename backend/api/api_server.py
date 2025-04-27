"""
API Server for Privacy-Focused Email Agent

This module implements the REST API server using Flask, coordinating services
and registering API blueprints.
"""

import logging

from flask import Flask, jsonify, request

from ..services.gmail_api_service import GmailApiService
from ..services.google_auth_service import GoogleAuthService
from ..services.local_llm_service import LocalLlmService
from ..util.config_manager import ConfigurationManager
from ..util.exceptions import (
    AuthError,
    ConfigError,
    GmailApiError,
    NotFoundError,
    OllamaError,
    ServiceError,
    ValidationError,
)
from ..util.logging_service import LoggingService
from ..util.secure_token_storage import SecureTokenStorage

# Import Blueprints
from .routes.auth import auth_bp
from .routes.config import config_bp
from .routes.emails import emails_bp
from .routes.status import status_bp

# Setup logging
logger = logging.getLogger(__name__)


class ApiServer:
    """
    Flask REST API server for the Email Agent backend.
    Initializes services, registers error handlers, and registers API blueprints.
    """

    def __init__(self):
        """Initialize the API server and services"""
        # Create Flask app
        self.app = Flask(__name__)

        # Initialize configuration
        self.config_manager = ConfigurationManager()

        # Initialize logging
        self.logging_service = LoggingService(self.config_manager)

        # Initialize services
        self.token_storage = SecureTokenStorage(self.config_manager)
        self.auth_service = GoogleAuthService(self.config_manager, self.token_storage)
        self.llm_service = LocalLlmService(self.config_manager)

        # Gmail service will be created when needed with valid credentials
        # Note: This instance variable might become less necessary if routes
        # always use the _get_authenticated_gmail_service helper method.
        self.gmail_service = None

        # Store service instances and helper methods in app config for Blueprint access
        # Using a dedicated key like 'SERVICES' avoids cluttering the config root.
        self.app.config["SERVICES"] = {
            "auth_service": self.auth_service,
            "llm_service": self.llm_service,
            "config_manager": self.config_manager,
            "get_gmail_service": self._get_authenticated_gmail_service,  # Store bound method
        }

        # Register error handlers
        self._register_error_handlers()

        # Register Blueprints
        self.app.register_blueprint(auth_bp)
        self.app.register_blueprint(config_bp)
        self.app.register_blueprint(emails_bp)
        self.app.register_blueprint(status_bp)

        logger.info("API server initialized with blueprints registered")

    def _register_error_handlers(self):
        """Register error handlers for custom exceptions"""

        @self.app.errorhandler(ServiceError)
        def handle_service_error(error):
            """Handle all ServiceError instances"""
            logger.error(f"Service error: {error.message}")
            return jsonify(error.to_dict()), error.code

        # Handle specific subclasses if needed, or let them fall through to ServiceError
        @self.app.errorhandler(AuthError)
        def handle_auth_error(error):
            logger.error(f"Auth error: {error.message}")
            return jsonify(error.to_dict()), error.code

        @self.app.errorhandler(GmailApiError)
        def handle_gmail_api_error(error):
            logger.error(f"Gmail API error: {error.message}")
            return jsonify(error.to_dict()), error.code

        @self.app.errorhandler(OllamaError)
        def handle_ollama_error(error):
            logger.error(f"Ollama error: {error.message}")
            return jsonify(error.to_dict()), error.code

        @self.app.errorhandler(ConfigError)
        def handle_config_error(error):
            logger.error(f"Config error: {error.message}")
            return jsonify(error.to_dict()), error.code

        @self.app.errorhandler(ValidationError)
        def handle_validation_error(error):
            logger.warning(
                f"Validation error: {error.message}",
            )  # Usually client error, log as warning
            return jsonify(error.to_dict()), error.code

        @self.app.errorhandler(NotFoundError)
        def handle_not_found_error(error):
            logger.warning(
                f"Resource not found: {error.message}",
            )  # Usually client error, log as warning
            return jsonify(error.to_dict()), error.code

        # Standard HTTP errors
        @self.app.errorhandler(404)
        def handle_404(error):
            """Handle 404 Not Found errors"""
            logger.info(f"Not found: {request.path}")
            # Ensure consistent JSON format
            nf_error = NotFoundError(
                f"The requested URL {request.path} was not found on this server.",
            )
            return jsonify(nf_error.to_dict()), nf_error.code

        @self.app.errorhandler(405)
        def handle_405(error):
            """Handle 405 Method Not Allowed errors"""
            logger.info(f"Method not allowed: {request.method} {request.path}")
            ma_error = ServiceError(
                f"The method {request.method} is not allowed for the requested URL {request.path}.",
                code=405,
            )
            return jsonify(ma_error.to_dict()), ma_error.code

        @self.app.errorhandler(500)
        def handle_500(error):
            """Handle generic 500 Internal Server Error"""
            # Log the original exception if possible
            logger.exception(
                f"Internal server error processing {request.path}: {error}",
            )
            ise_error = ServiceError("An internal server error occurred.", code=500)
            return jsonify(ise_error.to_dict()), ise_error.code

    # Routes are removed from here and moved to blueprints in ./routes/
    # def _setup_routes(self):
    #    pass

    def _get_authenticated_gmail_service(self):
        """
        Get an authenticated Gmail service instance.
        This method remains here as it depends on self.auth_service and self.config_manager.
        It's accessed by blueprints via current_app.config['SERVICES']['get_gmail_service']().

        Returns:
            GmailApiService instance or None if authentication fails
        """
        try:
            # Get credentials using the instance's auth_service
            credentials = self.auth_service.get_credentials()
            if not credentials:
                logger.error("Failed to get valid credentials")
                return None

            # Create Gmail service if needed - reuse self.gmail_service instance if already created
            # This provides a basic singleton-like behavior per ApiServer instance.
            # If the credentials changed, this logic doesn't automatically recreate the service,
            # but get_credentials() should handle token refresh internally.
            # TODO: Review if credential changes require explicit recreation of GmailApiService.
            if not self.gmail_service or self.gmail_service.credentials != credentials:
                logger.info("Creating/Updating GmailApiService instance.")
                self.gmail_service = GmailApiService(credentials, self.config_manager)

            return self.gmail_service

        except Exception as e:
            # Log error specific to this helper function
            logger.exception(f"Error getting/creating Gmail service: {e}")
            return None

    def run(self):
        """Run the API server"""
        host = self.config_manager.get("API", "host", fallback="127.0.0.1")
        port = self.config_manager.getint("API", "port", fallback=5000)

        logger.info(f"Starting API server on {host}:{port}")
        # Consider using a production-ready WSGI server like gunicorn or waitress
        # instead of self.app.run() for production deployments.
        # For development, debug=False is usually appropriate for API servers unless actively debugging Flask itself.
        self.app.run(host=host, port=port, debug=False)


# Entry point for running the server (e.g., for development)
# The main entry point is usually run_backend.py
def main():
    """Main entry point for the API server if run directly"""
    # Basic logging if run directly (might differ from run_backend.py setup)
    logging.basicConfig(level=logging.INFO)
    logger.info("Running ApiServer directly...")
    server = ApiServer()
    server.run()


if __name__ == "__main__":
    main()
