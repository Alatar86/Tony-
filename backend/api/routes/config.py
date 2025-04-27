"""
Configuration related routes
"""
import configparser
import logging

from flask import Blueprint, current_app, jsonify, request

# No longer importing services directly, will use current_app
from ...util.exceptions import (  # Keep specific exceptions needed
    ServiceError,
    ValidationError,
)

logger = logging.getLogger(__name__)

config_bp = Blueprint("config_bp", __name__, url_prefix="/config")


@config_bp.route("", methods=["GET"])
def get_config():
    """Get current application configuration"""
    config_manager = current_app.config["SERVICES"]["config_manager"]
    try:
        # Specify the settings we want to expose
        settings_to_expose = {
            "Ollama": ["api_base_url", "model_name"],
            "App": ["max_emails_fetch"],
            "User": ["signature"],
        }

        config_values = {}
        for section, keys in settings_to_expose.items():
            # Check if section exists
            if not config_manager.config.has_section(section):
                logger.warning(
                    f"Config section [{section}] not found during GET /config. Skipping."
                )
                config_values[section] = {}  # Still return the section key, but empty
                continue

            config_values[section] = {}
            for key in keys:
                try:
                    # Use appropriate getter based on expected type
                    if key == "max_emails_fetch":
                        value = config_manager.getint(section, key)
                    # elif key == "some_other_boolean_key": # Example for future
                    #     value = config_manager.getboolean(section, key)
                    else:
                        # Attempt to get the value WITHOUT a fallback
                        value = config_manager.get(section, key)

                    # Log the retrieved value for debugging
                    if section == "User" and key == "signature":
                        logger.info(
                            f"GET /config: Retrieved signature value: '{value}'"
                        )

                    config_values[section][key] = value
                except (configparser.NoOptionError, configparser.NoSectionError) as e:
                    # Log if a specific key is missing within an existing section
                    logger.warning(
                        f"Config key [{section}] {key} not found during GET /config: {e}. Setting to default/null."
                    )
                    # Decide on a default value (e.g., None, empty string, or specific default)
                    config_values[section][
                        key
                    ] = None  # Or use "" or a default value like for max_emails_fetch
                except ValueError as e:
                    # Handle cases where getint/getboolean fails due to wrong type in file
                    logger.error(
                        f"Config key [{section}] {key} has invalid format: {e}. Setting to default/null."
                    )
                    config_values[section][key] = None  # Or default

        return jsonify(config_values)

    except Exception as e:
        logger.exception("Error fetching configuration")
        raise ServiceError(f"Error fetching configuration: {str(e)}", 500)


@config_bp.route("", methods=["POST"])
def update_config():
    """Update application configuration"""
    config_manager = current_app.config["SERVICES"]["config_manager"]
    # Need direct access to llm_service instance to update it
    # This is slightly awkward with the context approach, might need refinement.
    # A better way might be to have services register update callbacks.
    # For now, let's assume api_server updates it if needed after calling this.
    # llm_service = current_app.config['SERVICES']['llm_service']

    try:
        data = request.get_json()
        if not data:
            raise ValidationError("No data provided")

        logger.info(f"Received config update request: {data}")

        # Create default sections if they don't exist
        required_sections = ["Ollama", "App", "User"]
        for section in required_sections:
            if section not in config_manager.config:
                logger.info(f"Creating required config section: {section}")
                config_manager.config.add_section(section)

        # Validate and update settings
        updated_any = False
        ollama_updated = False
        for section, settings in data.items():
            # Skip null or empty settings
            if settings is None:
                continue

            # Validate based on section and key
            if section == "Ollama" and settings is not None:
                for key, value in settings.items():
                    if key not in ["api_base_url", "model_name"]:
                        logger.warning(f"Ignoring invalid Ollama setting: {key}")
                        continue

                    # Update the value in the ConfigParser object
                    config_manager.config.set(
                        section, key, str(value)
                    )  # Store as string
                    logger.info(f"Configuration updated: [{section}] {key} = {value}")
                    updated_any = True
                    ollama_updated = True  # Track if Ollama settings changed

            elif section == "App" and settings is not None:
                for key, value in settings.items():
                    if key not in ["max_emails_fetch"]:
                        logger.warning(f"Ignoring invalid App setting: {key}")
                        continue

                    # Update the value in the ConfigParser object
                    config_manager.config.set(
                        section, key, str(value)
                    )  # Store as string
                    logger.info(f"Configuration updated: [{section}] {key} = {value}")
                    updated_any = True

            elif section == "User" and settings is not None:
                for key, value in settings.items():
                    if key not in ["signature"]:
                        logger.warning(f"Ignoring invalid User setting: {key}")
                        continue

                    # Ensure the value is a string (might be null)
                    if value is None:
                        value = ""

                    # Update the value in the ConfigParser object
                    config_manager.config.set(
                        section, key, str(value)
                    )  # Store as string
                    logger.info(f"Configuration updated: [{section}] {key} = {value}")
                    updated_any = True
            else:
                logger.warning(f"Ignoring unknown config section: {section}")

        if updated_any:
            # Save the changes to config.ini
            config_manager.save()
            # Signal if LLM service needs re-initialization
            # The actual re-initialization might happen in api_server.py
            # after this request finishes, triggered by this flag.
            if ollama_updated:
                # Ideally, trigger re-init via a signal or callback
                logger.info(
                    "Ollama config updated. Related services may need re-initialization."
                )
                # Re-initialization logic removed from here, needs to be handled centrally.
                # current_app.config['SERVICES']['llm_service'] = LocalLlmService(config_manager)

        return jsonify(
            {"success": True, "message": "Configuration updated successfully."}
        )

    except ValidationError:
        raise  # Let the error handler catch validation errors
    except Exception as e:
        logger.exception("Error updating configuration")
        # Use a specific error type if available, e.g., ConfigError
        raise ServiceError(f"Error updating configuration: {str(e)}", 500)


@config_bp.route("/signature", methods=["POST"])
def update_signature():
    """Update only the user signature."""
    config_manager = current_app.config["SERVICES"]["config_manager"]
    try:
        data = request.get_json()
        if not data or "signature" not in data:
            raise ValidationError("Missing 'signature' in request data")

        new_signature = data["signature"]
        if new_signature is None:
            new_signature = ""  # Ensure it's a string

        # Ensure [User] section exists
        if "User" not in config_manager.config:
            logger.info("Creating missing section User for signature update")
            config_manager.config.add_section("User")

        logger.info(f"Attempting to set [User] signature to: '{new_signature}'")
        config_manager.config.set("User", "signature", str(new_signature))
        logger.info("Value set in config object. Attempting to save...")
        config_manager.save()
        logger.info("User signature updated successfully.")
        return jsonify(
            {"status": "success", "message": "Signature updated successfully."}
        ), 200

    except ValidationError:
        raise  # Let the error handler catch validation errors
    except Exception as e:
        logger.exception("Error updating signature")
        raise ServiceError(f"Error updating signature: {str(e)}", 500)
