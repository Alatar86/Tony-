"""
GmailApiService for Privacy-Focused Email Agent

This service handles interactions with the Gmail API.
"""

import base64
import logging
import socket
from email.mime.text import MIMEText
from email.utils import formatdate, make_msgid
from typing import Any, Dict, List, Optional

import httplib2
from google_auth_httplib2 import AuthorizedHttp
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

from ..util.exceptions import GmailApiError, NotFoundError, ValidationError

logger = logging.getLogger(__name__)


class GmailApiService:
    """
    Service responsible for interacting with the Gmail API.
    """

    def __init__(self, credentials: Any, config_manager: Any) -> None:
        """
        Initialize the GmailApiService.

        Args:
            credentials: Google OAuth credentials
            config_manager: ConfigurationManager instance for accessing configuration

        Raises:
            GmailApiError: If service creation fails
        """
        self.credentials = credentials
        self.config_manager = config_manager
        self.service: Optional[Any] = None  # Make service Optional

        # Get configuration parameters
        self.max_results = config_manager.getint("App", "max_emails_fetch", fallback=50)
        self.api_timeout = config_manager.getint(
            "Gmail",
            "api_timeout_sec",
            fallback=60,
        )

        logger.info(f"GmailApiService initialized with timeout: {self.api_timeout}s")

        # Build the Gmail API service
        try:
            self._get_service()
        except Exception as e:
            logger.error(f"Failed to initialize Gmail service: {e}")
            raise GmailApiError(f"Failed to initialize Gmail service: {str(e)}") from e

    def _get_service(self) -> Any:
        """
        Build and return the Gmail API service object with timeout configuration.

        Returns:
            Resource: Gmail API service object

        Raises:
            GmailApiError: If service creation fails
        """
        try:
            if not self.service:
                # Create an http object with timeout settings
                http = httplib2.Http(timeout=self.api_timeout)

                # Create an authorized http object with our credentials
                authorized_http = AuthorizedHttp(self.credentials, http=http)

                # Build the Gmail API service with the authorized http object
                self.service = build("gmail", "v1", http=authorized_http)

                logger.info(
                    f"Gmail API service created successfully with {self.api_timeout}s timeout",  # noqa: E501
                )
            return self.service
        except httplib2.HttpLib2Error as e:
            logger.error(f"HTTP client error creating Gmail API service: {e}")
            raise GmailApiError(f"HTTP client error: {str(e)}") from e
        except socket.timeout as e:
            logger.error(f"Timeout while creating Gmail API service: {e}")
            raise GmailApiError(
                f"Timeout while creating Gmail API service: {str(e)}",
                code=504,
            ) from e
        except Exception as e:
            logger.error(f"Failed to create Gmail API service: {e}")
            raise GmailApiError(f"Failed to create Gmail API service: {str(e)}") from e

    def list_messages(self, label_id: Optional[str] = None, thread_id: Optional[str] = None, max_results: int = 50) -> List[str]:
        """
        List messages, optionally filtered by label.

        Args:
            label_id (str, optional): Gmail label ID (e.g., 'INBOX', 'SENT').
                Defaults to None.
            thread_id (str, optional): Thread ID to list messages from.
                Defaults to None.
            max_results (int, optional): Maximum number of messages to return.
                Defaults to 50.

        Returns:
            list: List of message IDs

        Raises:
            GmailApiError: If API call fails
            ConfigError: If configuration is invalid
        """
        try:
            # Ensure we have a service
            service = self._get_service()

            # Build query parameters
            query_params = {
                "userId": "me",
                "maxResults": max_results,
            }

            # Add labelIds parameter if provided
            if label_id:
                query_params["labelIds"] = [label_id]

            # Add threadId parameter as a search query parameter if provided
            if thread_id:
                query_params["q"] = f"threadId:{thread_id}"

            if label_id:
                logger.debug(f"Fetching messages with label: {label_id}")
            if thread_id:
                logger.debug(f"Fetching messages in thread: {thread_id}")

            # Call the Gmail API
            response = service.users().messages().list(**query_params).execute()

            # Extract message IDs
            messages = []
            if "messages" in response:
                messages = [msg["id"] for msg in response["messages"]]

            logger.debug(f"Found {len(messages)} messages")
            return messages

        except HttpError as error:
            logger.error(f"HTTP error while listing messages: {error}")
            raise GmailApiError(
                f"Failed to list messages: {str(error)}",
                code=error.resp.status,
            ) from error
        except socket.timeout as e:
            logger.error(f"Timeout while listing messages: {e}")
            raise GmailApiError(
                f"Request to list messages timed out after {self.api_timeout}s",
                code=504,
            ) from e
        except httplib2.HttpLib2Error as e:
            logger.error(f"HTTP client error while listing messages: {e}")
            raise GmailApiError(
                f"HTTP client error while listing messages: {str(e)}"
            ) from e
        except Exception as e:
            logger.error(f"Error listing messages: {e}")
            raise GmailApiError(f"Error listing messages: {str(e)}") from e

    def get_message_metadata(self, message_id: str) -> Dict[str, Any]:
        """
        Get metadata for a specific message.

        Args:
            message_id (str): Gmail message ID

        Returns:
            dict: Message metadata including 'Subject', 'From', 'Date'

        Raises:
            GmailApiError: If API call fails
            NotFoundError: If the message is not found
        """
        try:
            # Ensure we have a service
            service = self._get_service()

            logger.debug(f"Fetching metadata for message: {message_id}")

            # Call the Gmail API to get message with only headers
            message = (
                service.users()
                .messages()
                .get(
                    userId="me",
                    id=message_id,
                    format="metadata",
                    metadataHeaders=["Subject", "From", "Date"],
                )
                .execute()
            )

            # Extract headers
            headers = {}
            for header in message["payload"]["headers"]:
                name = header["name"]
                value = header["value"]
                headers[name] = value

            # Create metadata dictionary
            metadata = {
                "id": message_id,
                "subject": headers.get("Subject", "(No subject)"),
                "from": headers.get("From", "(Unknown sender)"),
                "date": headers.get("Date", "(Unknown date)"),
                "labelIds": message.get("labelIds", []),
            }

            return metadata

        except HttpError as error:
            if error.resp.status == 404:
                logger.warning(f"Message not found: {message_id}")
                raise NotFoundError(f"Message not found: {message_id}") from error
            logger.error(f"HTTP error while getting message metadata: {error}")
            raise GmailApiError(
                f"Failed to get message metadata: {str(error)}",
                code=error.resp.status,
            ) from error
        except Exception as e:
            logger.error(f"Error getting message metadata: {e}")
            raise GmailApiError(f"Error getting message metadata: {str(e)}") from e

    def get_multiple_messages_metadata(self, message_ids: List[str]) -> Dict[str, Optional[Dict[str, Any]]]:
        """
        Fetch metadata for multiple messages efficiently using batch requests.

        Args:
            message_ids: A list of message IDs to fetch metadata for.

        Returns:
            A dictionary where keys are message IDs and values are metadata dicts
            (containing subject, from, date etc.), or None if metadata fetch
            failed for that ID.
            Returns an empty dict if input list is empty.
        """
        if not message_ids:
            return {}

        metadata_results: Dict[str, Optional[Dict[str, Any]]] = {}  # Dictionary to store results, keyed by message_id

        def _batch_callback(request_id: str, response: Optional[Dict[str, Any]], exception: Optional[Exception]) -> None:
            """Callback function for the batch request."""
            if exception:
                # Handle errors for individual requests if needed
                # Check if it's an HttpError and potentially log status code
                if isinstance(exception, HttpError):
                    logger.warning(
                        f"Error {exception.resp.status} fetching metadata for ",
                        f"message ID {request_id} in batch: {exception}",
                    )
                else:
                    logger.warning(
                        f"Error fetching metadata for message ID {request_id} "
                        f"in batch: {exception}",
                    )
                metadata_results[request_id] = None  # Mark as failed
            else:
                # Process successful response - similar to old get_message_metadata
                try:
                    # Reuse the header parsing logic if it exists as a helper
                    # If not, extract parsing logic from old get_message_metadata here
                    if response is None:
                        metadata_results[request_id] = None
                        return

                    headers = response.get("payload", {}).get("headers", [])
                    subject = next(
                        (h["value"] for h in headers if h["name"].lower() == "subject"),
                        "(No subject)",
                    )  # Provide default
                    from_addr = next(
                        (h["value"] for h in headers if h["name"].lower() == "from"),
                        "(Unknown sender)",
                    )  # Provide default
                    date_str = next(
                        (h["value"] for h in headers if h["name"].lower() == "date"),
                        "(Unknown date)",
                    )  # Provide default

                    metadata_results[request_id] = {
                        "id": request_id,  # Use request_id which is the message_id
                        "subject": subject,
                        "from": from_addr,
                        "date": date_str,
                        "labelIds": response.get("labelIds", []),
                        # Add other relevant metadata fields if needed
                    }
                except Exception as parse_error:
                    logger.error(
                        f"Error parsing metadata for message ID {request_id} "
                        f"in batch callback: {parse_error}",
                    )
                    metadata_results[request_id] = None  # Mark as failed

        try:
            service = self._get_service()  # Get the authorized service object
            # Create a Gmail batch request for metadata operations
            batch = service.new_batch_http_request(callback=_batch_callback)

            logger.info(
                f"Creating batch request for {len(message_ids)} messages metadata.",
            )
            for msg_id in message_ids:
                # Ensure message ID is valid before adding
                if msg_id and isinstance(msg_id, str):
                    batch.add(
                        service.users()
                        .messages()
                        .get(
                            userId="me",
                            id=msg_id,
                            format="metadata",
                            metadataHeaders=[
                                "Subject",
                                "From",
                                "Date",
                            ],  # Explicitly request headers needed
                        ),
                        # Use message_id as request_id for callback mapping
                        request_id=msg_id,
                    )
                else:
                    logger.warning(
                        f"Skipping invalid message ID in batch request: {msg_id}",
                    )

            # Execute the batch request.
            # The new_batch_http_request ensures correct http and batch path
            batch.execute()

            processed = len(metadata_results)
            successful = len([v for v in metadata_results.values() if v])
            logger.info(
                "Batch request executed. Processed %s results. "
                "Returning %s successful.",
                processed,
                successful,
            )
            return metadata_results

        except HttpError as error:
            logger.error(f"HTTP error during batch metadata fetch execution: {error}")
            # Depending on how you want to handle total batch failure:
            # Option 1: Return partial results collected so far (might be empty)
            # return metadata_results
            # Option 2: Raise an exception (current choice)
            raise GmailApiError(
                f"Batch request failed: {str(error)}",
                code=error.resp.status,
            ) from error
        except socket.timeout as e:
            logger.error(f"Timeout during batch metadata fetch execution: {e}")
            raise GmailApiError(
                f"Batch request timed out after {self.api_timeout}s",
                code=504,
            ) from e
        except httplib2.HttpLib2Error as e:
            logger.error(f"HTTP client error during batch request: {e}")
            raise GmailApiError(
                f"HTTP client error during batch request: {str(e)}"
            ) from e
        except Exception as e:
            logger.error(
                f"Unexpected error during batch metadata fetch execution: {e}",
                exc_info=True,
            )
            # Option 1: Return partial results
            # return metadata_results
            # Option 2: Raise an exception (current choice)
            raise GmailApiError(
                f"Batch request failed due to unexpected error: {str(e)}",
            ) from e

    def get_message_details(self, message_id: str) -> Dict[str, Any]:
        """
        Get the full details of a specific message including body.

        Args:
            message_id (str): Gmail message ID

        Returns:
            dict: Message details including headers and body

        Raises:
            GmailApiError: If API call fails
            NotFoundError: If the message is not found
        """
        try:
            # Ensure we have a service
            service = self._get_service()

            logger.debug(f"Fetching full details for message: {message_id}")

            # Call the Gmail API to get full message
            message = (
                service.users()
                .messages()
                .get(
                    userId="me",
                    id=message_id,
                    format="full",
                )
                .execute()
            )

            # Extract headers
            headers = {}
            for header in message["payload"]["headers"]:
                name = header["name"]
                value = header["value"]
                headers[name] = value

            # Extract body - get only the text component from the tuple
            body_text, is_html = self._get_body_text(message["payload"])

            # Extract threading headers
            message_id_header = self._extract_message_id_header(headers)
            references = self._extract_references_header(headers)

            # Create details dictionary
            details = {
                "id": message_id,
                "subject": headers.get("Subject", "(No subject)"),
                "from": headers.get("From", "(Unknown sender)"),
                "to": headers.get("To", "(Unknown recipient)"),
                "date": headers.get("Date", "(Unknown date)"),
                "body": body_text,
                # Flag indicating if content was originally HTML
                "is_html": is_html,
                "thread_id": message.get("threadId", ""),
                "message_id": message_id_header,
                "references": references,
                "in_reply_to": headers.get("In-Reply-To", ""),
            }

            return details

        except HttpError as error:
            if error.resp.status == 404:
                logger.warning(f"Message not found: {message_id}")
                raise NotFoundError(f"Message not found: {message_id}") from error
            logger.error(f"HTTP error while getting message details: {error}")
            raise GmailApiError(
                f"Failed to get message details: {str(error)}",
                code=error.resp.status,
            ) from error
        except socket.timeout as e:
            logger.error(f"Timeout while getting message details: {e}")
            raise GmailApiError(
                f"Request to get message details timed out after {self.api_timeout}s",
                code=504,
            ) from e
        except httplib2.HttpLib2Error as e:
            logger.error(f"HTTP client error getting message details: {e}")
            raise GmailApiError(
                f"HTTP client error while getting message details: {str(e)}"
            ) from e
        except Exception as e:
            logger.error(f"Error getting message details: {e}")
            raise GmailApiError(f"Error getting message details: {str(e)}") from e

    def archive_message(self, message_id: str) -> Dict[str, Any]:
        """
        Archive a message by removing the INBOX label.

        Args:
            message_id (str): Gmail message ID

        Returns:
            dict: Response from Gmail API

        Raises:
            GmailApiError: If API call fails
            NotFoundError: If the message is not found
        """
        try:
            # Ensure we have a service
            service = self._get_service()

            logger.info(f"Archiving message: {message_id}")

            # Create a label modification request
            # To archive a message, we remove the INBOX label
            label_modification = {
                "removeLabelIds": ["INBOX"],
            }

            # Call the Gmail API
            response = (
                service.users()
                .messages()
                .modify(
                    userId="me",
                    id=message_id,
                    body=label_modification,
                )
                .execute()
            )

            logger.info(f"Message archived successfully: {message_id}")
            return response  # type: ignore[no-any-return]

        except HttpError as error:
            if error.resp.status == 404:
                logger.warning(f"Message not found: {message_id}")
                raise NotFoundError(f"Message not found: {message_id}") from error
            logger.error(f"HTTP error while archiving message: {error}")
            raise GmailApiError(
                f"Failed to archive message: {str(error)}",
                code=error.resp.status,
            ) from error
        except Exception as e:
            logger.error(f"Error archiving message: {e}")
            raise GmailApiError(f"Error archiving message: {str(e)}") from e

    def delete_message(self, message_id: str) -> Dict[str, Any]:
        """
        Delete a message by sending it to trash.

        Args:
            message_id (str): Gmail message ID

        Returns:
            dict: Response from Gmail API

        Raises:
            GmailApiError: If API call fails
            NotFoundError: If the message is not found
        """
        try:
            # Ensure we have a service
            service = self._get_service()

            logger.info(f"Deleting message: {message_id}")

            # Call the Gmail API to delete the message
            response = (
                service.users()
                .messages()
                .delete(
                    userId="me",
                    id=message_id,
                )
                .execute()
            )

            logger.info(f"Message deleted successfully: {message_id}")
            return response  # type: ignore[no-any-return]

        except HttpError as error:
            if error.resp.status == 404:
                logger.warning(f"Message not found: {message_id}")
                raise NotFoundError(f"Message not found: {message_id}") from error
            logger.error(f"HTTP error while deleting message: {error}")
            raise GmailApiError(
                f"Failed to delete message: {str(error)}",
                code=error.resp.status,
            ) from error
        except Exception as e:
            logger.error(f"Error deleting message: {e}")
            raise GmailApiError(f"Error deleting message: {str(e)}") from e

    def send_email(self, to: str, subject: str, body: str, reply_to: Optional[str] = None) -> Dict[str, Any]:
        """
        Send an email through Gmail API.

        Args:
            to (str): Recipient email address
            subject (str): Email subject
            body (str): Email body
            reply_to (str, optional): Reply-to email address. Defaults to None.

        Returns:
            dict: Response from Gmail API

        Raises:
            GmailApiError: If API call fails
            ValidationError: If input parameters are invalid
        """
        try:
            # Validate required parameters
            if not to or not isinstance(to, str):
                logger.error("Invalid 'to' parameter")
                raise ValidationError("Email recipient is required and must be a string")

            if not subject or not isinstance(subject, str):
                logger.error("Invalid 'subject' parameter")
                raise ValidationError("Email subject is required and must be a string")

            if not body or not isinstance(body, str):
                logger.error("Invalid 'body' parameter")
                raise ValidationError("Email body is required and must be a string")

            # Ensure we have a service
            service = self._get_service()

            logger.info(f"Sending email to: {to}")

            # Create the email message
            message = MIMEText(body)
            message["to"] = to
            message["subject"] = subject

            # Set the From header to the user's email
            user_email = self.get_user_email()
            if user_email:
                message["from"] = user_email

            # Add a Message-ID header for tracking
            message["Message-ID"] = make_msgid()

            # Add a Date header
            message["Date"] = formatdate(localtime=True)

            # For replies, set up proper threading headers
            try:
                # For replies, construct proper threading headers
                if reply_to:
                    # Get the original message to set up proper reply headers
                    original = self.get_message_details(reply_to)

                    # Set up threading headers
                    references = []

                    # First, add original message References for threading
                    original_references = original.get("references")
                    if original_references:
                        references.extend(
                            [
                                ref.strip()
                                for ref in original_references.split()
                            ],
                        )

                    # Then add the original message's Message-ID
                    original_message_id = original.get("message_id")
                    if (
                        original_message_id
                        and original_message_id not in references
                    ):
                        references.append(original_message_id)

                    # Set the headers if we found valid references
                    if references:
                        # Set References header with all IDs
                        message["References"] = " ".join(references)

                        # Set In-Reply-To header to the direct parent message ID
                        if original_message_id:
                            message["In-Reply-To"] = original_message_id

                    else:
                        # Fallback: use Gmail ID if no Message-ID/References found
                        fallback_id = f"<{reply_to}@mail.gmail.com>"
                        message["References"] = fallback_id
                        message["In-Reply-To"] = fallback_id

                    # Normally sourced from credentials/user profile.
                    # Here we simply keep headers consistent.

            except Exception as e:
                logger.warning(f"Could not set up proper reply headers: {e}")
                # Continue without reply headers, but log the error

            # Encode the message for the Gmail API
            raw_message = base64.urlsafe_b64encode(message.as_bytes()).decode("utf-8")

            # Create the message
            gmail_message = {
                "raw": raw_message,
            }

            # If this is a reply, set the threadId to ensure proper threading in Gmail
            if reply_to:
                try:
                    # Get the thread ID from the original message
                    thread_id = self.get_message_details(reply_to).get("thread_id")
                    if thread_id:
                        gmail_message["threadId"] = thread_id
                except Exception as e:
                    logger.warning(f"Could not set thread ID: {e}")

            # Send the message
            sent_message = (
                service.users()
                .messages()
                .send(
                    userId="me",
                    body=gmail_message,
                )
                .execute()
            )

            logger.info(
                f"Email sent successfully. Message ID: {sent_message.get('id')}",
            )
            return sent_message  # type: ignore[no-any-return]

        except HttpError as error:
            logger.error(f"HTTP error while sending email: {error}")
            raise GmailApiError(
                f"Failed to send email: {str(error)}",
                code=error.resp.status,
            ) from error
        except socket.timeout as e:
            logger.error(f"Timeout while sending email: {e}")
            raise GmailApiError(
                f"Request to send email timed out after {self.api_timeout}s",
                code=504,
            ) from e
        except httplib2.HttpLib2Error as e:
            logger.error(f"HTTP client error while sending email: {e}")
            raise GmailApiError(
                f"HTTP client error while sending email: {str(e)}"
            ) from e
        except ValidationError:
            # Re-raise validation errors
            raise
        except Exception as e:
            logger.error(f"Error sending email: {e}")
            raise GmailApiError(f"Error sending email: {str(e)}") from e

    def _get_body_text(self, payload: Dict[str, Any], depth: int = 0, max_depth: int = 10) -> tuple[str, bool]:
        """
        Extract the body text from a Gmail message payload.

        Args:
            payload: Gmail message payload
            depth: Current recursion depth
            max_depth: Maximum recursion depth

        Returns:
            tuple: (body_text, is_html) where is_html indicates if content was HTML
        """
        # Guard against excessive recursion
        if depth > max_depth:
            logger.warning(
                "Maximum recursion depth reached (%s) while parsing email body",
                max_depth,
            )
            return "(Email structure too complex to parse)", False

        # Variables to store our findings
        plain_text = None
        html_text = None

        # Case 1: Direct content in payload body
        if "body" in payload and payload["body"].get("data"):
            mime_type = payload.get("mimeType", "").lower()
            body_data = self._decode_base64(payload["body"]["data"])

            if mime_type == "text/plain":
                plain_text = body_data
            elif mime_type == "text/html":
                html_text = body_data
            # Fallback: unknown type with content => assume plain text
            elif body_data:
                plain_text = body_data

        # Case 2: Multipart message - check all parts
        if "parts" in payload:
            for part in payload["parts"]:
                mime_type = part.get("mimeType", "").lower()

                # Handle direct text or html parts
                if mime_type == "text/plain" and part.get("body", {}).get("data"):
                    if plain_text is None:  # Only set if not already found
                        plain_text = self._decode_base64(part["body"]["data"])

                elif mime_type == "text/html" and part.get("body", {}).get("data"):
                    if html_text is None:  # Only set if not already found
                        html_text = self._decode_base64(part["body"]["data"])

                # Recursively check nested parts with necessary MIME types
                elif ("parts" in part) or (mime_type.startswith("multipart/")):
                    nested_text, is_html = self._get_body_text(
                        part,
                        depth + 1,
                        max_depth,
                    )
                    if nested_text:
                        if is_html and html_text is None:
                            html_text = nested_text
                        elif not is_html and plain_text is None:
                            plain_text = nested_text

        # Prioritize HTML content over plain text
        if html_text:
            # Return the raw HTML content instead of parsing it with BeautifulSoup
            return html_text, True

        # Return plain text if we have it
        if plain_text:
            return plain_text, False

        # Last resort: return an empty string
        return "", False

    def _decode_base64(self, data: str) -> str:
        """
        Decode base64 URL-safe encoded data.

        Args:
            data: Base64 encoded string

        Returns:
            str: Decoded string
        """
        try:
            # Add padding if needed
            padded_data = data + "=" * (4 - len(data) % 4) if len(data) % 4 else data

            # Decode and convert to string
            decoded_bytes = base64.urlsafe_b64decode(padded_data)

            # Try to decode as UTF-8
            try:
                return decoded_bytes.decode("utf-8")
            except UnicodeDecodeError:
                # Fallback to ISO-8859-1 (Latin-1)
                return decoded_bytes.decode("ISO-8859-1")

        except Exception as e:
            logger.error(f"Error decoding base64: {e}")
            return "(Could not decode message body)"

    def _map_folder_to_label(self, folder: str) -> str:
        """
        Map folder names to Gmail label IDs.

        Args:
            folder: Folder name

        Returns:
            str: Gmail label ID
        """
        # Define mapping for folder names to Gmail API label IDs
        label_mapping = {
            "INBOX": "INBOX",
            "STARRED": "STARRED",
            "SENT": "SENT",
            "DRAFTS": "DRAFT",
            "ARCHIVE": "ARCHIVE",
            "SPAM": "SPAM",
            "TRASH": "TRASH",
            "CATEGORY_WORK": "CATEGORY_WORK",  # Gmail category for work-related emails
            # Gmail category for personal emails
            "CATEGORY_PERSONAL": "CATEGORY_PERSONAL",
            # Add any other needed mappings here
        }

        # Return the mapped label ID or the original folder name if not in mapping
        return label_mapping.get(folder, folder)

    def _extract_message_id_header(self, headers: Dict[str, str]) -> str:
        """
        Extract the Message-ID header from email headers.

        Args:
            headers: Dictionary of email headers

        Returns:
            str: Message-ID header value
        """
        # Check for both 'Message-ID' and 'Message-Id' variants
        message_id = headers.get("Message-ID", headers.get("Message-Id", ""))

        # Ensure it's properly formatted with angle brackets
        if message_id and not (message_id.startswith("<") and message_id.endswith(">")):
            message_id = f"<{message_id}>"

        return message_id

    def _extract_references_header(self, headers: Dict[str, str]) -> List[str]:
        """
        Extract References header and return as list of message IDs.

        Args:
            headers: Dictionary of email headers

        Returns:
            list: List of message IDs from References header
        """
        references = headers.get("References", "")
        if references:
            return references.split()
        return []

    def get_user_email(self) -> Optional[str]:
        """
        Get the authenticated user's email address.

        Returns:
            str: User's email address, or None if it can't be retrieved

        Raises:
            GmailApiError: If API call fails
        """
        try:
            # Ensure we have a service
            service = self._get_service()

            # Call the Gmail API to get the user's profile
            profile = service.users().getProfile(userId="me").execute()

            email_address = profile.get("emailAddress")
            if email_address:
                logger.info(f"Retrieved user email: {email_address}")
                return email_address  # type: ignore[no-any-return]
            else:
                logger.warning("No email address found in user profile")
                return None

        except HttpError as error:
            logger.error(f"HTTP error while getting user profile: {error}")
            return None
        except Exception as e:
            logger.error(f"Error getting user profile: {e}")
            return None

    def modify_message_labels(self, message_id: str, add_labels: Optional[List[str]] = None, remove_labels: Optional[List[str]] = None) -> Dict[str, Any]:
        """
        Modify labels on a message.

        Args:
            message_id (str): Gmail message ID
            add_labels (list, optional): Labels to add. Defaults to None.
            remove_labels (list, optional): Labels to remove. Defaults to None.

        Returns:
            dict: Response from Gmail API

        Raises:
            GmailApiError: If API call fails
            NotFoundError: If the message is not found
            ValidationError: If no labels are specified
        """
        try:
            # Validate that at least one label operation is specified
            if not add_labels and not remove_labels:
                raise ValidationError("At least one of add_labels or remove_labels must be specified")

            # Ensure we have a service
            service = self._get_service()

            logger.info(f"Modifying labels for message: {message_id}")

            # Build the label modification request
            label_modification = {}
            if add_labels:
                label_modification["addLabelIds"] = add_labels
            if remove_labels:
                label_modification["removeLabelIds"] = remove_labels

            # Call the Gmail API
            response = (
                service.users()
                .messages()
                .modify(
                    userId="me",
                    id=message_id,
                    body=label_modification,
                )
                .execute()
            )

            logger.info(f"Labels modified successfully for message: {message_id}")

            return response  # type: ignore[no-any-return]

        except HttpError as error:
            if error.resp.status == 404:
                logger.warning(f"Message not found: {message_id}")
                raise NotFoundError(f"Message not found: {message_id}") from error
            logger.error(f"HTTP error while modifying message labels: {error}")
            raise GmailApiError(
                f"Failed to modify message labels: {str(error)}",
                code=error.resp.status,
            ) from error
        except Exception as e:
            logger.error(f"Error modifying message labels: {e}")
            raise GmailApiError(f"Error modifying message labels: {str(e)}") from e
