# Refactoring Plan

## Backend Code Organization

The primary issue identified in the backend codebase is the large `api_server.py` file (632 lines), which contains too much business logic directly in route handlers. This document outlines a plan to refactor this file into a more maintainable structure.

### Current Structure Issues

- Route handlers contain business logic that should be in service classes
- Single file is too large for maintainability
- Error handling is duplicated across route handlers
- Difficult to test individual components

### Proposed Directory Structure

```
backend/
├── api/
│   ├── __init__.py
│   ├── server.py            # Core API server setup (previously api_server.py)
│   ├── routes/
│   │   ├── __init__.py      # Route registration
│   │   ├── auth_routes.py   # Authentication endpoints
│   │   ├── config_routes.py # Configuration endpoints
│   │   ├── email_routes.py  # Email CRUD endpoints
│   │   └── status_routes.py # Status endpoints
│   └── middleware/
│       ├── __init__.py
│       ├── auth.py          # Authentication middleware
│       ├── error_handler.py # Error handling middleware
│       └── logging.py       # Request logging middleware
├── services/
│   ├── __init__.py
│   ├── google_auth_service.py
│   ├── gmail_api_service.py
│   ├── local_llm_service.py
│   ├── email_service.py     # NEW: Business logic for email operations
│   ├── suggestion_service.py # NEW: Business logic for suggestions
│   └── config_service.py    # NEW: Business logic for configuration
└── util/
    ├── __init__.py
    ├── config_manager.py
    ├── secure_token_storage.py
    ├── logging_service.py
    └── exceptions.py
```

### Refactoring Steps

1. **Create Route Modules**
   - Create separate route modules for each logical group of endpoints
   - Move route handlers from api_server.py to appropriate modules
   - Register routes in `routes/__init__.py`

2. **Extract Business Logic to Service Classes**
   - Create new service classes for email operations, suggestions, etc.
   - Move business logic from route handlers to these service classes
   - Route handlers should only handle request/response formatting

3. **Centralize Error Handling**
   - Create a global error handler in middleware
   - Remove duplicate error handling code from route handlers

4. **Implement Dependency Injection**
   - Services should be initialized once and passed to route handlers
   - Makes testing easier with mock services

### Example Refactoring: Email Routes

Before:
```python
@self.app.route('/emails/<message_id>', methods=['GET'])
def get_email(message_id):
    """Get details for a specific email"""
    # Check authentication
    if not self.auth_service.check_auth_status():
        raise AuthError("Not authenticated")

    try:
        # Get Gmail service
        gmail_service = self._get_authenticated_gmail_service()
        if not gmail_service:
            raise GmailApiError("Failed to initialize Gmail service")

        # Get message details
        details = gmail_service.get_message_details(message_id)
        if not details:
            raise NotFoundError(f"Message with ID {message_id} not found")

        return jsonify(details)

    except (AuthError, GmailApiError, NotFoundError) as e:
        # Let these be caught by the global handler
        raise
    except Exception as e:
        logger.exception(f"Error getting email {message_id}")
        raise GmailApiError(f"Error retrieving email: {str(e)}")
```

After:

```python
# In email_service.py
class EmailService:
    def __init__(self, auth_service, gmail_service):
        self.auth_service = auth_service
        self.gmail_service = gmail_service

    def get_email_details(self, message_id):
        """Get details for a specific email"""
        if not self.auth_service.check_auth_status():
            raise AuthError("Not authenticated")

        # Get message details
        details = self.gmail_service.get_message_details(message_id)
        if not details:
            raise NotFoundError(f"Message with ID {message_id} not found")

        return details
```

```python
# In email_routes.py
@bp.route('/<message_id>', methods=['GET'])
def get_email(message_id):
    """Get details for a specific email"""
    try:
        details = email_service.get_email_details(message_id)
        return jsonify(details)
    except Exception as e:
        # Let middleware handle the error
        raise
```

### Testing Strategy

- Each service class should have its own unit tests
- Route handlers can be tested with mock services
- Integration tests should verify end-to-end functionality

### Migration Plan

1. Create the new directory structure without modifying existing code
2. Implement the new service classes to mirror existing functionality
3. Create new route handlers that use the service classes
4. Migrate one endpoint at a time, with tests for each
5. Replace old routes with new routes after testing
6. Remove the original api_server.py file

This incremental approach allows for safe refactoring with minimal downtime or risk.
