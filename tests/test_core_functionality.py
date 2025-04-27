"""
End-to-End Test Script for Privacy-Focused Email Agent

This script tests the core functionality of the application:
1. Backend server startup
2. Authentication status check
3. OAuth flow (manual verification)
4. Email listing
5. Email detail retrieval
6. AI suggestion generation

Usage:
    python tests/test_core_functionality.py
"""

import logging
from datetime import datetime

import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("e2e_test")

# Configuration
BACKEND_URL = "http://localhost:5000"
TEST_REPORT_PATH = "tests/test_report.txt"


def main():
    """Main test function"""
    logger.info("Starting end-to-end test of core functionality")
    logger.info(f"Backend URL: {BACKEND_URL}")

    # Create test report file
    with open(TEST_REPORT_PATH, "w") as f:
        f.write("Privacy-Focused Email Agent - Core Functionality Test Report\n")
        f.write(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"{'='*80}\n\n")

    # Run tests
    if test_backend_running():
        auth_success = test_auth_status()
        if auth_success:
            test_email_listing()

    logger.info("End-to-end test completed")
    logger.info(f"Test report saved to {TEST_REPORT_PATH}")


def test_backend_running():
    """Test if the backend server is running"""
    logger.info("Testing if backend server is running...")

    try:
        response = requests.get(f"{BACKEND_URL}/status", timeout=5)

        if response.status_code == 200:
            logger.info("Backend server is running")
            append_to_report("1. Backend Server", "PASS - Server is running")
            return True
        else:
            logger.error(f"Backend returned status code {response.status_code}")
            append_to_report(
                "1. Backend Server",
                f"FAIL - Unexpected status code: {response.status_code}",
            )
            return False

    except requests.exceptions.ConnectionError:
        logger.error("Could not connect to backend server")
        append_to_report("1. Backend Server", "FAIL - Could not connect to server")
        logger.info("Make sure the backend server is running (python run_backend.py)")
        return False
    except Exception as e:
        logger.error(f"Error checking backend: {e}")
        append_to_report("1. Backend Server", f"FAIL - Error: {str(e)}")
        return False


def test_auth_status():
    """Test authentication status with Gmail"""
    logger.info("Testing authentication status...")

    try:
        response = requests.get(f"{BACKEND_URL}/auth/status", timeout=5)

        if response.status_code == 200:
            data = response.json()
            authenticated = data.get("authenticated", False)

            if authenticated:
                logger.info("Gmail authentication is active")
                append_to_report(
                    "2. Authentication Status", "PASS - Authenticated with Gmail"
                )
                return True
            else:
                logger.info("Not authenticated with Gmail")
                append_to_report(
                    "2. Authentication Status", "INFO - Not authenticated with Gmail"
                )

                # Prompt user to authenticate
                user_choice = input(
                    "Would you like to initiate the authentication process? (y/n): "
                )
                if user_choice.lower() == "y":
                    return initiate_authentication()
                else:
                    logger.info("Authentication skipped by user")
                    append_to_report(
                        "2. Authentication Status",
                        "SKIPPED - User opted not to authenticate",
                    )
                    return False
        else:
            logger.error(
                f"Auth status check failed with status code {response.status_code}"
            )
            append_to_report(
                "2. Authentication Status",
                f"FAIL - Unexpected status code: {response.status_code}",
            )
            return False

    except Exception as e:
        logger.error(f"Error checking authentication status: {e}")
        append_to_report("2. Authentication Status", f"FAIL - Error: {str(e)}")
        return False


def initiate_authentication():
    """Initiate the OAuth authentication flow"""
    logger.info("Initiating authentication...")

    try:
        # Trigger the OAuth flow
        response = requests.post(f"{BACKEND_URL}/auth/login", timeout=5)

        if response.status_code == 200:
            data = response.json()
            if data.get("success", False):
                logger.info("Authentication process started in browser")
                append_to_report(
                    "2.1 Authentication Initiation",
                    "PASS - OAuth flow started in browser",
                )

                # Wait for user to complete the flow
                print("\nPlease complete the authentication in your browser.")
                input("Press Enter when you have completed the authentication...")

                # Verify authentication succeeded
                return verify_authentication()
            else:
                logger.error("Authentication process failed to start")
                append_to_report(
                    "2.1 Authentication Initiation",
                    f"FAIL - {data.get('message', 'Unknown error')}",
                )
                return False
        else:
            logger.error(
                f"Authentication initiation failed with status code {response.status_code}"
            )
            append_to_report(
                "2.1 Authentication Initiation",
                f"FAIL - Unexpected status code: {response.status_code}",
            )
            return False

    except Exception as e:
        logger.error(f"Error initiating authentication: {e}")
        append_to_report("2.1 Authentication Initiation", f"FAIL - Error: {str(e)}")
        return False


def verify_authentication():
    """Verify that authentication was successful"""
    logger.info("Verifying authentication success...")

    try:
        # Check auth status again
        response = requests.get(f"{BACKEND_URL}/auth/status", timeout=5)

        if response.status_code == 200:
            data = response.json()
            authenticated = data.get("authenticated", False)

            if authenticated:
                logger.info("Authentication successful!")
                append_to_report(
                    "2.2 Authentication Verification",
                    "PASS - Successfully authenticated with Gmail",
                )
                return True
            else:
                logger.error(
                    "Authentication process completed but still not authenticated"
                )
                append_to_report(
                    "2.2 Authentication Verification",
                    "FAIL - Still not authenticated after OAuth flow",
                )
                return False
        else:
            logger.error(
                f"Authentication verification failed with status code {response.status_code}"
            )
            append_to_report(
                "2.2 Authentication Verification",
                f"FAIL - Unexpected status code: {response.status_code}",
            )
            return False

    except Exception as e:
        logger.error(f"Error verifying authentication: {e}")
        append_to_report("2.2 Authentication Verification", f"FAIL - Error: {str(e)}")
        return False


def test_email_listing():
    """Test fetching the email list"""
    logger.info("Testing email listing functionality...")

    try:
        # Fetch email list
        response = requests.get(f"{BACKEND_URL}/emails", timeout=10)

        if response.status_code == 200:
            emails = response.json()

            if isinstance(emails, list):
                email_count = len(emails)
                logger.info(f"Successfully fetched {email_count} emails")

                # Check if we got some emails
                if email_count > 0:
                    # Display first few emails for verification
                    logger.info("First few emails:")
                    for i, email in enumerate(emails[:3]):
                        logger.info(
                            f"  {i+1}. Subject: {email.get('subject', 'No Subject')}"
                        )

                    # Check required fields in email objects
                    has_required_fields = all(
                        "id" in email
                        and "subject" in email
                        and "from" in email
                        and "date" in email
                        for email in emails[:5]  # Check first 5 emails
                    )

                    if has_required_fields:
                        append_to_report(
                            "3. Email Listing",
                            f"PASS - Received {email_count} emails with correct data structure",
                        )

                        # Store first email ID for later tests if available
                        if email_count > 0:
                            first_email_id = emails[0].get("id")
                            if first_email_id:
                                logger.info(
                                    f"Selected email ID for detail test: {first_email_id}"
                                )
                                test_email_detail(first_email_id)

                        return True
                    else:
                        logger.warning("Emails missing required fields")
                        append_to_report(
                            "3. Email Listing",
                            f"PARTIAL - Got {email_count} emails, but some have missing fields",
                        )
                        return False
                else:
                    logger.info("No emails received (empty inbox or folder)")
                    append_to_report(
                        "3. Email Listing", "INFO - No emails in the selected folder"
                    )
                    return True
            else:
                logger.error("Unexpected response format (not a list)")
                append_to_report(
                    "3. Email Listing", "FAIL - Response is not a list of emails"
                )
                return False
        elif response.status_code == 401:
            logger.error("Authentication required for email listing")
            append_to_report(
                "3. Email Listing", "FAIL - Authentication required or expired"
            )
            return False
        else:
            logger.error(
                f"Email listing failed with status code {response.status_code}"
            )
            append_to_report(
                "3. Email Listing",
                f"FAIL - Unexpected status code: {response.status_code}",
            )
            return False

    except Exception as e:
        logger.error(f"Error fetching email list: {e}")
        append_to_report("3. Email Listing", f"FAIL - Error: {str(e)}")
        return False


def test_email_detail(email_id):
    """Test fetching a specific email's details"""
    logger.info(f"Testing email detail retrieval for ID: {email_id}")

    try:
        # Fetch email details
        response = requests.get(f"{BACKEND_URL}/emails/{email_id}", timeout=10)

        if response.status_code == 200:
            email = response.json()

            # Check if we got the right email and it has all required fields
            if (
                email.get("id") == email_id
                and "subject" in email
                and "from" in email
                and "to" in email
                and "date" in email
                and "body" in email
            ):
                logger.info(
                    f"Successfully fetched email: {email.get('subject', 'No Subject')}"
                )
                append_to_report(
                    "4. Email Detail",
                    "PASS - Successfully retrieved email details with all fields",
                )

                # Store email ID for testing AI suggestions
                logger.info("Will use this email for AI suggestion test")
                test_email_suggestions(email_id)

                return True
            else:
                logger.warning("Email is missing required fields")
                append_to_report(
                    "4. Email Detail",
                    "PARTIAL - Retrieved email but missing some fields",
                )
                return False
        elif response.status_code == 401:
            logger.error("Authentication required for email detail")
            append_to_report(
                "4. Email Detail", "FAIL - Authentication required or expired"
            )
            return False
        elif response.status_code == 404:
            logger.error("Email not found")
            append_to_report("4. Email Detail", "FAIL - Email ID not found")
            return False
        else:
            logger.error(
                f"Email detail retrieval failed with status code {response.status_code}"
            )
            append_to_report(
                "4. Email Detail",
                f"FAIL - Unexpected status code: {response.status_code}",
            )
            return False

    except Exception as e:
        logger.error(f"Error fetching email detail: {e}")
        append_to_report("4. Email Detail", f"FAIL - Error: {str(e)}")
        return False


def test_email_suggestions(email_id):
    """Test generating AI suggestions for an email"""
    logger.info(f"Testing AI suggestion generation for email ID: {email_id}")

    try:
        # Get suggestions
        response = requests.get(
            f"{BACKEND_URL}/emails/{email_id}/suggestions", timeout=30
        )  # Longer timeout for AI

        if response.status_code == 200:
            data = response.json()
            suggestions = data.get("suggestions", [])

            if isinstance(suggestions, list) and len(suggestions) > 0:
                logger.info(f"Successfully generated {len(suggestions)} suggestions")

                # Display the suggestions
                for i, suggestion in enumerate(suggestions):
                    logger.info(f"  {i+1}. {suggestion}")

                append_to_report(
                    "5. AI Suggestions",
                    f"PASS - Generated {len(suggestions)} suggestions",
                )
                return True
            else:
                logger.warning("No suggestions were generated")
                append_to_report(
                    "5. AI Suggestions",
                    "PARTIAL - API call succeeded but no suggestions generated",
                )
                return False
        elif response.status_code == 401:
            logger.error("Authentication required for suggestion generation")
            append_to_report(
                "5. AI Suggestions", "FAIL - Authentication required or expired"
            )
            return False
        elif response.status_code == 404:
            logger.error("Email not found when generating suggestions")
            append_to_report("5. AI Suggestions", "FAIL - Email ID not found")
            return False
        else:
            logger.error(
                f"Suggestion generation failed with status code {response.status_code}"
            )
            append_to_report(
                "5. AI Suggestions",
                f"FAIL - Unexpected status code: {response.status_code}",
            )
            return False

    except Exception as e:
        logger.error(f"Error generating suggestions: {e}")
        append_to_report("5. AI Suggestions", f"FAIL - Error: {str(e)}")
        return False


def append_to_report(section, message):
    """Append a message to the test report"""
    with open(TEST_REPORT_PATH, "a") as f:
        f.write(f"{section}:\n")
        f.write(f"  {message}\n\n")


if __name__ == "__main__":
    main()
