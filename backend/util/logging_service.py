"""
LoggingService for Privacy-Focused Email Agent

This utility class configures application logging.
"""

import logging
import os
import sys
from logging.handlers import RotatingFileHandler

# Improved error handling for the json logger import
JSON_LOGGER_AVAILABLE = False
try:
    # Try the new import location first (for newer versions)
    from pythonjsonlogger.json import JsonFormatter

    JSON_LOGGER_AVAILABLE = True
except ImportError:
    try:
        # Fall back to the old location for backward compatibility
        from pythonjsonlogger.jsonlogger import JsonFormatter

        JSON_LOGGER_AVAILABLE = True
    except ImportError:
        # If import fails completely, log a warning and continue with standard formatter
        logging.warning(
            "python-json-logger package not found. Using standard formatter instead.",
        )

        # Create a simple formatter that mimics JsonFormatter
        class FallbackFormatter(logging.Formatter):
            """Fallback formatter when JsonFormatter is not available"""

            def __init__(self, *args, **kwargs):
                super().__init__("%(asctime)s - %(name)s - %(levelname)s - %(message)s")

            def format(self, record):
                # Use the standard formatter format method
                return super().format(record)

        # Alias the FallbackFormatter as JsonFormatter for use in the code
        JsonFormatter = FallbackFormatter


class LoggingService:
    """
    Configures and provides access to the application logger.
    """

    def __init__(self, config_manager=None):
        """
        Initialize the LoggingService.

        Args:
            config_manager: ConfigurationManager instance for accessing 
                configuration (optional)
        """
        self.config_manager = config_manager

        # Get log level from environment variable, fall back to config or default
        self.log_level_name = os.environ.get("LOG_LEVEL", "INFO")
        if config_manager:
            self.log_level_name = config_manager.get(
                "App",
                "log_level",
                fallback=self.log_level_name,
            )

        # Get log file path from environment variable, if set
        self.log_file = os.environ.get("LOG_FILE")
        if not self.log_file and config_manager:
            self.log_file = config_manager.get("App", "log_file", fallback=None)

        # Map string level to logging level
        self.log_level = self._get_log_level(self.log_level_name)

        # Configure logging
        self._configure_logging()

    def _get_log_level(self, level_name):
        """Convert string log level to logging constant"""
        return {
            "DEBUG": logging.DEBUG,
            "INFO": logging.INFO,
            "WARNING": logging.WARNING,
            "ERROR": logging.ERROR,
            "CRITICAL": logging.CRITICAL,
        }.get(level_name.upper(), logging.INFO)

    def _configure_logging(self):
        """Configure the root logger"""
        # Configure root logger
        root_logger = logging.getLogger()
        root_logger.setLevel(self.log_level)

        # Remove existing handlers
        for handler in root_logger.handlers[:]:
            root_logger.removeHandler(handler)

        # Create formatter for stdout (JSON if available, standard otherwise)
        if JSON_LOGGER_AVAILABLE:
            formatter = JsonFormatter(
                "%(timestamp)s %(level)s %(name)s %(message)s",
                rename_fields={
                    "levelname": "level",
                    "asctime": "timestamp",
                },
                json_ensure_ascii=False,
            )
        else:
            formatter = logging.Formatter(
                "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
            )

        # Console handler (stdout)
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setFormatter(formatter)
        root_logger.addHandler(console_handler)

        # Optionally add file handler if LOG_FILE is set
        if self.log_file:
            # Create log directory if needed
            log_dir = os.path.dirname(self.log_file)
            if log_dir and not os.path.exists(log_dir):
                os.makedirs(log_dir, exist_ok=True)

            # File handler with rotation
            file_handler = RotatingFileHandler(
                self.log_file,
                maxBytes=10 * 1024 * 1024,  # 10MB
                backupCount=5,
            )
            # Use standard formatter for file logs
            file_format = logging.Formatter(
                "%(asctime)s - %(name)s - %(levelname)s - "
                "%(filename)s:%(lineno)d - %(message)s",
            )
            file_handler.setFormatter(file_format)
            root_logger.addHandler(file_handler)

        # Log configuration info
        logger = logging.getLogger(__name__)
        logger.info(
            "Logging configured",
            extra={
                "log_level": self.log_level_name,
                "log_file": self.log_file or "stdout only",
                "json_logging": JSON_LOGGER_AVAILABLE,
            },
        )

    def get_logger(self, name):
        """
        Get a logger instance for a specific module.

        Args:
            name (str): Name for the logger (typically __name__)

        Returns:
            Logger: Configured logger instance
        """
        return logging.getLogger(name)
