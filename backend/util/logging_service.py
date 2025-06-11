"""
LoggingService for Privacy-Focused Email Agent

This utility class configures application logging.
"""

import logging
import os
import sys
from logging.handlers import RotatingFileHandler
from typing import Any, Optional, Type

# Improved error handling for the json logger import
JSON_LOGGER_AVAILABLE = False
JsonFormatterType: Type[logging.Formatter]

try:
    # Try the new import location first (for newer versions)
    from pythonjsonlogger.json import JsonFormatter

    JSON_LOGGER_AVAILABLE = True
    JsonFormatterType = JsonFormatter
except ImportError:
    try:
        # Fall back to the old location for backward compatibility
        from pythonjsonlogger.jsonlogger import JsonFormatter

        JSON_LOGGER_AVAILABLE = True
        JsonFormatterType = JsonFormatter
    except ImportError:
        # If import fails completely, log a warning and continue with standard formatter
        logging.warning(
            "python-json-logger package not found. Using standard formatter instead.",
        )

        # Create a simple formatter that mimics JsonFormatter
        class FallbackFormatter(logging.Formatter):
            """Fallback formatter when JsonFormatter is not available"""

            def __init__(self, *args: Any, **kwargs: Any) -> None:
                super().__init__("%(asctime)s - %(name)s - %(levelname)s - %(message)s")

            def format(self, record: logging.LogRecord) -> str:
                # Use the standard formatter format method
                return super().format(record)

        # Set the type for fallback
        JsonFormatterType = FallbackFormatter


class LoggingService:
    """
    Configures and provides access to the application logger.
    """

    def __init__(self, config_manager: Optional[Any] = None) -> None:
        """
        Initialize the LoggingService.

        Args:
            config_manager: ConfigurationManager instance for accessing
                configuration (optional)
        """
        self.config_manager = config_manager

        # Get log level from environment variable, fall back to config or default
        self.log_level_name: str = os.environ.get("LOG_LEVEL", "INFO")
        if config_manager:
            config_log_level = config_manager.get(
                "App",
                "log_level",
                fallback=self.log_level_name,
            )
            if config_log_level:
                self.log_level_name = config_log_level

        # Get log file path from environment variable, if set
        self.log_file: Optional[str] = os.environ.get("LOG_FILE")
        if not self.log_file and config_manager:
            self.log_file = config_manager.get("App", "log_file", fallback=None)

        # Map string level to logging level
        self.log_level: int = self._get_log_level(self.log_level_name)

        # Configure logging
        self._configure_logging()

    def _get_log_level(self, level_name: str) -> int:
        """Convert string log level to logging constant"""
        return {
            "DEBUG": logging.DEBUG,
            "INFO": logging.INFO,
            "WARNING": logging.WARNING,
            "ERROR": logging.ERROR,
            "CRITICAL": logging.CRITICAL,
        }.get(level_name.upper(), logging.INFO)

    def _configure_formatter(self) -> logging.Formatter:
        """
        Configure and return the appropriate formatter based on availability of JsonFormatter.

        Returns:
            logging.Formatter: Either JsonFormatter with specific arguments or FallbackFormatter
        """
        if JSON_LOGGER_AVAILABLE:
            # Create JsonFormatter with all specific arguments
            try:
                # Use type: ignore to suppress Mypy warnings for JsonFormatter-specific arguments
                formatter = JsonFormatterType(  # type: ignore[call-arg]
                    "%(timestamp)s %(level)s %(name)s %(message)s",
                    rename_fields={
                        "levelname": "level",
                        "asctime": "timestamp",
                    },
                    json_ensure_ascii=False,
                )
            except TypeError:
                # Fallback if JsonFormatter doesn't support these arguments
                formatter = JsonFormatterType("%(asctime)s %(levelname)s %(name)s %(message)s")
        else:
            # Create FallbackFormatter (standard logging formatter)
            formatter = logging.Formatter(
                "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
            )

        return formatter

    def _configure_logging(self) -> None:
        """Configure the root logger"""
        # Configure root logger
        root_logger = logging.getLogger()
        root_logger.setLevel(self.log_level)

        # Remove existing handlers
        for handler in root_logger.handlers[:]:
            root_logger.removeHandler(handler)

        # Create formatter using the dedicated method
        formatter = self._configure_formatter()

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

    def get_logger(self, name: str) -> logging.Logger:
        """
        Get a logger instance for a specific module.

        Args:
            name (str): Name for the logger (typically __name__)

        Returns:
            Logger: Configured logger instance
        """
        return logging.getLogger(name)
