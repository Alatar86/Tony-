# V5 Combined Implementation Plan

## Backend (Python Service)

### Phase 0: Environment & Configuration Setup

#### (Stage 0.1: Project Initialization)
Goal: Set up basic project files and dependencies.
- [x] Create project directory.
- [x] Initialize Git (git init).
- [x] Create requirements.txt: Add google-api-python-client, google-auth-oauthlib, google-auth-httplib2, requests (or httpx), Flask (or FastAPI), python-dotenv (optional, for loading env vars if needed), keyring (optional, for secure storage).
- [x] Create .gitignore: Include common Python patterns, IDE files, __pycache__, virtual env folders, client_secret.json, token_storage*, *.log, config.ini (if sensitive, otherwise track changes).
- [x] **User Action:** Create virtual environment, install requirements (pip install -r requirements.txt).

#### (Stage 0.2: Configuration File)
Goal: Create the config.ini file.
- [x] Create config.ini.
- [x] Define sections [Google], [Ollama], [App], [API] with specified keys (see Outline Section 6) and placeholder/default values.
- [x] Create config_manager.py with a function/class to load and provide access to config values.

#### (Stage 0.3: Google Cloud Setup Guidance)
Goal: Obtain OAuth 2.0 Desktop App credentials.
- [x] (Manual User Process, Guided by AI Instructions) - Same as your original plan.
- [x] **User Action:** Perform steps on GCP, save client_secret.json to the configured path.

#### (Stage 0.4: Ollama Setup Guidance)
Goal: Install Ollama and the target LLM locally.
- [x] (Manual User Process, Guided by AI Instructions) - Same as your original plan.
- [x] **User Action:** Install/Configure Ollama and download the model.

### Phase 1: Authentication Module

#### (Stage 1.1: Google Auth Service Implementation)
Goal: Implement the OAuth 2.0 logic and secure storage service.
- [x] Create secure_token_storage.py: Implement class SecureTokenStorage with save_token(token_data) and load_token() methods using keyring library (or fallback file storage if keyring fails/unavailable).
- [x] Use token_cache_path from config as service name/identifier for keyring.
- [x] Create google_auth_service.py: Implement class GoogleAuthService.
- [x] Constructor takes config and token_storage.
- [x] Method get_credentials(): Reads config (client_secret_file, scopes), uses token_storage to load cached token, uses InstalledAppFlow to get/refresh credentials.
- [x] Saves updated credentials via token_storage. Handles errors. Returns credentials object or None.
- [x] Method initiate_auth_flow(): Forces flow.run_local_server() part of get_credentials.
- [x] Method check_auth_status(): Loads token, checks validity/expiration, attempts refresh if needed. Returns True if valid/refreshed, False otherwise.

#### (Stage 1.2: Auth API Endpoints)
Goal: Expose authentication status and initiation via API.
- [x] Create api_server.py. Initialize Flask/FastAPI app.
- [x] Load config. Instantiate SecureTokenStorage and GoogleAuthService.
- [x] Implement GET /auth/status endpoint: Calls auth_service.check_auth_status(), returns JSON {"authenticated": boolean}.
- [x] Implement POST /auth/login endpoint: Calls auth_service.initiate_auth_flow(), catches exceptions, returns JSON {"success": boolean, "message": string}.
- [x] **User Action:** Run python api_server.py (Corrected to `python -m backend.main` from project root).
- [x] **User Action:** Use curl or Postman to test endpoints. Test /auth/login flow. Check token storage.

### Phase 2: Email Reading Service & API

#### (Stage 2.1: Gmail Service Implementation)
Goal: Implement logic to interact with Gmail API.
- [x] Create gmail_api_service.py. Implement class GmailApiService.
- [x] Constructor takes credentials. Method _get_service() builds the Gmail API resource.
- [x] Method list_messages(label_id='INBOX', max_results=20): Calls service.users().messages().list(). Returns list of message IDs or empty list/None on error. *(Updated to use label_id)*
- [x] Method get_message_metadata(message_id): Calls service.users().messages().get(format='metadata').
- [x] Parses 'Subject', 'From', 'Date' headers. Returns dict or None.
- [x] Method get_message_details(message_id): Calls service.users().messages().get(format='full').
- [x] Parses headers and text/plain body (handles base64 decoding, different MIME types/parts). Returns dict with full details or None.
- [x] Implement robust body parsing helper function. Include comprehensive error handling (e.g., for invalid IDs, API errors).

#### (Stage 2.2: Email API Endpoints)
Goal: Expose email fetching via API.
- [x] Modify api_server.py.
- [x] Add helper function _get_authenticated_gmail_service() that uses GoogleAuthService to get credentials and then creates/returns GmailApiService instance, handling auth errors (returning e.g., 401).
- [x] Implement GET /emails endpoint: Use helper to get gmail_service. If successful, call list_messages, loop IDs calling get_message_metadata. *(Updated to accept labelId query parameter)*
- [x] Return JSON list or appropriate error response. Read maxResults from query param/config.
- [x] Implement GET /emails/{message_id} endpoint: Use helper to get gmail_service. Call get_message_details.
- [x] Return JSON object or error response (e.g., 404 if details not found).
- [x] **User Action:** Run api_server.py.
- [x] **User Action:** Test /emails and /emails/{id} endpoints (assuming authenticated). *(Also tested /emails?labelId=SENT etc.)*

### Phase 3: Local AI Integration Service & API

#### (Stage 3.1: Ollama Client Service Implementation)
Goal: Implement function to call the local Ollama API.
- [x] Create llm_service.py. Implement class LocalLlmService.
- [x] Constructor takes config.
- [x] Method get_suggestions(email_body): Reads config (model_name, api_base_url, request_timeout_sec, suggestion_prompt_template). Formats prompt with email_body. Builds JSON payload.
- [x] Uses requests.post to call Ollama /api/generate. Parses JSON response, extracts suggestions (may need post-processing if response is one block).
- [x] Returns list of strings. Include error handling for network and Ollama API errors.

#### (Stage 3.2: Suggestion API Endpoint)
Goal: Expose suggestion generation via API.
- [x] Modify api_server.py. Instantiate LocalLlmService.
- [x] Implement GET /emails/{message_id}/suggestions endpoint:
- [x] Use helper _get_authenticated_gmail_service() to get gmail_service.
- [x] Call gmail_service.get_message_details(message_id) to get email body.
- [x] Handle case where body is not found.
- [x] Call llm_service.get_suggestions(body).
- [x] Return JSON {"suggestions": [...]} or error response (e.g., if body fetch failed or Ollama failed).
- [x] **User Action:** Run api_server.py. Test /emails/{id}/suggestions endpoint for an existing message ID.

### Phase 4: Refinements

#### (Stage 4.1: Configuration Loading)
Goal: Ensure consistent use of config_manager throughout the backend.
- [x] Review all modules, replace any hardcoded values with calls to fetch them from the central config_manager.
- [x] Ensure config is loaded once on startup.

#### (Stage 4.2: Error Handling)
Goal: Implement robust, consistent error handling and API responses.
- [x] Review all try...except blocks. Use specific exceptions where possible.
- [x] Implement Flask/FastAPI error handlers to catch custom/common exceptions and return standardized JSON error responses with appropriate HTTP status codes.
- [x] Ensure sensitive details aren't leaked in error messages.

#### (Stage 4.3: Basic Logging)
Goal: Implement comprehensive file-based logging.
- [x] Configure logging in api_server.py based on config.ini (level, file path using RotatingFileHandler, format).
- [x] Add logger = logging.getLogger(__name__) to each module.
- [x] Add logger.info/debug calls for key events (API request received, service call initiated, etc.).
- [x] Log all caught exceptions using logger.error or logger.exception. Ensure no PII is logged.

### Phase 5: Packaging

#### (Stage 5.1: PyInstaller Setup & Build)
Goal: Create a distributable executable for the backend service.
- [ ] Install PyInstaller (pip install pyinstaller).
- [ ] Generate initial spec file (pyi-makespec api_server.py --name=EmailAgentBackend).
- [ ] Edit backend.spec: Add necessary data files (e.g., potentially config.ini if not user-provided externally).
- [ ] Add hidden imports if needed (common for Flask/FastAPI, requests, google libs). Configure console=False for a background process executable.
- [ ] Build the executable (pyinstaller backend.spec). Test the bundled application.
- [ ] **User Action:** Build and test the executable.

---

## Frontend (JavaFX Client)

### Phase 0: Environment & Project Setup

#### (Stage 0.1: Environment Setup)
Goal: Install necessary tools for JavaFX development.
- [x] Install JDK 21 LTS, Apache Maven. Configure IDE (IntelliJ/Eclipse/VSCode) for Java/Maven.
- [x] **User Action:** Install JDK/Maven, set up IDE.

#### (Stage 0.2: Project Initialization)
Goal: Create Maven project structure.
- [x] Use Maven archetype or IDE wizard to create a new JavaFX project.
- [x] Configure pom.xml: Add dependencies for JavaFX (controls, fxml, graphics), HTTP Client (e.g., java.net.http), JSON library (e.g., Jackson Databind), Logging (SLF4j API, Logback), Testing (JUnit5, Mockito).
- [x] Set Java version to 21. Configure Maven plugins for JavaFX if needed (e.g., javafx-maven-plugin).
- [x] Create basic package structure (com.your_app_name, controllers, services, model, util).
- [x] Create main App.java extending javafx.application.Application. Load a placeholder main FXML.
- [x] Create .gitignore. Initialize Git (git init).
- [x] **User Action:** Create Maven project, verify basic empty JavaFX window runs. Commit initial structure.

### Phase 1: Basic UI Shell

#### (Stage 1.1: Main Window Layout)
Goal: Implement the static layout of the main application window using FXML.
- [x] Create MainWindow.fxml. Define the BorderPane structure.
- [x] Add placeholders (e.g., ToolBar, VBox for sidebar, SplitPane for center, HBox for status bar) with fx:id attributes.
- [x] Add basic controls (buttons, lists, text areas) without functionality, assign fx:id.
- [x] Create MainWindowController.java implementing Initializable. Inject FXML elements using @FXML.
- [x] Modify App.java to load MainWindow.fxml as the main scene.
- [x] **User Action:** Run app, verify static layout matches visual design (image_7dc418.png).
- [x] **User Action:** Commit changes (ui: Add main window layout).

### Phase 2: API Client & Status Check

#### (Stage 2.1: API Client Service)
Goal: Implement the service for making HTTP calls to the backend API.
- [x] Create POJO classes (AuthStatusResponse, StatusResponse) matching backend JSON structure. Add Jackson/Gson annotations if needed.
- [ ] Create JsonUtil helper class (or use library directly) for parsing JSON. *(Using ObjectMapper directly)*
- [x] Create ApiClient.java.
- [x] Initialize java.net.http.HttpClient. Store backend base URL (http://localhost:5000).
- [x] Implement checkAuthStatus(): Build HttpRequest for GET /auth/status, send request using httpClient.send(), handle response (check status code), parse JSON body into AuthStatusResponse using JsonUtil.
- [x] Return AuthStatusResponse object. Include try-catch for IOException, InterruptedException.
- [x] Implement getBackendStatus(): Similar process for GET /status, returning StatusResponse.

#### (Stage 2.2: Initial Status Check)
Goal: Call backend on startup to check status and update UI.
- [x] In MainWindowController.initialize(): Instantiate ApiClient.
- [x] Create a javafx.concurrent.Task to call apiClient.checkAuthStatus() and apiClient.getBackendStatus() in the background.
- [x] Define setOnSucceeded handler for the task: Use Platform.runLater to update status bar labels (gmailStatusLabel, aiStatusLabel) and show/hide a "Login" button (loginButton) based on the results.
- [x] Define setOnFailed handler to show an error message (Alert) if backend communication fails.
- [x] Run the task using an ExecutorService or new Thread(task).start().
- [x] **User Action:** Run app.
- [x] **User Action:** Ensure backend is NOT running - verify error message. Run backend.
- [x] **User Action:** Run app - verify status bar updates and Login button appears/hides correctly. Commit changes (feat: Implement initial status check).

### Phase 3: Authentication Trigger

#### (Stage 3.1: Login Action)
Goal: Implement the action when the user clicks the "Login" button.
- [x] Implement ApiClient.initiateLogin(): Build HttpRequest for POST /auth/login, send request, handle response (check success/failure in JSON body).
- [x] Return boolean success status.
- [x] In MainWindowController, create @FXML void handleLoginButtonAction().
- [x] Inside handleLoginButtonAction: Disable login button. Show status message ("Check browser...").
- [x] Create/run a background Task to call apiClient.initiateLogin().
- [x] In task's setOnSucceeded: Re-enable button (maybe). Update status message based on success/failure.
- [x] Suggest user manually refresh status or trigger /auth/status check again after a delay.
- [x] Connect loginButton's onAction to this handler in FXML or controller.
- [x] **User Action:** Run backend & frontend. Click Login.
- [x] **User Action:** Verify status updates and backend triggers browser flow. Commit changes (feat: Implement login trigger).

### Phase 4: Email Display

#### (Stage 4.1: Data Models & API Calls)
Goal: Implement API calls and data models for fetching emails.
- [x] Create POJOs EmailMetadata (id, subject, from, date) and EmailDetails (id, subject, from, to, date, body) matching backend JSON.
- [x] Implement ApiClient.getEmailList(): GET /emails, parse JSON array into List<EmailMetadata>. *(Modified to potentially accept labelId in future steps)*
- [x] Implement ApiClient.getEmailDetails(String messageId): GET /emails/{message_id}, parse JSON into EmailDetails.

#### (Stage 4.2: Refresh & List Display)
Goal: Populate the email list view on refresh.
- [x] Configure emailListView (ListView<EmailMetadata>) in controller/FXML. (Optional: Create custom ListCell for better rendering).
- [x] Create refreshEmails() method in controller.
- [x] Inside refreshEmails: Show loading indicator. Create/run Task to call apiClient.getEmailList(). *(Currently defaults to INBOX)*
- [x] Task setOnSucceeded: Hide indicator. Clear list view.
- [x] Populate emailListView's ObservableList with the received List<EmailMetadata>. Handle empty list state (show message).
- [x] Task setOnFailed: Hide indicator. Show error Alert.
- [x] Connect refreshButton action to call refreshEmails(). Call refreshEmails() initially after successful status check if authenticated.
- [x] **User Action:** Run apps.
- [x] **User Action:** Test Refresh button. Verify email list populates. Test empty state. Commit (feat: Implement email list display).

#### (Stage 4.3: Detail Display)
Goal: Display email body when an item is selected.
- [x] Add listener to emailListView.getSelectionModel().selectedItemProperty().
- [x] Inside listener: Get selected EmailMetadata. If null, clear detail view. If not null, get messageId.
- [x] Show loading indicator for detail view. Create/run Task to call apiClient.getEmailDetails(messageId).
- [x] Task setOnSucceeded: Hide indicator.
- [x] Update detail view labels (Subject, From, Date) and emailBodyView (TextArea or WebView) with data from EmailDetails. Enable "Suggest Replies" button.
- [x] Task setOnFailed: Hide indicator. Show error Alert. Clear detail view.
- [x] **User Action:** Test selecting emails.
- [x] **User Action:** Verify details and body appear correctly. Commit (feat: Implement email detail display).

### Phase 5: AI Suggestion Integration

#### (Stage 5.1: API Call & Model)
Goal: Implement API call for suggestions.
- [x] Create SuggestionResponse POJO (e.g., containing List<String> suggestions).
- [x] Implement ApiClient.getSuggestions(String messageId): GET /emails/{message_id}/suggestions, parse JSON response into SuggestionResponse.

#### (Stage 5.2: UI Integration)
Goal: Trigger suggestion fetch and display results.
- [x] Add UI elements (e.g., VBox or ListView) to display suggestions (suggestionsView).
- [x] Create handleSuggestButtonAction() method.
- [x] Inside handler: Get current messageId. Disable suggest button, show loading status. Create/run Task to call apiClient.getSuggestions(messageId).
- [x] Task setOnSucceeded: Re-enable button.
- [x] Clear previous suggestions. Display new suggestions from SuggestionResponse in suggestionsView. Update status.
- [x] Task setOnFailed: Re-enable button. Show error Alert.
- [x] Connect suggestButton action to this handler. Ensure button is disabled initially/when no email selected.
- [x] **User Action:** Test Suggest Replies button.
- [x] **User Action:** Verify suggestions appear and UI remains responsive. Commit (feat: Implement AI suggestion display).

### Phase 6: Preparatory Refactoring & Documentation

#### (Stage 6.1: Refactoring for Clean Architecture)
Goal: Refactor the codebase to prepare for future features, ensuring a clean architecture.
- [x] Implement dependency injection for services in the MainWindowController.
- [x] Extract interfaces for key services (ApiClient, CredentialsService) to allow for easier testing and future modifications.
- [x] Reorganize packages if needed for better separation of concerns.

#### (Stage 6.2: Security & Testing Documentation)
Goal: Document security plans and testing strategy.
- [x] Create FileService sandbox security plan in docs/FileService_Sandbox_Plan.md.
- [x] Document core security principles, path validation strategy, handling of absolute paths and symbolic links.
- [x] Define method signatures with security considerations.
- [x] Create testing plan in docs/Testing_Plan.md focusing on FileService security, CredentialsService, DI refactoring.
- [x] Document comprehensive test cases for security features including directory traversal prevention, absolute path handling, and symbolic link tests.
- [x] Outline future UI testing needs using TestFX framework.
- [x] Define test directory structure and implementation priorities.

### Phase 7: FileService Sandbox Implementation

### Phase 8: Folder/Label Navigation (Completed)

Goal: Allow user to select different email folders/labels.
- **Backend:**
    - [x] Modify `GmailApiService.list_messages` to accept `label_id`.
    - [x] Modify `GET /emails` endpoint in `api_server.py` to accept `labelId` query parameter.
    - [x] **User Action:** Verify backend endpoint with different `labelId` values. *(User confirmed working)*
- **Frontend:**
    - [x] Modify `ApiClient.getEmailList` to accept `labelId`.
    - [x] Add UI elements for folder selection in `MainWindow.fxml` *(Existing elements utilized)*.
    - [x] Implement click handlers in `MainWindowController` for folder UI elements.
    - [x] Update `refreshEmails` to use the selected `labelId` when calling `ApiClient`.
    - [x] Update UI to indicate the currently selected folder.
    - [ ] **UI Bugfix:** Address issue where ListView visual doesn't always update on folder change *(Attempted fix by recreating cell nodes, verification pending)*.

### Phase 9: Basic Actions - Delete/Archive (Added Feature)

Goal: Implement Delete and Archive functionality.
- Backend:
    - [x] Implement `archive_message` in `GmailApiService`.
    - [x] Implement `delete_message` in `GmailApiService`.
    - [x] Implement `POST /emails/{message_id}/archive` endpoint in `api_server.py`.
    - [x] Implement `DELETE /emails/{message_id}/delete` endpoint in `api_server.py`.
- Frontend:
    - [x] Implement `archiveEmail` in `ApiClient`.
    - [x] Implement `deleteEmail` in `ApiClient`.
    - [x] Add Archive/Delete buttons to `MainWindow.fxml`.
    - [x] Implement `handleArchiveAction` in `MainWindowController`.
    - [x] Implement `handleDeleteAction` in `MainWindowController`.
    - [x] Add logic to refresh email list after action.
    - [x] **User Action:** Test Archive and Delete buttons.

### Phase 10: UI Refinements (Added Feature)

Goal: Improve UI layout and responsiveness.
- [x] Fix vertical pane sizing in main window right content area.

### Phase 11: Backend Performance Optimization (Added Feature / Refinement)

Goal: Improve performance of fetching email list metadata.
- [x] Add `get_multiple_messages_metadata` method to `GmailApiService` using `BatchHttpRequest`.
- [x] Update `GET /emails` endpoint in `api_server.py` to use batch fetching.
- [x] **User Action:** Verify performance improvement and lack of errors. *(User confirmed improved speed, addressed 404 error)*
