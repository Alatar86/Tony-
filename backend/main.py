"""
Main entry point for Privacy-Focused Email Agent Backend
This module provides the core functionality for running the backend server.
"""

import logging
import signal
import sys
from types import FrameType
from typing import Optional

from flask import Flask

from backend.api.api_server import ApiServer
from backend.util.config_manager import ConfigurationManager
from backend.util.logging_service import LoggingService


# Setup signal handling
def setup_signal_handlers() -> None:
    """Sets up signal handlers for graceful shutdown."""
    def handle_signal(signum: int, frame: Optional[FrameType]) -> None:
        logging.info(f"Received signal {signum}. Shutting down gracefully.")
        # Perform any necessary cleanup here
        sys.exit(0)

    signal.signal(signal.SIGTERM, handle_signal)
    signal.signal(signal.SIGINT, handle_signal)

# Function to run the server
def run_server(app: Flask) -> None:
    """
    Runs the Flask server with settings from the config.
    Handles different environments (development, production).
    """
    config = ConfigurationManager.get_instance()
    host = config.get('API', 'host', fallback='127.0.0.1')
    port = config.getint('API', 'port', fallback=5000)

    # Get debug mode configuration and convert to boolean
    debug_mode_str = config.get('development', 'debug_mode', fallback='false')
    debug = (
        debug_mode_str.lower() if debug_mode_str else 'false'
    ) in ('true', '1', 't')

    # The reloader should be disabled if running in a production environment
    # or if managed by an external tool like Gunicorn.
    use_reloader = debug

    app.run(host=host, port=port, debug=debug, use_reloader=use_reloader)

# Main entry point
def main() -> None:
    """
    Main function to initialize and run the application.
    """
    # Initialize configuration
    config = ConfigurationManager.get_instance()

    # Initialize logging service
    LoggingService(config)

    logging.info("Starting the Flask application.")

    # Setup signal handlers for graceful shutdown
    setup_signal_handlers()

    # Create API server instance
    api_server = ApiServer()

    # Run the server
    run_server(api_server.app)

if __name__ == '__main__':
    main()
