"""
Debug script for Google OAuth authentication
"""

import importlib
import json
import logging
import os
import sys
from typing import TYPE_CHECKING, Any

# Configure verbose logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

# Ensure project root is on sys.path for runtime imports
script_dir = os.path.abspath(os.path.dirname(__file__))
project_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
if project_root not in sys.path:
    sys.path.insert(0, project_root)

# Conditional imports for type checking only (to avoid mypy following heavy modules)
if TYPE_CHECKING:  # pragma: no cover
    GoogleAuthService = Any  # type: ignore
    ConfigurationManager = Any  # type: ignore
    SecureTokenStorage = Any  # type: ignore


def main() -> None:
    try:
        print("\n=== GOOGLE AUTH DEBUG SCRIPT ===\n")

        # Import modules lazily at runtime to avoid mypy analyzing them
        GoogleAuthService = importlib.import_module(
            "backend.services.google_auth_service",
        ).GoogleAuthService
        ConfigurationManager = importlib.import_module(
            "backend.util.config_manager",
        ).ConfigurationManager
        SecureTokenStorage = importlib.import_module(
            "backend.util.secure_token_storage",
        ).SecureTokenStorage

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
        status_str = "Authenticated" if status else "Not authenticated"
        print(f"Current auth status: {status_str}")

        # Step 5: Start auth flow if needed
        if not status:
            print("\nStarting OAuth authentication flow...")
            print("(This will open a browser window)")
            print("Please complete the authentication in your browser")

            try:
                success = auth_service.initiate_auth_flow()
                result_str = "Successfully" if success else "Failed"
                print(f"\nAuthentication flow completed: {result_str}")
            except Exception as e:
                print(f"\n✗ Error during authentication: {e}")

            # Check status again
            status = auth_service.check_auth_status()
            status_str = "Authenticated" if status else "Not authenticated"
            print(f"Auth status after flow: {status_str}")

    except Exception as e:
        print(f"Error: {e}")
        import traceback

        traceback.print_exc()


if __name__ == "__main__":
    main()
