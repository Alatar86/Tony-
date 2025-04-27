"""
Debug script for Google OAuth authentication
"""

import json
import logging
import os
import sys

# Configure verbose logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

# Add the project root directory to Python's path
script_dir = os.path.abspath(os.path.dirname(__file__))
project_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
if project_root not in sys.path:
    sys.path.insert(0, project_root)


def main():
    try:
        print("\n=== GOOGLE AUTH DEBUG SCRIPT ===\n")

        # Import our modules after path setup
        from backend.services.google_auth_service import GoogleAuthService
        from backend.util.config_manager import ConfigurationManager
        from backend.util.secure_token_storage import SecureTokenStorage

        # Step 1: Check client secret file
        config = ConfigurationManager()  # Let it search in standard locations
        client_secret_file = config.get("Google", "client_secret_file")

        print(f"Looking for client secret file: {client_secret_file}")
        if os.path.exists(client_secret_file):
            print("✓ Client secret file exists")

            # Try to parse it
            try:
                with open(client_secret_file, "r") as f:
                    client_data = json.load(f)
                print("✓ Client secret file is valid JSON")

                # Check if it has the expected structure
                if (
                    "installed" in client_data
                    and "client_id" in client_data["installed"]
                ):
                    print("✓ Client secret file has correct structure")
                    print(f"Client ID: {client_data['installed']['client_id']}")
                else:
                    print("✗ Client secret file doesn't have expected structure")
                    print(f"Content: {json.dumps(client_data, indent=2)}")
            except json.JSONDecodeError:
                print("✗ Client secret file is not valid JSON")
        else:
            print("✗ Client secret file not found!")
            return

        # Step 2: Initialize token storage
        print("\nInitializing token storage...")
        token_storage = SecureTokenStorage(config)

        # Check if token exists
        token = token_storage.load_token()
        if token:
            print("✓ Token exists in storage")
        else:
            print("- No token found in storage (this is normal for first login)")

        # Step 3: Initialize auth service
        print("\nInitializing Google Auth service...")
        auth_service = GoogleAuthService(config, token_storage)

        # Step 4: Check current auth status
        print("\nChecking current auth status...")
        status = auth_service.check_auth_status()
        print(
            f"Current auth status: {'Authenticated' if status else 'Not authenticated'}",
        )

        # Step 5: Start auth flow if needed
        if not status:
            print("\nStarting OAuth authentication flow...")
            print("(This will open a browser window)")
            print("Please complete the authentication in your browser")

            try:
                success = auth_service.initiate_auth_flow()
                print(
                    f"\nAuthentication flow completed: {'Successfully' if success else 'Failed'}",
                )
            except Exception as e:
                print(f"\n✗ Error during authentication: {e}")

            # Check status again
            status = auth_service.check_auth_status()
            print(
                f"Auth status after flow: {'Authenticated' if status else 'Not authenticated'}",
            )

    except Exception as e:
        print(f"Error: {e}")
        import traceback

        traceback.print_exc()


if __name__ == "__main__":
    main()
