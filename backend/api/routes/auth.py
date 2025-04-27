"""
Authentication related routes
"""
import logging

from flask import Blueprint, current_app, jsonify

# No longer importing services directly, will use current_app
from ...util.exceptions import AuthError  # Keep specific exceptions needed

logger = logging.getLogger(__name__)

auth_bp = Blueprint("auth_bp", __name__, url_prefix="/auth")


@auth_bp.route("/status", methods=["GET"])
def auth_status():
    """Check authentication status"""
    # Access services via current_app context
    auth_service = current_app.config["SERVICES"]["auth_service"]
    authenticated = auth_service.check_auth_status()
    return jsonify({"authenticated": authenticated})


@auth_bp.route("/login", methods=["POST"])
def auth_login():
    """Initiate OAuth login flow"""
    # Access services via current_app context
    auth_service = current_app.config["SERVICES"]["auth_service"]
    try:
        success = auth_service.initiate_auth_flow()
        return jsonify(
            {
                "success": success,
                "message": "Authentication successful"
                if success
                else "Authentication failed",
            }
        )
    except Exception as e:
        # Log the exception within the route
        logger.exception("Error during authentication")
        # Raise specific error for the handler in api_server.py
        raise AuthError(f"Error during authentication: {str(e)}")


# TODO: Move auth routes here
