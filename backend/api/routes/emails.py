"""
Email related routes (list, get, suggest, send, archive, delete, modify)
"""

import logging

from flask import Blueprint, current_app, jsonify, request

from ...util.exceptions import (
    AuthError,
    GmailApiError,
    NotFoundError,
    OllamaError,
    ValidationError,
)

logger = logging.getLogger(__name__)

emails_bp = Blueprint("emails_bp", __name__, url_prefix="/emails")


@emails_bp.route("", methods=["GET"])
def list_emails():
    """List emails using a specific label ID."""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    config_manager = current_app.config["SERVICES"]["config_manager"]
    get_gmail_service = current_app.config["SERVICES"]["get_gmail_service"]

    if not auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    label_id = request.args.get("labelId", default="INBOX", type=str)
    max_results = request.args.get(
        "maxResults",
        default=config_manager.getint("App", "max_emails_fetch"),
        type=int,
    )

    logger.info(
        f"API request: List emails for labelId='{label_id}', maxResults={max_results}",
    )

    try:
        gmail_service = get_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

        message_ids = gmail_service.list_messages(
            label_id=label_id,
            max_results=max_results,
        )
        if message_ids is None:
            logger.warning(f"list_messages returned None for labelId: {label_id}")
            message_ids = []

        logger.info(f"Found {len(message_ids)} message IDs for labelId: {label_id}")

        emails = []
        if message_ids:
            # Chunk size for metadata fetching (Reduced from 25)
            METADATA_BATCH_CHUNK_SIZE = 15

            # Dictionary to store all metadata results
            all_metadata_results = {}

            # Process message IDs in chunks to avoid hitting API rate limits
            logger.info(
                f"Processing {len(message_ids)} IDs in chunks "
                f"of {METADATA_BATCH_CHUNK_SIZE}...",
            )
            for i in range(0, len(message_ids), METADATA_BATCH_CHUNK_SIZE):
                chunk_ids = message_ids[i : i + METADATA_BATCH_CHUNK_SIZE]
                chunk_num = i // METADATA_BATCH_CHUNK_SIZE + 1
                chunk_size = len(chunk_ids)

                logger.debug(
                    f"Fetching metadata for chunk {chunk_num}, size: {chunk_size}",
                )
                try:
                    # Fetch metadata for just this chunk of IDs
                    chunk_metadata_dict = gmail_service.get_multiple_messages_metadata(
                        chunk_ids,
                    )

                    # Merge results into the main dictionary
                    for msg_id, metadata in chunk_metadata_dict.items():
                        if metadata is not None:
                            all_metadata_results[msg_id] = metadata
                except Exception as chunk_error:
                    # Log the error but continue with other chunks
                    logger.error(
                        f"Error fetching metadata for chunk {chunk_num}: {chunk_error}",
                    )

            # Convert aggregated results to final list, filtering out None values
            emails = [
                metadata
                for metadata in all_metadata_results.values()
                if metadata is not None
            ]
            logger.info(
                f"Successfully fetched metadata for {len(emails)} out of "
                f"{len(message_ids)} emails via chunked batch processing "
                f"for labelId: {label_id}",
            )
        else:
            logger.info(
                f"No message IDs found for labelId: {label_id}, "
                f"skipping batch metadata fetch.",
            )

        return jsonify(emails)

    except (AuthError, GmailApiError):
        raise
    except Exception as e:
        logger.exception(f"Error listing emails for labelId={label_id}")
        raise GmailApiError(f"Error listing emails: {str(e)}") from e


@emails_bp.route("/<message_id>", methods=["GET"])
def get_email(message_id):
    """Get details for a specific email"""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    get_gmail_service = current_app.config["SERVICES"]["get_gmail_service"]

    if not auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    try:
        gmail_service = get_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

        details = gmail_service.get_message_details(message_id)
        if not details:
            raise NotFoundError(f"Message with ID {message_id} not found")

        return jsonify(details)

    except (AuthError, GmailApiError, NotFoundError):
        raise
    except Exception as e:
        logger.exception(f"Error getting email {message_id}")
        raise GmailApiError(f"Error retrieving email: {str(e)}") from e


@emails_bp.route("/<message_id>/suggestions", methods=["GET"])
def get_suggestions(message_id):
    """Get AI-generated reply suggestions for an email"""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    llm_service = current_app.config["SERVICES"]["llm_service"]
    get_gmail_service = current_app.config["SERVICES"]["get_gmail_service"]

    if not auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    try:
        gmail_service = get_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

        details = gmail_service.get_message_details(message_id)
        if not details:
            raise NotFoundError(f"Message with ID {message_id} not found")

        thread_context = ""
        is_reply = False

        if "in_reply_to" in details and details["in_reply_to"]:
            is_reply = True
            logger.info(
                f"Email {message_id} is a reply - building conversation context",
            )
            thread_id = details.get("thread_id")
            if thread_id:
                thread_messages_ids = gmail_service.list_messages(thread_id=thread_id)
                if thread_messages_ids:
                    details_list = []
                    for msg_id in thread_messages_ids:
                        try:
                            msg_details = gmail_service.get_message_details(msg_id)
                            if msg_details:
                                details_list.append(msg_details)
                        except Exception as e:
                            logger.warning(
                                f"Error getting thread message detail {msg_id}: {e}",
                            )
                    try:
                        details_list.sort(key=lambda x: x.get("internalDate", "0"))
                    except Exception as e:
                        logger.warning(f"Could not sort thread messages by date: {e}")

                    current_message_index = -1
                    for i, msg in enumerate(details_list):
                        if msg.get("id") == message_id:
                            current_message_index = i
                            break

                    if current_message_index > 0:
                        context_messages = details_list[
                            max(0, current_message_index - 2) : current_message_index
                        ]
                        user_email = gmail_service.get_user_email()
                        thread_context = "Previous messages in this conversation:\n\n"
                        for i, msg in enumerate(context_messages):
                            from_part = msg.get("from", "(Unknown)")
                            body = msg.get("body", "")
                            if len(body) > 500:
                                body = body[:500] + "... [message truncated]"
                            sender_type = (
                                "You"
                                if user_email and user_email in from_part
                                else "Other party"
                            )
                            thread_context += f"------- Message {i + 1} -------\n"
                            thread_context += f"From: {from_part} ({sender_type})\n"
                            thread_context += f"{body}\n\n"

        if is_reply:
            user_email = gmail_service.get_user_email()
            current_from = details.get("from", "")
            is_replying_to_self = user_email and user_email in current_from

            if is_replying_to_self:
                logger.info(
                    "User is attempting to reply to their own email in thread context",
                )
                suggestions = [
                    "Did you mean to add more information to your previous message?",
                    (
                        "Replying to your own message in a thread. "
                        "Continue here or reply to "
                        "the last message from the other party?"
                    ),
                    "Forward this thread?",
                ]
            else:
                logger.info("Generating suggestions with thread context")
                suggestions = llm_service.get_suggestions_with_context(
                    details["body"],
                    thread_context,
                    is_reply,
                )
        else:
            logger.info("Generating suggestions for standalone email")
            suggestions = llm_service.get_suggestions(details["body"])

        if suggestions is None:
            raise OllamaError("Failed to generate suggestions")

        return jsonify({"suggestions": suggestions})

    except (AuthError, GmailApiError, NotFoundError, OllamaError):
        raise
    except Exception as e:
        logger.exception(f"Error getting suggestions for email {message_id}")
        raise OllamaError(f"Error generating suggestions: {str(e)}") from e


@emails_bp.route("/<message_id>/archive", methods=["POST"])
def archive_email(message_id):
    """Archive a specific email"""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    get_gmail_service = current_app.config["SERVICES"]["get_gmail_service"]

    if not auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    try:
        gmail_service = get_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

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
        raise
    except Exception as e:
        logger.exception(f"Error archiving email {message_id}")
        raise GmailApiError(f"Error archiving email: {str(e)}") from e


@emails_bp.route("/<message_id>/delete", methods=["DELETE"])
def delete_email(message_id):
    """Delete a specific email (move to trash)"""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    get_gmail_service = current_app.config["SERVICES"]["get_gmail_service"]

    if not auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    try:
        gmail_service = get_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

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
        raise
    except Exception as e:
        logger.exception(f"Error deleting email {message_id}")
        raise GmailApiError(f"Error deleting email: {str(e)}") from e


@emails_bp.route("/send", methods=["POST"])
def send_email():
    """Send a new email"""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    get_gmail_service = current_app.config["SERVICES"]["get_gmail_service"]

    if not auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    try:
        data = request.json
        if not data:
            raise ValidationError("No data provided")

        to = data.get("to")
        subject = data.get("subject")
        body = data.get("body")
        reply_to = data.get("reply_to")

        if not to:
            raise ValidationError("Recipient (to) is required")
        if not subject:
            raise ValidationError("Subject is required")
        if not body:
            raise ValidationError("Message body is required")

        gmail_service = get_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

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
        raise
    except Exception as e:
        logger.exception("Error sending email")
        raise GmailApiError(f"Error sending email: {str(e)}") from e


@emails_bp.route("/<message_id>/modify", methods=["POST"])
def modify_email_labels(message_id):
    """Modify email labels OR perform actions like trash/archive."""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    get_gmail_service = current_app.config["SERVICES"]["get_gmail_service"]

    if not auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    data = request.get_json()
    if not data:
        raise ValidationError("No data provided")

    try:
        gmail_service = get_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

        action = data.get("action")

        if action == "trash":
            logger.info(f"Trashing email {message_id} via modify endpoint")
            result = gmail_service.delete_message(message_id)
            if not result:
                raise GmailApiError("Failed to trash email")
            return jsonify({"success": True, "message": "Email trashed successfully"})

        elif action == "archive":
            logger.info(f"Archiving email {message_id} via modify endpoint")
            result = gmail_service.archive_message(message_id)
            if not result:
                raise GmailApiError("Failed to archive email")
            return jsonify({"success": True, "message": "Email archived successfully"})

        else:
            # Original label modification logic
            add_labels = data.get("addLabelIds", [])
            remove_labels = data.get("removeLabelIds", [])

            if not add_labels and not remove_labels:
                raise ValidationError("No action or label modifications specified")

            logger.info(
                f"Modifying labels for {message_id}: "
                f"add={add_labels}, remove={remove_labels}",
            )
            result = gmail_service.modify_message_labels(
                message_id,
                add_labels,
                remove_labels,
            )
            if not result:
                raise GmailApiError("Failed to modify labels")

            return jsonify(
                {
                    "success": True,
                    "message": "Labels modified successfully",
                    "message_id": message_id,
                    # Optional: return modified message resource from result
                    # if available
                },
            )

    except (AuthError, GmailApiError, NotFoundError, ValidationError):
        raise
    except Exception as e:
        logger.exception(f"Error modifying email {message_id}")
        raise GmailApiError(f"Error modifying email: {str(e)}") from e
