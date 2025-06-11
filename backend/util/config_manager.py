"""
ConfigurationManager for Privacy-Focused Email Agent

This utility class provides centralized access to application configuration.
"""

import configparser
import logging
import os
from typing import Any, Dict, Optional

from .exceptions import ConfigError

logger = logging.getLogger(__name__)


class ConfigurationManager:
    """
    Manages application configuration from config.ini file.
    Provides centralized access to configuration values.
    """

    # Environment variable prefix for configuration overrides
    ENV_PREFIX = "APP_"
    
    # Singleton instance
    _instance: Optional['ConfigurationManager'] = None

    def __init__(self, config_file: Optional[str] = None) -> None:
        """
        Initialize the ConfigurationManager.

        Args:
            config_file (Optional[str]): Path to the configuration file
                (default: look in standard locations)
        """
        # Look for config in standard locations if not specified
        if config_file is None:
            # Try the following locations in order:
            # 1. Current directory
            # 2. Project root directory
            # 3. backend/config directory
            possible_locations = [
                "config.ini",  # Current directory
                os.path.join(
                    os.path.dirname(__file__), "..", "..", "config.ini"
                ),  # Project root
                os.path.join(
                    os.path.dirname(__file__),
                    "..",
                    "config",
                    "config.ini",
                ),  # backend/config
            ]

            for location in possible_locations:
                if os.path.exists(location):
                    config_file = location
                    break

        self.config_file: Optional[str] = config_file
        self.config: configparser.ConfigParser = configparser.ConfigParser()

        # Load configuration
        if config_file and os.path.exists(config_file):
            logger.info(f"Loading configuration from {config_file}")
            self.config.read(config_file)
            # Add logging to check signature right after load
            loaded_signature = self.config.get(
                "User",
                "signature",
                fallback="<NOT FOUND>",
            )
            logger.info(f"Signature loaded from config file: '{loaded_signature}'")

            # Apply environment variable overrides
            self._apply_env_overrides()
        else:
            logger.warning(f"Configuration file not found: {config_file}")
            self._create_default_config()

            # Apply environment variable overrides to default config
            self._apply_env_overrides()

    @classmethod
    def get_instance(cls, config_file: Optional[str] = None) -> 'ConfigurationManager':
        """
        Get the singleton instance of ConfigurationManager.
        
        Args:
            config_file (Optional[str]): Path to the configuration file
        
        Returns:
            ConfigurationManager: The singleton instance
        """
        if cls._instance is None:
            cls._instance = cls(config_file)
        return cls._instance

    def _apply_env_overrides(self) -> None:
        """
        Override configuration values with environment variables.
        Environment variable naming convention: APP_SECTION_KEY (all uppercase)
        For example, [api] base_url could be overridden with APP_API_BASE_URL
        """
        logger.debug("Checking for environment variable overrides")

        # Iterate through all sections and options in the config
        for section in self.config.sections():
            for key in self.config[section]:
                # Construct the environment variable name: APP_SECTION_KEY
                env_var_name = f"{self.ENV_PREFIX}{section}_{key}".upper()
                env_value = os.environ.get(env_var_name)

                if env_value is not None:
                    # Try to convert the environment variable value
                    # to the same type as the original config value
                    original_value = self.config[section][key]
                    converted_value = self._convert_value(env_value, original_value)

                    # Update the configuration
                    self.config[section][key] = converted_value
                    logger.info(
                        f"Configuration override from environment: "
                        f"[{section}] {key} = {converted_value}",
                    )

    def _convert_value(self, env_value: str, original_value: str) -> str:
        """
        Attempt to convert an environment variable value to the same type
        as the original value.

        Args:
            env_value (str): The value from the environment variable
            original_value (str): The original value from the config file

        Returns:
            str: The converted value, or the original string if conversion fails
        """
        try:
            # Try boolean conversion first (special case)
            if original_value.lower() in (
                "true",
                "false",
                "yes",
                "no",
                "on",
                "off",
                "1",
                "0",
            ):
                # Keep as string to maintain configparser compatibility
                return env_value

            # Try integer conversion
            try:
                int(original_value)
                # If successful, just return the string
                # as configparser handles the conversion
                return env_value
            except ValueError:
                pass

            # Try float conversion
            try:
                float(original_value)
                # If successful, just return the string
                # as configparser handles the conversion
                return env_value
            except ValueError:
                pass

            # Default: return as string
            return env_value

        except Exception as e:
            logger.warning(
                f"Failed to convert environment variable value {env_value} "
                f"to appropriate type: {e}",
            )
            return env_value

    def get(self, section: str, key: str, fallback: Optional[Any] = None) -> Optional[str]:
        """
        Get a configuration value.

        Args:
            section (str): Configuration section name
            key (str): Configuration key name
            fallback (Optional[Any]): Default value if section/key doesn't exist

        Returns:
            Optional[str]: The configuration value or fallback if not found
        """
        try:
            return self.config.get(section, key)
        except (configparser.NoSectionError, configparser.NoOptionError) as e:
            logger.warning(f"Configuration not found: [{section}] {key}. {e}")
            return fallback

    def get_section(self, section: str) -> Dict[str, str]:
        """
        Get an entire configuration section as a dictionary.
        
        Args:
            section (str): Configuration section name
            
        Returns:
            Dict[str, str]: Dictionary of key-value pairs in the section
        """
        try:
            return dict(self.config[section])
        except KeyError:
            logger.warning(f"Configuration section not found: {section}")
            return {}

    def getint(self, section: str, key: str, fallback: Optional[int] = None) -> Optional[int]:
        """Get configuration value as integer"""
        try:
            return self.config.getint(section, key)
        except (configparser.NoSectionError, configparser.NoOptionError) as e:
            logger.warning(f"Configuration not found: [{section}] {key}. {e}")
            return fallback

    def getfloat(self, section: str, key: str, fallback: Optional[float] = None) -> Optional[float]:
        """Get configuration value as float"""
        try:
            return self.config.getfloat(section, key)
        except (configparser.NoSectionError, configparser.NoOptionError) as e:
            logger.warning(f"Configuration not found: [{section}] {key}. {e}")
            return fallback

    def getboolean(self, section: str, key: str, fallback: Optional[bool] = None) -> Optional[bool]:
        """Get configuration value as boolean"""
        try:
            return self.config.getboolean(section, key)
        except (configparser.NoSectionError, configparser.NoOptionError) as e:
            logger.warning(f"Configuration not found: [{section}] {key}. {e}")
            return fallback

    def save(self) -> None:
        """Save current configuration to potentially multiple files."""
        # Define potential file paths
        root_config_path = os.path.join(
            os.path.dirname(__file__),
            "..",
            "..",
            "config.ini",
        )
        backend_config_path = os.path.join(
            os.path.dirname(__file__),
            "..",
            "config",
            "config.ini",
        )

        # Determine the primary file being managed (based on __init__ logic)
        primary_config_file = self.config_file
        if not primary_config_file:
            primary_config_file = "config.ini"  # Default if none was found/set
            logger.warning(
                f"No primary config file path determined, "
                f"defaulting to: {primary_config_file}",
            )

        files_to_save = [primary_config_file]
        # Add the other known location if it exists and is different
        other_path: Optional[str] = None
        if primary_config_file == root_config_path and os.path.exists(
            backend_config_path,
        ):
            other_path = backend_config_path
        elif primary_config_file == backend_config_path and os.path.exists(
            root_config_path,
        ):
            other_path = root_config_path
        elif primary_config_file == "config.ini" and os.path.exists(
            backend_config_path,
        ):
            # Handle case where default "config.ini" in root is used
            other_path = backend_config_path

        if other_path and os.path.abspath(other_path) != os.path.abspath(
            primary_config_file,
        ):
            files_to_save.append(other_path)

        logger.info(f"ConfigurationManager will attempt to save to: {files_to_save}")

        for file_path in files_to_save:
            abs_path = os.path.abspath(file_path)
            logger.info(f"Attempting to save configuration to: {abs_path}")
            try:
                # Ensure directory exists
                os.makedirs(os.path.dirname(abs_path), exist_ok=True)
                with open(file_path, "w") as f:
                    self.config.write(f)
                logger.info(f"Configuration successfully saved to {file_path}")
            except Exception as e:
                logger.exception(f"Failed to save configuration to {file_path}")
                # Raise the exception so the caller knows saving failed,
                # but log which file failed
                raise ConfigError(f"Failed to write config to {file_path}: {e}") from e

    def _create_default_config(self) -> None:
        """Create default configuration file"""
        logger.info("Creating default configuration")

        # Google section
        self.config["Google"] = {
            "client_secret_file": (
                "client_secret_93499475515-psjngo6sm9m5reun6t7ndtv5q6h183pi"
                ".apps.googleusercontent.com.json"
            ),
            "token_cache_path": "token_storage",
            "scopes": "https://www.googleapis.com/auth/gmail.readonly,"
            "https://www.googleapis.com/auth/gmail.send",
        }

        # Ollama section
        self.config["Ollama"] = {
            "model_name": "llama3:8b-instruct-q4_K_M",
            "api_base_url": "http://localhost:11434",
            "request_timeout_sec": "120",
            "suggestion_prompt_template": (
                "System: You are a helpful assistant providing concise email reply "
                "suggestions.\n"
                "User: Based on the following email, generate 3 brief, distinct reply "
                "suggestions (each under 20 words):\n\n"
                "EMAIL BODY:\n{email_body}\n\n"
                "Assistant: 1."
            ),
        }

        # App section
        self.config["App"] = {
            "max_emails_fetch": "50",
            "log_file": "backend_app.log",
            "log_level": "INFO",
        }

        # API section
        self.config["API"] = {
            "host": "127.0.0.1",
            "port": "5000",
        }

        # Save default configuration
        self.save()
