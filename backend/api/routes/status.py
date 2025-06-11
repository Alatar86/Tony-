"""
Status endpoint
"""

import logging

from flask import Blueprint, current_app, jsonify, Response

# No longer importing services directly, will use current_app
from ...util.exceptions import ServiceError  # Keep specific exceptions needed

logger = logging.getLogger(__name__)

status_bp = Blueprint("status_bp", __name__, url_prefix="/status")


@status_bp.route("", methods=["GET"])
def status() -> Response:
    """Get overall backend status"""
    auth_service = current_app.config["SERVICES"]["auth_service"]
    llm_service = current_app.config["SERVICES"]["llm_service"]
    try:
        gmail_authenticated = auth_service.check_auth_status()
        ai_service_status = "active" if llm_service.check_status() else "inactive"

        return jsonify(
            {
                "gmail_authenticated": gmail_authenticated,
                "local_ai_service_status": ai_service_status,
            },
        )
    except Exception as e:
        logger.exception("Error checking status")
        raise ServiceError(f"Error checking service status: {str(e)}") from e


# TODO: Move status route here
