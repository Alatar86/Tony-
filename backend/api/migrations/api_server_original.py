"""
API Server for Privacy-Focused Email Agent

This module implements the REST API server using Flask.
"""

import logging

from flask import Flask, jsonify, request

from ..services.gmail_api_service import GmailApiService
from ..services.google_auth_service import GoogleAuthService
from ..services.local_llm_service import LocalLlmService
from ..util.config_manager import ConfigurationManager
from ..util.exceptions import (
    AuthError,
    GmailApiError,
    NotFoundError,
    OllamaError,
    ServiceError,
    ValidationError,
)
from ..util.logging_service import LoggingService
from ..util.secure_token_storage import SecureTokenStorage

# Setup logging
logger = logging.getLogger(__name__)


class ApiServer:
    """
    Flask REST API server for the Email Agent backend.
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
        self.gmail_service = None

        # Set up routes
        self._setup_routes()

        # Register error handlers
        self._register_error_handlers()

        logger.info("API server initialized")

    def _register_error_handlers(self):
        """Register error handlers for custom exceptions"""

        @self.app.errorhandler(ServiceError)
        def handle_service_error(error):
            """Handle all ServiceError instances"""
            logger.error(f"Service error: {error.message}")
            return jsonify(error.to_dict()), error.code

        @self.app.errorhandler(404)
        def handle_404(error):
            """Handle 404 Not Found errors"""
            logger.info(f"Not found: {request.path}")
            return jsonify({"error": "Resource not found", "code": 404}), 404

        @self.app.errorhandler(405)
        def handle_405(error):
            """Handle 405 Method Not Allowed errors"""
            logger.info(f"Method not allowed: {request.method} {request.path}")
            return jsonify({"error": "Method not allowed", "code": 405}), 405

        @self.app.errorhandler(500)
        def handle_500(error):
            """Handle 500 Internal Server Error"""
            logger.error(f"Internal server error: {error}")
            return jsonify({"error": "Internal server error", "code": 500}), 500

    def _setup_routes(self):
        """Set up the API routes"""

        # Authentication endpoints
        @self.app.route("/auth/status", methods=["GET"])
        def auth_status():
            """Check authentication status"""
            authenticated = self.auth_service.check_auth_status()
            return jsonify({"authenticated": authenticated})

        @self.app.route("/auth/login", methods=["POST"])
        def auth_login():
            """Initiate OAuth login flow"""
            try:
                success = self.auth_service.initiate_auth_flow()
                return jsonify(
                    {
                        "success": success,
                        "message": "Authentication successful"
                        if success
                        else "Authentication failed",
                    },
                )
            except Exception as e:
                logger.exception("Error during authentication")
                raise AuthError(f"Error during authentication: {str(e)}") from e

        # Configuration endpoints
        @self.app.route("/config", methods=["GET"])
        def get_config():
            """Get current application configuration"""
            try:
                # Specify the settings we want to expose
                settings_to_expose = {
                    "Ollama": ["api_base_url", "model_name"],
                    "App": ["max_emails_fetch"],
                    "User": ["signature"],
                }

                config_values = {}
                for section, keys in settings_to_expose.items():
                    # Check if section exists, if not create it with default values
                    if section not in self.config_manager.config:
                        logger.info(f"Creating missing section {section} in config")
                        self.config_manager.config.add_section(section)
                        self.config_manager.save()

                    config_values[section] = {}
                    for key in keys:
                        # Use appropriate getter based on expected type
                        # (optional, but good practice)
                        if key == "max_emails_fetch":
                            value = self.config_manager.getint(
                                section,
                                key,
                                fallback=50,
                            )
                        else:
                            value = self.config_manager.get(section, key, fallback="")
                        config_values[section][key] = value

                return jsonify(config_values)

            except Exception as e:
                logger.exception("Error fetching configuration")
                # Use a specific error type if available, e.g., ConfigError
                raise ServiceError(
                    f"Error fetching configuration: {str(e)}", 500
                ) from e

        @self.app.route("/config", methods=["POST"])
        def update_config():
            """Update application configuration"""
            try:
                data = request.get_json()
                if not data:
                    raise ValidationError("No data provided")

                logger.info(f"Received config update request: {data}")

                # Create default sections if they don't exist
                required_sections = ["Ollama", "App", "User"]
                for section in required_sections:
                    if section not in self.config_manager.config:
                        logger.info(f"Creating required config section: {section}")
                        self.config_manager.config.add_section(section)

                # Validate and update settings
                updated_any = False
                for section, settings in data.items():
                    # Skip null or empty settings
                    if settings is None:
                        continue

                    # Validate based on section and key
                    if section == "Ollama" and settings is not None:
                        for key, value in settings.items():
                            if key not in ["api_base_url", "model_name"]:
                                logger.warning(
                                    f"Ignoring invalid Ollama setting: {key}",
                                )
                                continue

                            # Update the value in the ConfigParser object
                            self.config_manager.config.set(
                                section,
                                key,
                                str(value),
                            )  # Store as string
                            logger.info(
                                f"Configuration updated: [{section}] {key} = {value}",
                            )
                            updated_any = True

                    elif section == "App" and settings is not None:
                        for key, value in settings.items():
                            if key not in ["max_emails_fetch"]:
                                logger.warning(f"Ignoring invalid App setting: {key}")
                                continue

                            # Update the value in the ConfigParser object
                            self.config_manager.config.set(
                                section,
                                key,
                                str(value),
                            )  # Store as string
                            logger.info(
                                f"Configuration updated: [{section}] {key} = {value}",
                            )
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
                            self.config_manager.config.set(
                                section,
                                key,
                                str(value),
                            )  # Store as string
                            logger.info(
                                f"Configuration updated: [{section}] {key} = {value}",
                            )
                            updated_any = True
                    else:
                        logger.warning(f"Ignoring unknown config section: {section}")

                if updated_any:
                    # Save the changes to config.ini
                    self.config_manager.save()
                    # Re-initialize services that depend on these settings
                    if any(
                        key in data.get("Ollama", {})
                        for key in ["api_base_url", "model_name"]
                    ):
                        self.llm_service = LocalLlmService(self.config_manager)
                        logger.info("LLM service re-initialized after config update.")

                return jsonify(
                    {"success": True, "message": "Configuration updated successfully."},
                )

            except ValidationError:
                raise  # Let the error handler catch validation errors
            except Exception as e:
                logger.exception("Error updating configuration")
                # Use a specific error type if available, e.g., ConfigError
                raise ServiceError(
                    f"Error updating configuration: {str(e)}", 500
                ) from e

        # Email data endpoints
        @self.app.route("/emails", methods=["GET"])
        def list_emails():
            """List emails using a specific label ID."""
            # Check authentication
            if not self.auth_service.check_auth_status():
                raise AuthError("Not authenticated")

            # Get parameters
            label_id = request.args.get("labelId", default="INBOX", type=str)
            max_results = request.args.get(
                "maxResults",
                default=self.config_manager.getint("App", "max_emails_fetch"),
                type=int,
            )

            logger.info(
                f"API request: List emails for labelId='{label_id}', "
                f"maxResults={max_results}",
            )

            try:
                # Get Gmail service
                gmail_service = self._get_authenticated_gmail_service()
                if not gmail_service:
                    raise GmailApiError("Failed to initialize Gmail service")

                # Step 1: Get message IDs using the label_id
                message_ids = gmail_service.list_messages(
                    label_id=label_id,
                    max_results=max_results,
                )
                if (
                    message_ids is None
                ):  # list_messages should return [], but defensive check
                    logger.warning(
                        f"list_messages returned None for labelId: {label_id}",
                    )
                    message_ids = []

                logger.info(
                    f"Found {len(message_ids)} message IDs for labelId: {label_id}",
                )

                # Step 2: Get metadata for all message IDs using the new batch method
                if message_ids:
                    metadata_dict = gmail_service.get_multiple_messages_metadata(
                        message_ids,
                    )
                    # Filter out any None results (where individual fetches failed)
                    # and get a list of metadata dictionaries
                    emails = [
                        metadata
                        for metadata in metadata_dict.values()
                        if metadata is not None
                    ]
                    logger.info(
                        f"Successfully fetched metadata for {len(emails)} out of "
                        f"{len(message_ids)} emails via batch for labelId: {label_id}",
                    )
                else:
                    emails = []  # No IDs found, so no metadata to fetch
                    logger.info(
                        f"No message IDs found for labelId: {label_id}, skipping "
                        "batch metadata fetch.",
                    )

                return jsonify(emails)

            except (AuthError, GmailApiError):
                # Let these be caught by the global handler
                raise
            except Exception as e:
                logger.exception(f"Error listing emails for labelId={label_id}")
                raise GmailApiError(f"Error listing emails: {str(e)}") from e

        @self.app.route("/emails/<message_id>", methods=["GET"])
        def get_email(message_id):
            """Get details for a specific email"""
            # Check authentication
            if not self.auth_service.check_auth_status():
                raise AuthError("Not authenticated")

            try:
                # Get Gmail service
                gmail_service = self._get_authenticated_gmail_service()
                if not gmail_service:
                    raise GmailApiError("Failed to initialize Gmail service")

                # Get message details
                details = gmail_service.get_message_details(message_id)
                if not details:
                    raise NotFoundError(f"Message with ID {message_id} not found")

                return jsonify(details)

            except (AuthError, GmailApiError, NotFoundError):
                # Let these be caught by the global handler
                raise
            except Exception as e:
                logger.exception(f"Error getting email {message_id}")
                raise GmailApiError(f"Error retrieving email: {str(e)}") from e

        @self.app.route("/emails/<message_id>/suggestions", methods=["GET"])
        def get_suggestions(message_id):
            """Get AI-generated reply suggestions for an email"""
            # Check authentication
            if not self.auth_service.check_auth_status():
                raise AuthError("Not authenticated")

            try:
                # Get Gmail service
                gmail_service = self._get_authenticated_gmail_service()
                if not gmail_service:
                    raise GmailApiError("Failed to initialize Gmail service")

                # Get message details
                details = gmail_service.get_message_details(message_id)
                if not details:
                    raise NotFoundError(f"Message with ID {message_id} not found")

                # Check if this is part of a thread/conversation
                thread_context = ""
                is_reply = False

                if "in_reply_to" in details and details["in_reply_to"]:
                    is_reply = True
                    logger.info(
                        f"Email {message_id} is a reply - building conversation "
                        "context",
                    )

                    # Get thread ID from the message
                    thread_id = details.get("thread_id")
                    if thread_id:
                        # Get all messages in this thread to build context
                        thread_messages = gmail_service.list_messages(
                            thread_id=thread_id,
                        )

                        if thread_messages:
                            # Sort messages by date (oldest first) to establish the
                            # conversation order
                            conversation = []

                            for msg_id in thread_messages:
                                if msg_id != message_id:  # Skip the current message
                                    try:
                                        msg_details = gmail_service.get_message_details(
                                            msg_id,
                                        )
                                        if msg_details:
                                            conversation.append(
                                                {
                                                    "from": msg_details.get(
                                                        "from",
                                                        "(Unknown)",
                                                    ),
                                                    "body": msg_details.get("body", ""),
                                                    "date": msg_details.get("date", ""),
                                                },
                                            )
                                    except Exception as e:
                                        logger.warning(
                                            (
                                                "Error getting thread message "
                                                f"{msg_id}: {e}"
                                            ),
                                        )

                            # Build thread context (limit to 2 previous messages)
                            # to avoid excessive context
                            if conversation:
                                # Sort by date
                                conversation.sort(key=lambda x: x.get("date", ""))

                                # Take up to 2 most recent messages
                                recent_messages = (
                                    conversation[-2:]
                                    if len(conversation) > 2
                                    else conversation
                                )

                                # Get the user's email address for better context
                                # identification
                                user_email = gmail_service.get_user_email()

                                # Build context string
                                thread_context = (
                                    "Previous messages in this conversation:\n\n"
                                )
                                for i, msg in enumerate(recent_messages):
                                    from_part = msg.get("from", "(Unknown)")
                                    # Truncate the message body if too long
                                    body = msg.get("body", "")
                                    if len(body) > 500:
                                        body = body[:500] + "... [message truncated]"

                                    # Identify if this message is from the user
                                    sender_type = (
                                        "You"
                                        if user_email and user_email in from_part
                                        else "Other party"
                                    )

                                    thread_context += (
                                        f"------- Message {i + 1} -------\n"
                                    )
                                    thread_context += (
                                        f"From: {from_part} ({sender_type})\n"
                                    )
                                    thread_context += f"{body}\n\n"

                                # For the current message, also identify if it is from
                                # the user
                                current_from = details.get("from", "(Unknown)")
                                sender_type = (
                                    "You"
                                    if user_email and user_email in current_from
                                    else "Other party"
                                )

                                thread_context += "------- Current Email -------\n"
                                thread_context += (
                                    f"From: {current_from} ({sender_type})\n"
                                )
                                thread_context += f"{details.get('body', '')}\n"

                # Get suggestions from Ollama
                if is_reply and thread_context:
                    logger.info("Generating suggestions with thread context")

                    # Check if the current email is from the user (replying to self)
                    user_email = gmail_service.get_user_email()
                    current_from = details.get("from", "")
                    is_replying_to_self = user_email and user_email in current_from

                    if is_replying_to_self:
                        logger.info("User is attempting to reply to their own email")
                        # Provide special suggestions for this case
                        suggestions = [
                            (
                                "Did you mean to add more information to your previous "
                                "message?"
                            ),
                            (
                                "I see you're replying to your own message. "
                                "Did you want to follow up with the recipient?"
                            ),
                            (
                                "Would you like to send a reminder about this "
                                "conversation?"
                            ),
                        ]
                    else:
                        # Normal case - get suggestions with context
                        suggestions = self.llm_service.get_suggestions_with_context(
                            details["body"],
                            thread_context,
                            is_reply,
                        )
                else:
                    suggestions = self.llm_service.get_suggestions(details["body"])

                if suggestions is None:
                    raise OllamaError("Failed to generate suggestions")

                return jsonify({"suggestions": suggestions})

            except (AuthError, GmailApiError, NotFoundError, OllamaError):
                # Let these be caught by the global handler
                raise
            except Exception as e:
                logger.exception(f"Error getting suggestions for email {message_id}")
                raise OllamaError(f"Error generating suggestions: {str(e)}") from e

        @self.app.route("/emails/<message_id>/archive", methods=["POST"])
        def archive_email(message_id):
            """Archive a specific email"""
            # Check authentication
            if not self.auth_service.check_auth_status():
                raise AuthError("Not authenticated")

            try:
                # Get Gmail service
                gmail_service = self._get_authenticated_gmail_service()
                if not gmail_service:
                    raise GmailApiError("Failed to initialize Gmail service")

                # Archive the message
                result = gmail_service.archive_message(message_id)
                if not result:
                    raise GmailApiError("Failed to archive email")

                return jsonify(
                    {
                        "success": True,
                        "message": "Email archived successfully",
                        "message_id": message_id,
                    },
                )

            except (AuthError, GmailApiError, NotFoundError):
                # Let these be caught by the global handler
                raise
            except Exception as e:
                logger.exception(f"Error archiving email {message_id}")
                raise GmailApiError(f"Error archiving email: {str(e)}") from e

        @self.app.route("/emails/<message_id>/delete", methods=["DELETE"])
        def delete_email(message_id):
            """Delete a specific email (move to trash)"""
            # Check authentication
            if not self.auth_service.check_auth_status():
                raise AuthError("Not authenticated")

            try:
                # Get Gmail service
                gmail_service = self._get_authenticated_gmail_service()
                if not gmail_service:
                    raise GmailApiError("Failed to initialize Gmail service")

                # Delete the message
                result = gmail_service.delete_message(message_id)
                if not result:
                    raise GmailApiError("Failed to delete email")

                return jsonify(
                    {
                        "success": True,
                        "message": "Email deleted successfully",
                        "message_id": message_id,
                    },
                )

            except (AuthError, GmailApiError, NotFoundError):
                # Let these be caught by the global handler
                raise
            except Exception as e:
                logger.exception(f"Error deleting email {message_id}")
                raise GmailApiError(f"Error deleting email: {str(e)}") from e

        @self.app.route("/emails/send", methods=["POST"])
        def send_email():
            """Send a new email"""
            # Check authentication
            if not self.auth_service.check_auth_status():
                raise AuthError("Not authenticated")

            try:
                # Get request data
                data = request.json
                if not data:
                    raise ValidationError("No data provided")

                # Extract required fields
                to = data.get("to")
                subject = data.get("subject")
                body = data.get("body")
                reply_to = data.get("reply_to")  # Optional: message ID being replied to

                # Validate required fields
                if not to:
                    raise ValidationError("Recipient (to) is required")
                if not subject:
                    raise ValidationError("Subject is required")
                if not body:
                    raise ValidationError("Message body is required")

                # Get Gmail service
                gmail_service = self._get_authenticated_gmail_service()
                if not gmail_service:
                    raise GmailApiError("Failed to initialize Gmail service")

                # Send the email
                result = gmail_service.send_email(to, subject, body, reply_to)
                if not result:
                    raise GmailApiError("Failed to send email")

                return jsonify(
                    {
                        "success": True,
                        "message": "Email sent successfully",
                        "message_id": result.get("id"),
                    },
                )

            except (AuthError, GmailApiError, ValidationError):
                # Let these be caught by the global handler
                raise
            except Exception as e:
                logger.exception("Error sending email")
                raise GmailApiError(f"Error sending email: {str(e)}") from e

        @self.app.route("/emails/<message_id>/modify", methods=["POST"])
        def modify_email_labels(message_id):
            """Modify email labels (add/remove)"""
            # Check authentication
            if not self.auth_service.check_auth_status():
                raise AuthError("Not authenticated")

            try:
                # Get request data
                data = request.json
                if not data:
                    raise ValidationError("No data provided")

                # Extract label modifications
                add_labels = data.get("addLabelIds", [])
                remove_labels = data.get("removeLabelIds", [])

                # Validate that we have at least one modification
                if not add_labels and not remove_labels:
                    raise ValidationError("No label modifications specified")

                # Get Gmail service
                gmail_service = self._get_authenticated_gmail_service()
                if not gmail_service:
                    raise GmailApiError("Failed to initialize Gmail service")

                # Modify the message labels
                result = gmail_service.modify_message_labels(
                    message_id,
                    add_labels,
                    remove_labels,
                )
                if not result:
                    raise GmailApiError("Failed to modify email labels")

                return jsonify(
                    {
                        "success": True,
                        "message": "Email labels modified successfully",
                        "message_id": message_id,
                    },
                )

            except (AuthError, GmailApiError, NotFoundError, ValidationError):
                # Let these be caught by the global handler
                raise
            except Exception as e:
                logger.exception(f"Error modifying email labels for {message_id}")
                raise GmailApiError(f"Error modifying email labels: {str(e)}") from e

        # Status endpoint
        @self.app.route("/status", methods=["GET"])
        def status():
            """Get overall backend status"""
            try:
                gmail_authenticated = self.auth_service.check_auth_status()
                ai_service_status = (
                    "active" if self.llm_service.check_status() else "inactive"
                )

                return jsonify(
                    {
                        "gmail_authenticated": gmail_authenticated,
                        "local_ai_service_status": ai_service_status,
                    },
                )
            except Exception as e:
                logger.exception("Error checking status")
                raise ServiceError(f"Error checking service status: {str(e)}") from e

    def _get_authenticated_gmail_service(self):
        """
        Get an authenticated Gmail service instance.

        Returns:
            GmailApiService instance or None if authentication fails
        """
        try:
            # Get credentials
            credentials = self.auth_service.get_credentials()
            if not credentials:
                logger.error("Failed to get valid credentials")
                return None

            # Create Gmail service if needed
            if not self.gmail_service:
                self.gmail_service = GmailApiService(credentials, self.config_manager)

            return self.gmail_service

        except Exception as e:
            logger.error(f"Error getting Gmail service: {e}")
            return None

    def run(self):
        """Run the API server"""
        host = self.config_manager.get("API", "host", fallback="127.0.0.1")
        port = self.config_manager.getint("API", "port", fallback=5000)

        logger.info(f"Starting API server on {host}:{port}")
        self.app.run(host=host, port=port, debug=False)


# Entry point for running the server
def main():
    """Main entry point for the API server"""
    server = ApiServer()
    server.run()


if __name__ == "__main__":
    main()
