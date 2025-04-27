"""
Test Runner for Privacy-Focused Email Agent

This script:
1. Checks if the backend server is running
2. Starts the backend server if needed
3. Runs the end-to-end tests
4. Displays the test results

Usage:
    python tests/run_tests.py
"""

import logging
import subprocess
import sys
import time
from pathlib import Path

import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("test_runner")

# Configuration
BACKEND_URL = "http://localhost:5000"
PROJECT_ROOT = Path(__file__).parent.parent
BACKEND_SCRIPT = PROJECT_ROOT / "run_backend.py"
TEST_SCRIPT = PROJECT_ROOT / "tests" / "test_core_functionality.py"
TEST_REPORT_PATH = PROJECT_ROOT / "tests" / "test_report.txt"


def main():
    """Main runner function"""
    logger.info("Starting test runner")

    backend_process = None
    backend_started = False

    try:
        # Check if backend is already running
        if not is_backend_running():
            logger.info("Backend server is not running. Starting it now...")
            backend_process = start_backend_server()
            backend_started = True

            # Wait for backend to start up
            logger.info("Waiting for backend to initialize...")
            for _ in range(10):  # Try for up to 10 seconds
                if is_backend_running():
                    logger.info("Backend server started successfully")
                    break
                time.sleep(1)
            else:
                logger.error("Backend server failed to start within the timeout period")
                return 1
        else:
            logger.info("Backend server is already running")

        # Run the tests
        logger.info("Running end-to-end tests...")
        run_tests()

        # Display test results
        if TEST_REPORT_PATH.exists():
            display_test_report()

        return 0

    except KeyboardInterrupt:
        logger.info("Test runner interrupted by user")
        return 1
    finally:
        # Clean up the backend process if we started it
        if backend_started and backend_process:
            logger.info("Shutting down backend server...")
            backend_process.terminate()
            try:
                backend_process.wait(timeout=5)
                logger.info("Backend server shutdown complete")
            except subprocess.TimeoutExpired:
                logger.warning(
                    "Backend server did not shut down gracefully, forcing termination"
                )
                backend_process.kill()


def is_backend_running():
    """Check if the backend server is running"""
    try:
        response = requests.get(f"{BACKEND_URL}/status", timeout=2)
        return response.status_code == 200
    except:
        return False


def start_backend_server():
    """Start the backend server as a subprocess"""
    # Start the backend server in a new process
    process = subprocess.Popen(
        [sys.executable, str(BACKEND_SCRIPT)],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        universal_newlines=True,
    )

    return process


def run_tests():
    """Run the end-to-end tests"""
    try:
        # Run the test script
        result = subprocess.run(
            [sys.executable, str(TEST_SCRIPT)],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )

        # Print test script output
        if result.stdout:
            print("Test Script Output:")
            print(result.stdout)

        # Print test script errors if any
        if result.stderr:
            print("Test Script Errors:")
            print(result.stderr)

        return result.returncode == 0
    except Exception as e:
        logger.error(f"Error running tests: {e}")
        return False


def display_test_report():
    """Display the test report contents"""
    print("\n" + "=" * 80)
    print("TEST RESULTS")
    print("=" * 80)

    with open(TEST_REPORT_PATH, "r") as f:
        print(f.read())


if __name__ == "__main__":
    exit(main())
