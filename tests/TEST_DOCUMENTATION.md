# Testing the Privacy-Focused Email Agent

This document provides instructions for running tests to verify the functionality of the Privacy-Focused Email Agent application.

## Test Overview

The testing framework verifies the following core functionality:

1. **Backend Server Connectivity**: Checks if the backend server is running and responding
2. **Authentication**: Tests the Gmail OAuth authentication flow
3. **Email Listing**: Tests fetching emails from Gmail
4. **Email Detail View**: Tests retrieving the full content of specific emails
5. **AI Suggestions**: Tests generating reply suggestions using the local LLM (Ollama)

## Prerequisites

Before running the tests, ensure:

1. Your Python environment has all the required dependencies installed:
   ```
   pip install -r requirements.txt
   ```

2. Ollama is installed and running on your machine with the required model:
   ```
   ollama pull llama3:8b-instruct-q4_K_M
   ```

3. You have created a Google OAuth client ID and downloaded the client secret file to:
   ```
   client_secret_93499475515-psjngo6sm9m5reun6t7ndtv5q6h183pi.apps.googleusercontent.com.json
   ```

## Running the Tests

### Option 1: Using the Test Runner (Recommended)

The test runner script will automatically:
- Check if the backend is running
- Start the backend if needed
- Run all the tests
- Display the results

To use the test runner:

```bash
python tests/run_tests.py
```

### Option 2: Manual Testing

If you prefer to run tests manually:

1. Start the backend server:
   ```bash
   python run_backend.py
   ```

2. In a separate terminal, run the tests:
   ```bash
   python tests/test_core_functionality.py
   ```

## Test Reports

After running the tests, a report file will be generated at:
```
tests/test_report.txt
```

The report includes:
- Pass/fail status for each test
- Detailed error messages if any
- Information about the authentication status, emails retrieved, and suggestions generated

## Troubleshooting

### Backend Server Issues

If the tests report that the backend server isn't running:
1. Check if you can manually run `python run_backend.py`
2. Verify that port 5000 isn't being used by another application
3. Check the backend logs for any errors

### Authentication Issues

If authentication fails:
1. Verify your client secret file is correctly named and located in the project root
2. Ensure you've added the correct redirect URI in your Google Cloud Console (http://localhost:PORT)
3. Check that you've granted the necessary permissions during the OAuth flow

### AI Suggestion Issues

If AI suggestions fail:
1. Verify Ollama is running (`ollama ps`)
2. Check that you've downloaded the correct model
3. Confirm your system meets the hardware requirements for the LLM

## Next Steps

After verifying the core functionality, you may want to:

1. Test additional features as they're implemented (archive, delete, etc.)
2. Run performance tests to evaluate response times
3. Test with various email types (plain text, HTML, attachments)
