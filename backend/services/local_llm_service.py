"""
LocalLlmService for Privacy-Focused Email Agent

This service handles interactions with the local Ollama instance.
"""

import logging
import time
from typing import List

import requests

from ..util.exceptions import ConfigError, OllamaError

logger = logging.getLogger(__name__)


class LocalLlmService:
    """
    Service responsible for interacting with the Ollama API.
    """

    def __init__(self, config_manager):
        """
        Initialize the LocalLlmService.

        Args:
            config_manager: ConfigurationManager instance for accessing configuration

        Raises:
            ConfigError: If required configuration is missing
        """
        try:
            self.config_manager = config_manager

            # Load configuration
            self.model_name = config_manager.get("Ollama", "model_name")
            self.api_base_url = config_manager.get("Ollama", "api_base_url")
            self.request_timeout = config_manager.getint(
                "Ollama",
                "request_timeout_sec",
                fallback=120,
            )
            self.status_timeout = config_manager.getint(
                "Ollama",
                "status_timeout_sec",
                fallback=10,
            )
            self.suggestion_prompt_template = config_manager.get(
                "Ollama",
                "suggestion_prompt_template",
            )
            self.max_retries = config_manager.getint(
                "Ollama",
                "max_retries",
                fallback=3,
            )
            self.retry_delay = config_manager.getint(
                "Ollama",
                "retry_delay_sec",
                fallback=2,
            )

            logger.info(f"LocalLlmService initialized with model: {self.model_name}")
            logger.info(f"Ollama API URL: {self.api_base_url}")
            logger.info(
                f"Request timeout: {self.request_timeout}s, "
                f"Status timeout: {self.status_timeout}s",
            )
            logger.info(
                f"Retry settings: max_retries={self.max_retries}, "
                f"retry_delay={self.retry_delay}s",
            )
        except Exception as e:
            logger.error(f"Error initializing LocalLlmService: {e}")
            raise ConfigError(f"Failed to initialize LocalLlmService: {str(e)}") from e

    def get_suggestions(self, email_body):
        """
        Get reply suggestions for an email.

        Args:
            email_body (str): Body text of the email

        Returns:
            list: List of suggestion strings

        Raises:
            OllamaError: If communication with Ollama fails
        """
        # Validate input
        if not email_body or not isinstance(email_body, str):
            logger.warning("Empty or non-string email body provided")
            raise OllamaError("Email body must be non-empty string", code=400)

        # Truncate exceptionally long emails to prevent request failures
        truncated_body = self._truncate_email(email_body)
        if truncated_body != email_body:
            logger.info(
                f"Email body truncated from {len(email_body)} to "
                f"{len(truncated_body)} characters",
            )

        # Format the prompt with the email body
        prompt = self.suggestion_prompt_template.format(email_body=truncated_body)

        for attempt in range(1, self.max_retries + 1):
            try:
                logger.debug(
                    f"Sending request to Ollama (attempt {attempt}/{self.max_retries})",  # noqa: E501
                )

                # Prepare request to Ollama API
                generate_url = f"{self.api_base_url}/api/generate"

                payload = {
                    "model": self.model_name,
                    "prompt": prompt,
                    "stream": False,
                }

                # Send request to Ollama
                response = requests.post(
                    generate_url,
                    json=payload,
                    timeout=self.request_timeout,
                )

                # Check if request was successful
                response.raise_for_status()

                # Parse response
                response_data = response.json()
                raw_response = response_data.get("response", "")

                # Process the response into individual suggestions
                suggestions = self._parse_suggestions(raw_response)

                # Validate suggestions
                if not suggestions:
                    if attempt < self.max_retries:
                        logger.warning(
                            f"Ollama returned no valid suggestions "
                            f"(attempt {attempt}/{self.max_retries}). Retrying...",
                        )
                        time.sleep(self.retry_delay)
                        continue
                    else:
                        raise OllamaError(
                            "Ollama returned empty or unparsable response",
                        )

                # If we got valid suggestions, ensure we have at least one
                if len(suggestions) == 0:
                    suggestions = ["I'll look into this and get back to you soon."]
                    logger.warning(
                        "No valid suggestions extracted, using fallback suggestion",
                    )

                logger.info(f"Generated {len(suggestions)} suggestions")
                return suggestions

            except requests.exceptions.ConnectionError as e:
                error_msg = "Ollama service not reachable. Make sure Ollama is running."
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(
                    error_msg,
                    code=503,
                    details={"service_url": self.api_base_url},
                ) from e
            except requests.exceptions.Timeout as e:
                error_msg = (
                    f"Request to Ollama timed out after {self.request_timeout} seconds"
                )
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(error_msg, code=504) from e
            except requests.exceptions.HTTPError as e:
                error_msg = f"HTTP error from Ollama API: {e}"
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(
                    f"Ollama API returned an error: {str(e)}",
                    code=e.response.status_code if hasattr(e, "response") else 500,
                ) from e
            except requests.exceptions.RequestException as e:
                error_msg = f"Request error while calling Ollama API: {e}"
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(f"Error communicating with Ollama: {str(e)}") from e
            except Exception as e:
                error_msg = f"Error generating suggestions: {e}"
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(error_msg) from e

        # This should never be reached as we always either return or raise in the loop
        raise OllamaError(
            "Unexpected error: all retries failed but no exception was raised",
        )

    def _truncate_email(self, email_body: str, max_chars: int = 4000) -> str:
        """
        Truncate email body if it's too long.

        Args:
            email_body: The email body text
            max_chars: Maximum number of characters to keep

        Returns:
            str: Truncated email body
        """
        if len(email_body) <= max_chars:
            return email_body

        # Truncate while preserving important content
        truncated = email_body[:max_chars]

        # Add an indicator that content was truncated
        truncated += "\n\n[Email truncated due to length...]"

        return truncated

    def _parse_suggestions(self, raw_response: str) -> List[str]:
        """
        Parse the raw LLM response into individual suggestions.

        Args:
            raw_response: Raw response from the LLM

        Returns:
            list: List of suggestion strings
        """
        if not raw_response:
            logger.warning("Received empty response from Ollama")
            return []

        logger.debug(f"Raw response from Ollama: {raw_response}")

        # First try to extract numbered suggestions (1. Something, 2. Something else)
        suggestions = []
        lines = raw_response.strip().split("\n")
        current_suggestion = ""

        # Track if we're in a numbered list
        in_numbered_list = False

        for line in lines:
            line = line.strip()
            if not line:
                continue

            # Check if line starts with a number and period (e.g., "1." or "2.")
            if (
                line
                and line[0].isdigit()
                and (
                    (len(line) > 1 and line[1] == ".")
                    or (len(line) > 2 and line[1:3] == ". ")
                )
            ):
                in_numbered_list = True
                # Save previous suggestion if it exists
                if current_suggestion:
                    suggestions.append(current_suggestion.strip())

                # Start a new suggestion (remove the number and period)
                if len(line) > 1 and line[1] == ".":
                    current_suggestion = line[2:].strip()
                else:
                    # Handle case like "1. " with space
                    current_suggestion = line[3:].strip() if len(line) > 3 else ""
            elif in_numbered_list and line:
                # Continue previous suggestion
                if current_suggestion:
                    # Only add a space if the current suggestion 
                    # doesn't end with punctuation
                    if current_suggestion and current_suggestion[-1] not in ".,:;!?":
                        current_suggestion += " "
                    current_suggestion += line

        # Add the last suggestion
        if current_suggestion:
            suggestions.append(current_suggestion.strip())

        # If we found valid numbered suggestions, return them
        if suggestions:
            logger.info(f"Found {len(suggestions)} numbered suggestions")
            return suggestions

        # Fallback: If no numbered suggestions, 
        # try to find any reasonable lines as suggestions
        if not suggestions:
            # Reset and try different approach - look for any reasonable sentence
            suggestions = []
            for line in lines:
                line = line.strip()
                # Skip empty lines or obvious non-suggestions
                if (
                    not line
                    or line.lower().startswith("system:")
                    or line.lower().startswith("user:")
                    or line.lower().startswith("assistant:")
                ):
                    continue

                # Skip lines that are too short or are just metadata
                if (
                    len(line) < 10
                    or line.lower().startswith("suggestion")
                    or line.lower().startswith("reply")
                ):
                    continue

                suggestions.append(line)
                # Only take the first 3 suggestions in this fallback mode
                if len(suggestions) >= 3:
                    break

        # Apply additional filtering for quality
        filtered_suggestions = []
        for suggestion in suggestions:
            # Remove any remaining numbering or formatting artifacts
            if (
                suggestion
                and suggestion[0].isdigit()
                and suggestion[1:3] in [". ", ".)"]
            ):
                suggestion = suggestion[3:].strip()

            # Skip suggestions that are too short
            if len(suggestion) < 10:
                continue

            # Skip suggestions that look like instructions
            if suggestion.lower().startswith(
                "suggest",
            ) or suggestion.lower().startswith("you could"):
                continue

            filtered_suggestions.append(suggestion)

        # If we still have no suggestions, return a fallback
        if not filtered_suggestions:
            logger.warning("Could not parse suggestions, using fallback")
            return [
                "I'll review this and get back to you soon.",
                "Thanks for your email. Let me think about this.",
                "I've received your message and will respond shortly.",
            ]

        logger.info(f"Parsed {len(filtered_suggestions)} final suggestions")
        return filtered_suggestions

    def check_status(self):
        """
        Check if Ollama service is available.

        Returns:
            bool: True if service is available, False otherwise
        """
        try:
            # Try to ping the Ollama API
            response = requests.get(
                f"{self.api_base_url}/api/tags",
                timeout=self.status_timeout,
            )

            # Check if request was successful
            if response.status_code == 200:
                # Check if our model is available
                models = response.json().get("models", [])
                for model in models:
                    if model.get("name") == self.model_name:
                        logger.info(
                            f"Ollama service is available with model "
                            f"{self.model_name}",
                        )
                        return True

                logger.warning(
                    f"Ollama service is available but model "
                    f"{self.model_name} not found",
                )
                return False
            else:
                logger.warning(
                    f"Ollama service returned status code {response.status_code}",
                )
                return False

        except requests.exceptions.Timeout:
            logger.error(
                f"Status check to Ollama timed out after {self.status_timeout} seconds",
            )
            return False
        except requests.exceptions.ConnectionError:
            logger.error(
                "Connection error during status check - Ollama service not reachable",
            )
            return False
        except requests.exceptions.RequestException as e:
            logger.error(f"Request error during status check: {e}")
            return False
        except Exception as e:
            logger.error(f"Error checking Ollama status: {e}")
            return False

    def get_suggestions_with_context(self, email_body, thread_context, is_reply=False):
        """
        Get reply suggestions for an email with conversation context.

        Args:
            email_body (str): Body text of the email
            thread_context (str): Previous messages in the thread
            is_reply (bool): Whether this is a reply to a previous message

        Returns:
            list: List of suggestion strings

        Raises:
            OllamaError: If communication with Ollama fails
        """
        # Validate input
        if not email_body or not isinstance(email_body, str):
            logger.warning("Empty or non-string email body provided")
            raise OllamaError("Email body must be non-empty string", code=400)

        # Prepare the prompt for conversation context
        # Long template as a single string with noqa comments
        reply_prompt_template = """System: You are a helpful assistant providing professional email reply suggestions. You are given a conversation thread and need to generate appropriate reply suggestions for the latest email."""  # noqa: E501

        reply_prompt_template += """

CONVERSATION CONTEXT:
{thread_context}

IMPORTANT: Pay careful attention to who sent each message in the thread. If the message marked as "Current Email" has "You" as the sender, then you are generating suggestions for replying to yourself, which would be unusual. In that case, provide suggestions that acknowledge this fact, such as adding more information to your previous email or following up with the other person."""  # noqa: E501

        reply_prompt_template += """

Based on this conversation thread, generate exactly 3 short, concise reply suggestions. Format each suggestion as a numbered list (1., 2., 3.) with each suggestion on its own line. Keep each suggestion under 20 words and make them natural, conversational responses that make sense given the full context of who sent each message."""  # noqa: E501

        reply_prompt_template += """

Your suggestions must be appropriate for the current conversational state and reflect a logical next message that the user would send.

Generate exactly 3, numbered suggestions for replying to this email:"""  # noqa: E501

        # Format the prompt with the conversation context
        prompt = reply_prompt_template.format(thread_context=thread_context)

        # We'll reuse the existing retry logic from get_suggestions
        for attempt in range(1, self.max_retries + 1):
            try:
                logger.debug(
                    f"Sending context-aware request to Ollama "
                    f"(attempt {attempt}/{self.max_retries})",
                )

                # Prepare request to Ollama API
                generate_url = f"{self.api_base_url}/api/generate"

                payload = {
                    "model": self.model_name,
                    "prompt": prompt,
                    "stream": False,
                }

                # Send request to Ollama
                response = requests.post(
                    generate_url,
                    json=payload,
                    timeout=self.request_timeout,
                )

                # Check if request was successful
                response.raise_for_status()

                # Parse response
                response_data = response.json()
                raw_response = response_data.get("response", "")

                # Process the response into individual suggestions 
                # using the existing method
                suggestions = self._parse_suggestions(raw_response)

                # Validate suggestions
                if not suggestions:
                    if attempt < self.max_retries:
                        logger.warning(
                            f"Ollama returned no valid suggestions with thread context "
                            f"(attempt {attempt}/{self.max_retries}). Retrying...",
                        )
                        time.sleep(self.retry_delay)
                        continue
                    else:
                        raise OllamaError(
                            "Ollama returned empty or unparsable response "
                            "for thread context",
                        )

                # If we got valid suggestions, ensure we have at least one
                if len(suggestions) == 0:
                    suggestions = ["I'll look into this and respond shortly."]
                    logger.warning(
                        "No valid suggestions extracted from thread context, "
                        "using fallback suggestion",
                    )

                logger.info(
                    f"Generated {len(suggestions)} suggestions with thread context",
                )
                return suggestions

            except requests.exceptions.ConnectionError as e:
                error_msg = "Ollama service not reachable. Make sure Ollama is running."
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(
                    error_msg,
                    code=503,
                    details={"service_url": self.api_base_url},
                ) from e
            except requests.exceptions.Timeout as e:
                error_msg = (
                    f"Request to Ollama timed out after {self.request_timeout} seconds"
                )
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(error_msg, code=504) from e
            except requests.exceptions.HTTPError as e:
                error_msg = f"HTTP error from Ollama API: {e}"
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(
                    f"Ollama API returned an error: {str(e)}",
                    code=e.response.status_code if hasattr(e, "response") else 500,
                ) from e
            except requests.exceptions.RequestException as e:
                error_msg = f"Request error while calling Ollama API: {e}"
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(f"Error communicating with Ollama: {str(e)}") from e
            except Exception as e:
                error_msg = f"Error generating suggestions: {e}"
                logger.error(error_msg)
                if attempt < self.max_retries:
                    logger.info(
                        f"Retrying in {self.retry_delay} seconds "
                        f"(attempt {attempt}/{self.max_retries})",
                    )
                    time.sleep(self.retry_delay)
                    continue
                raise OllamaError(error_msg) from e

        # This should never be reached as we always either return or raise in the loop
        raise OllamaError(
            "Unexpected error: all retries failed but no exception was raised",
        )
