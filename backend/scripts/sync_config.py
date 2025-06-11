"""
Configuration Sync Utility for Privacy-Focused Email Agent

This script ensures that configuration files are consistent across the project.
It syncronizes the root config.ini with backend/config/config.ini.
"""

import configparser
import logging
import os
import shutil
import sys
from typing import Dict, Any, Optional, List, Tuple, Union, NoReturn

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("config_sync")


def sync_configuration() -> bool:
    """
    Sync configuration files between project root and backend/config directory.

    Returns:
        bool: True if sync was successful, False otherwise
    """
    # Determine script directory
    script_dir = os.path.dirname(os.path.abspath(__file__))

    # Build paths to configuration files
    root_config_path = os.path.join(script_dir, "..", "..", "config.ini")
    backend_config_path = os.path.join(script_dir, "..", "config", "config.ini")

    # Convert to absolute paths
    root_config_path = os.path.abspath(root_config_path)
    backend_config_path = os.path.abspath(backend_config_path)

    logger.info(f"Root config path: {root_config_path}")
    logger.info(f"Backend config path: {backend_config_path}")

    # Check if both files exist
    root_exists = os.path.exists(root_config_path)
    backend_exists = os.path.exists(backend_config_path)

    if not root_exists and not backend_exists:
        logger.error("No configuration files found to sync")
        return False

    # If only one file exists, copy it to the other location
    if root_exists and not backend_exists:
        logger.info("Backend config not found. Copying from root.")
        os.makedirs(os.path.dirname(backend_config_path), exist_ok=True)
        shutil.copy2(root_config_path, backend_config_path)
        logger.info("Config copied from root to backend")
        return True

    if backend_exists and not root_exists:
        logger.info("Root config not found. Copying from backend.")
        shutil.copy2(backend_config_path, root_config_path)
        logger.info("Config copied from backend to root")
        return True

    # Both files exist, merge them
    logger.info("Both config files exist. Merging configurations.")

    # Read both configurations
    root_config = configparser.ConfigParser()
    backend_config = configparser.ConfigParser()

    root_config.read(root_config_path)
    backend_config.read(backend_config_path)

    # Flag to track if any changes were made
    changes_made = False

    # Merge configurations
    # We'll use the root config as the base and merge in any missing values from backend
    for section in root_config.sections():
        if not backend_config.has_section(section):
            backend_config.add_section(section)
            changes_made = True

        for key, value in root_config[section].items():
            if (
                not backend_config.has_option(section, key)
                or backend_config[section][key] != value
            ):
                backend_config[section][key] = value
                changes_made = True

    # Check for sections/options in backend that aren't in root
    for section in backend_config.sections():
        if not root_config.has_section(section):
            root_config.add_section(section)
            changes_made = True

        for key, value in backend_config[section].items():
            if not root_config.has_option(section, key):
                root_config[section][key] = value
                changes_made = True

    # Save the updated configurations if changes were made
    if changes_made:
        with open(root_config_path, "w") as f:
            root_config.write(f)

        with open(backend_config_path, "w") as f:
            backend_config.write(f)

        logger.info("Configurations synchronized successfully")
    else:
        logger.info("Configurations are already in sync. No changes needed.")

    return True


def main() -> int:
    """Main entry point"""
    logger.info("Starting configuration sync")
    success = sync_configuration()
    if success:
        logger.info("Configuration sync completed successfully")
        return 0
    else:
        logger.error("Configuration sync failed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
