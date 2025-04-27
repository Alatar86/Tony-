"""
Main entry point for Privacy-Focused Email Agent Backend
This module provides the core functionality for running the backend server.
"""

import logging

from backend.api.api_server import ApiServer

# Configure logging
logger = logging.getLogger(__name__)


def init_logging():
    """Initialize logging configuration"""
    try:
        from backend.util.logging_service import LoggingService

        # Initialize the LoggingService without config_manager
        # It will use environment variables for configuration
        LoggingService()
    except ImportError:
        # Fall back to basic configuration if LoggingService is not available
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        )
        logging.warning(
            "Could not import LoggingService, using basic logging configuration",
        )


def run_server():
    """Initialize and run the API server"""
    try:
        logger.info("Starting Privacy-Focused Email Agent Backend")

        # Initialize and run the API server
        server = ApiServer()
        server.run()

        return 0
    except Exception:
        logger.exception("Error starting application")
        return 1


def main():
    """Main entry point when running directly from the backend directory"""
    init_logging()
    return run_server()


if __name__ == "__main__":
    exit(main())
