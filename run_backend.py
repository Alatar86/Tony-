"""
Launcher script for Privacy-Focused Email Agent Backend

This script properly sets up the Python path and launches the backend service.
It can be run from the project root directory.
"""

import logging
import os
import sys


def setup_environment():
    """Set up the Python path and configure logging"""
    # Add the project root to Python's path to enable absolute imports
    current_dir = os.path.abspath(os.path.dirname(__file__))
    if current_dir not in sys.path:
        sys.path.insert(0, current_dir)
    
    # Import and use the new LoggingService instead of basicConfig
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
        logging.warning("Could not import LoggingService, using basic logging configuration")


def sync_configuration():
    """Sync configuration files to ensure consistency"""
    try:
        from backend.scripts.sync_config import sync_configuration as sync

        logging.info("Syncing configuration files...")
        sync()
        logging.info("Configuration sync completed")

    except ImportError as e:
        logging.warning(f"Could not import config sync utility: {e}")
    except Exception as e:
        logging.warning(f"Error syncing configuration: {e}")


def main():
    """Entry point for launching the backend from the project root"""
    setup_environment()

    # Sync configuration before starting
    sync_configuration()

    try:
        # Import backend functionality after path is set up
        from backend.main import run_server

        # Run the server
        return run_server()

    except ImportError:
        logging.exception("Error importing backend modules. Check your Python path.")
        return 1
    except Exception:
        logging.exception("Unexpected error starting application")
        return 1


if __name__ == "__main__":
    exit(main())
