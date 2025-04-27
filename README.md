# Privacy-Focused Email Agent

This is a comprehensive email client application with privacy features and AI-assisted email management.

## System Architecture

The application consists of two main components:

1. **Backend (Python/Flask)**: Handles Gmail API interactions, authentication, and AI services
2. **Frontend (Java/JavaFX)**: Provides the user interface and email client functionality

### Frontend Architecture

The frontend follows a layered architecture with clear separation of concerns:

#### Core Infrastructure Services
- **ApiClient**: Centralized HTTP client for all backend API communication
- **Configuration**: Manages application settings and configuration
- **FrontendPreferences**: Handles user preferences
- **SecureStorageService**: Interface for secure storage with JavaPreferencesStorage implementation

#### Business Logic Services
- **AuthenticationService**: Handles user authentication with Gmail
- **EmailManagementService**: Manages email operations (fetch, read, archive, delete)
- **StatusMonitorService**: Monitors backend and service status

#### UI Managers
- **EmailListViewManager**: Manages the email list display
- **EmailDetailViewManager**: Manages the email detail view
- **FolderNavigationManager**: Handles folder/label navigation
- **StatusUIManager**: Manages status display and updates
- **WindowManager**: Controls window/dialog creation and management

#### Controllers
- **MainWindowController**: Main application window controller
- **ComposeWindowController**: Email composition window controller
- **SettingsWindowController**: Application settings window controller

#### Application Initialization
- **ApplicationInitializer**: Centralizes dependency creation and wiring
- **App/MainApplication**: JavaFX application entry point

### Dependency Injection Pattern

The application uses constructor-based dependency injection throughout:
1. Dependencies are created and wired in the ApplicationInitializer
2. Services are passed to controllers via constructors
3. For JavaFX controllers, UI-related dependencies are injected after FXML loading using reflection (due to JavaFX FXML loading lifecycle constraints)

### Task Execution Pattern

Controllers use the `executeTask` helper method to standardize asynchronous operations:
- Provides consistent error handling
- Centralizes progress indicators and UI updates
- Manages UI control enabling/disabling during operations
- Ensures consistent status message handling

## Code Quality

This project uses several tools to maintain code quality:

### Static Code Analysis

- **Ruff**: Used for Python linting and formatting
- **Mypy**: Used for static type checking

### Pre-commit Hooks

The project uses pre-commit hooks to enforce code quality standards before committing:

1. **Install Development Dependencies**:
   ```bash
   pip install -r requirements-dev.txt
   ```

2. **Install Pre-commit Hooks**:
   ```bash
   pre-commit install
   ```

3. **Running Checks Manually**:
   To run checks on all files in the project:
   ```bash
   pre-commit run --all-files
   ```

### Configuration

- Code style and linting rules are configured in `pyproject.toml`
- Pre-commit hooks are configured in `.pre-commit-config.yaml`

## Requirements

### Backend Requirements
- Python 3.8+
- Flask
- Google API Client
- Ollama (optional, for AI suggestions)

### Frontend Requirements
- Java JDK 17+
- Maven
- JavaFX SDK

## Running the Application

### Step 1: Start the Backend Server

Navigate to the project root directory and run:

```bash
# Windows:
cd backend
python -m backend.main

# Linux/Mac:
cd backend
python3 -m backend.main
```

The backend service should start and listen on http://localhost:5000

### Step 2: Start the Frontend

In a new terminal, navigate to the frontend directory and run:

```bash
cd frontend
mvn javafx:run
```

The frontend application should launch and automatically attempt to connect to the backend service.

## Testing

### Running Backend Tests

The backend uses pytest for testing. To run tests:

```bash
# On Windows:
run_backend_tests.bat

# Using Python directly:
python -m pytest backend/tests
```

For code coverage reports:

```bash
python -m pytest backend/tests --cov=backend --cov-report=html
```

This will generate an HTML coverage report in the `htmlcov` directory.

For more information on testing and adding tests, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Authentication

The application uses OAuth 2.0 for Gmail authentication. On first use:

1. Click the "Login" button in the frontend
2. Follow the browser authentication flow
3. After successful authentication, return to the application

## Environment Variables for Secrets

For security, the application can load Google OAuth client secrets from environment variables instead of files:

1. **GOOGLE_CLIENT_SECRET_JSON_CONTENT**: The entire JSON content of your client_secret.json file
   ```bash
   # Example (Linux/macOS):
   export GOOGLE_CLIENT_SECRET_JSON_CONTENT='{"installed":{"client_id":"your-client-id.apps.googleusercontent.com","project_id":"your-project-id","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_secret":"your-client-secret","redirect_uris":["http://localhost"]}}'
   
   # Example (Windows PowerShell):
   $env:GOOGLE_CLIENT_SECRET_JSON_CONTENT='{"installed":{"client_id":"your-client-id.apps.googleusercontent.com","project_id":"your-project-id","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_secret":"your-client-secret","redirect_uris":["http://localhost"]}}'
   ```

2. **GOOGLE_CLIENT_SECRET_JSON_PATH**: Path to a client_secret.json file outside the repository
   ```bash
   # Example (Linux/macOS):
   export GOOGLE_CLIENT_SECRET_JSON_PATH="/secure/path/to/client_secret.json"
   
   # Example (Windows PowerShell):
   $env:GOOGLE_CLIENT_SECRET_JSON_PATH="C:\secure\path\to\client_secret.json"
   ```

When set, these environment variables take precedence over any client_secret.json file in the application's resources directory. For production deployments, using environment variables is the recommended approach.

## Configuration Environment Variable Overrides

Any configuration setting from the `config.ini` file can be overridden using environment variables. This is particularly useful for:

- Configuring deployments in different environments
- Setting sensitive values without modifying config files
- Temporarily changing settings for testing

### Environment Variable Naming Convention

Environment variables follow this pattern:
```
APP_<SECTION>_<KEY>
```

Where:
- `<SECTION>` is the INI file section name (uppercase)
- `<KEY>` is the configuration key name (uppercase)
- Spaces and special characters in the original section or key names are replaced with underscores

### Examples

| INI File Setting | Environment Variable | Example |
|------------------|----------------------|---------|
| `[Ollama] model_name` | `APP_OLLAMA_MODEL_NAME` | `export APP_OLLAMA_MODEL_NAME=llama3:8b-instruct-q8_0` |
| `[API] port` | `APP_API_PORT` | `export APP_API_PORT=8080` |
| `[App] log_level` | `APP_APP_LOG_LEVEL` | `export APP_APP_LOG_LEVEL=DEBUG` | 
| `[User] signature` | `APP_USER_SIGNATURE` | `export APP_USER_SIGNATURE="Best regards,\nJane Doe"` |

### Type Conversion

The application automatically attempts to convert environment variable values (which are always strings) to the appropriate type based on the original value in the INI file:

- If the original value is a boolean (`true`, `false`, `yes`, `no`, etc.), the environment variable is interpreted as a boolean
- If the original value is an integer, the environment variable is interpreted as an integer
- If the original value is a float, the environment variable is interpreted as a float

If type conversion fails, the value is used as a string and a warning is logged.

## Known Issues

- **MainWindowControllerTest**: Some test cases in MainWindowControllerTest are currently disabled/failing due to recent refactoring of the Task execution logic.

### Testing `MainWindowController`

Testing asynchronous operations within JavaFX controllers like `MainWindowController` presents challenges. To facilitate reliable unit testing of methods involving background tasks (managed via the `executeTask` helper method), a `testMode` flag has been implemented within the controller.

When `testMode` is set to `true` (typically done within the test setup, e.g., via `controller.setTestMode(true)`):

* The `executeTask` helper method executes the provided `javafx.concurrent.Task` object *synchronously* by calling its `run()` method directly, bypassing the background `ExecutorService`.
* This ensures that the task's logic and its success/failure callbacks are triggered predictably within the test thread, simplifying mock setup and verification.
* Error handling within `executeTask` ensures that `onFailed` handlers are still invoked correctly even during synchronous execution in test mode.

While this approach aids unit testing of the controller's logic, comprehensive testing of UI interactions and behaviour under true asynchronous conditions may still benefit from UI testing frameworks like TestFX in the future. The `MainWindowControllerTest` suite utilizes this `testMode` for its current verification.

- **Secure Storage**: The default JavaPreferencesStorage implementation is not suitable for storing highly sensitive information in production environments. For production use, consider implementing a more secure platform-specific storage service.
- **UI Responsiveness**: Under heavy load or with large email volumes, the UI may appear less responsive. This is being addressed in future updates.

## Secure Storage

The application supports two storage implementations for sensitive credentials:

1. **JavaPreferencesStorage**: The default implementation that uses Java Preferences API. It provides basic security but is not suitable for storing highly sensitive information in production environments.

2. **PlatformSecureStorage**: A more secure implementation that leverages native OS credential stores through the Microsoft credential-secure-storage-for-java library:
   - Windows: Uses Windows Credential Manager
   - macOS: Uses Keychain
   - Linux: Uses Secret Service API (GNOME Keyring) or KWallet (KDE)

To use the PlatformSecureStorage in your application:

```java
// In your AppContext or service initialization code
SecureStorageService secureStorage;
try {
    secureStorage = new PlatformSecureStorage();
} catch (SecurityException e) {
    // Fall back to JavaPreferencesStorage if platform secure storage is unavailable
    logger.warn("Platform secure storage unavailable, falling back to Java Preferences: {}", e.getMessage());
    secureStorage = new JavaPreferencesStorage(CredentialsService.class);
}

// Then create your CredentialsService with the secure storage
CredentialsService credentialsService = new CredentialsService(secureStorage);
```

Both implementations follow the same interface (SecureStorageService), making them interchangeable within the application.

## Troubleshooting

### Connection Issues
- Ensure the backend server is running before starting the frontend
- Check the backend server logs for error messages
- Verify that port 5000 is not in use by another application

### Authentication Issues
- Check the configuration files in the backend directory
- Ensure Google API credentials are properly configured
- Reset the application and try the authentication flow again
