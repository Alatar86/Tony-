"""
Test Script for Email Sending Functionality

This script tests the email sending functionality in the Privacy-Focused Email Agent.
It:
1. Checks if the backend server is running
2. Verifies authentication status
3. Attempts to send a test email
4. Reports the results

Usage:
    python tests/test_email_sending.py
"""

import json
import logging
import sys
from datetime import datetime

import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("email_send_test")

# Configuration
BACKEND_URL = "http://127.0.0.1:5000"  # Using the exact IP instead of localhost
TEST_EMAIL = {
    "to": "Investchicagoland@gmail.com",  # User's actual email address
    "subject": "Test Email from Privacy-Focused Email Agent",
    "body": "This is a test email sent from the Privacy-Focused Email Agent test script.\n\nTime: "
    + datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
}


def main():
    """Main test function"""
    logger.info("Starting email sending functionality test")

    # Step 1: Check if backend is running
    try:
        response = requests.get(f"{BACKEND_URL}/status", timeout=15)
        if response.status_code != 200:
            logger.error(f"Backend server returned status code {response.status_code}")
            logger.error("Please ensure the backend server is running")
            return 1

        logger.info("Backend server is running")

        # Step 2: Check authentication status
        response = requests.get(f"{BACKEND_URL}/auth/status", timeout=15)
        if response.status_code != 200:
            logger.error(
                f"Authentication status check failed with status code {response.status_code}"
            )
            return 1

        auth_status = response.json().get("authenticated", False)
        if not auth_status:
            logger.error("Not authenticated with Gmail")
            logger.error(
                "Please run the authentication process before testing email sending"
            )
            return 1

        logger.info("Authentication status: Authenticated")

        # Step 3: Send test email
        logger.info(f"Sending test email to: {TEST_EMAIL['to']}")
        logger.info(f"Subject: {TEST_EMAIL['subject']}")

        response = requests.post(
            f"{BACKEND_URL}/emails/send",
            json=TEST_EMAIL,
            timeout=30,  # Increased timeout for email sending
        )

        if response.status_code == 200:
            result = response.json()
            if result.get("success", False):
                logger.info("Email sent successfully!")
                logger.info(f"Message ID: {result.get('message_id')}")
                return 0
            else:
                logger.error(
                    f"Email sending failed: {result.get('message', 'No error message provided')}"
                )
                return 1
        else:
            logger.error(
                f"Email sending failed with status code {response.status_code}"
            )
            try:
                error_data = response.json()
                logger.error(
                    f"Error: {error_data.get('error', 'No error message provided')}"
                )
            except json.JSONDecodeError:
                logger.error(f"Response: {response.text}")
            return 1

    except requests.exceptions.ConnectionError:
        logger.error("Could not connect to backend server")
        logger.error("Please ensure the backend server is running")
        return 1
    except Exception as e:
        logger.error(f"Error during testing: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
