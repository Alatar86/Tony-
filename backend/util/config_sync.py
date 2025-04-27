import configparser
import logging
import os

logger = logging.getLogger(__name__)


def get_config_paths(root_dir):
    """Get the paths for root and backend config files."""
    root_config_path = os.path.join(root_dir, "config.ini")
    backend_config_path = os.path.join(root_dir, "backend", "config", "config.ini")
    return root_config_path, backend_config_path


def sync_configs(root_dir):
    """Synchronizes configuration settings between root and backend config files."""
    root_config_path, backend_config_path = get_config_paths(root_dir)

    logger.info(f"Root config path: {root_config_path}")
    logger.info(f"Backend config path: {backend_config_path}")

    root_exists = os.path.exists(root_config_path)
    backend_exists = os.path.exists(backend_config_path)

    root_config = configparser.ConfigParser()
    backend_config = configparser.ConfigParser()

    # Read existing configs
    if root_exists:
        root_config.read(root_config_path)
        # Log signature value BEFORE merge
        root_signature = root_config.get("User", "signature", fallback="<NOT SET>")
        logger.info(
            f"SYNC: Signature read from ROOT config BEFORE merge: '{root_signature}'",
        )
    if backend_exists:
        backend_config.read(backend_config_path)
        # Log signature value BEFORE merge
        backend_signature = backend_config.get(
            "User",
            "signature",
            fallback="<NOT SET>",
        )
        logger.info(
            f"SYNC: Signature read from BACKEND config BEFORE merge: '{backend_signature}'",
        )

    if not root_exists and not backend_exists:
        logger.warning("Neither root nor backend config file exists. Cannot sync.")
        return False  # Indicate failure or inability to sync

    elif not root_exists:
        logger.info("Root config file does not exist. Copying backend config to root.")
        # Copy backend to root
        try:
            with open(root_config_path, "w") as configfile:
                backend_config.write(configfile)
            logger.info("Successfully copied backend config to root.")
            return True
        except IOError as e:
            logger.error(f"Failed to copy backend config to root: {e}")
            return False

    elif not backend_exists:
        logger.info(
            "Backend config file does not exist. Copying root config to backend.",
        )
        # Copy root to backend
        try:
            os.makedirs(os.path.dirname(backend_config_path), exist_ok=True)
            with open(backend_config_path, "w") as configfile:
                root_config.write(configfile)
            logger.info("Successfully copied root config to backend.")
            return True
        except IOError as e:
            logger.error(f"Failed to copy root config to backend: {e}")
            return False

    else:
        # Both exist, merge root into backend (root takes precedence)
        logger.info(
            "Both config files exist. Merging configurations (root takes precedence).",
        )
        needs_update = False
        merged_config = configparser.ConfigParser()
        merged_config.read_dict(backend_config)  # Start with backend config

        for section in root_config.sections():
            if not merged_config.has_section(section):
                merged_config.add_section(section)
                needs_update = True
            for key, value in root_config.items(section):
                if (
                    not merged_config.has_option(section, key)
                    or merged_config.get(section, key) != value
                ):
                    merged_config.set(section, key, value)
                    needs_update = True

        # Log the final merged signature value BEFORE writing
        final_signature = merged_config.get("User", "signature", fallback="<NOT SET>")
        logger.info(
            f"SYNC: Final merged signature value BEFORE writing: '{final_signature}'",
        )

        if needs_update:
            logger.info(
                "Configurations differ. Writing merged configuration to both files.",
            )
            try:
                # Write to backend config
                with open(backend_config_path, "w") as configfile:
                    merged_config.write(configfile)
                logger.info("Successfully wrote merged config to backend file.")

                # Write to root config
                with open(root_config_path, "w") as configfile:
                    merged_config.write(configfile)
                logger.info("Successfully wrote merged config to root file.")
                return True  # Indicate sync occurred

            except IOError as e:
                logger.error(f"Failed to write merged configuration: {e}")
                return False  # Indicate failure

        else:
            logger.info("Configurations are already in sync. No changes needed.")
            return True  # Indicate sync checked, no changes needed


if __name__ == "__main__":
    # Example usage: Point to the project's root directory
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    print(f"Running config sync assuming project root: {project_root}")
    logging.basicConfig(level=logging.INFO)
    sync_configs(project_root)
