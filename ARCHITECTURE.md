# Privacy-Focused Email Agent - Architectural Overview

## System Architecture

The Privacy-Focused Email Agent is built on a client-server architecture with clear separation of concerns:

### Backend (Python/Flask)
- REST API for Gmail interaction and AI-powered suggestions
- Management of authentication, configuration, and service integrations
- Integration with Ollama for local LLM processing

### Frontend (Java/JavaFX)
- Desktop UI application for email viewing and management
- Service layer for communicating with the backend API
- Plugin system for extensibility

## Component Overview

### Backend Components

#### API Server (`backend/api/api_server.py`)
- Implements REST endpoints for frontend communication
- Handles authentication, email operations, and configuration
- Routes requests to appropriate services

#### Services
- **GoogleAuthService**: Manages OAuth authentication with Gmail
- **GmailApiService**: Interacts with Gmail API for email operations
- **LocalLlmService**: Provides AI-powered email suggestions using Ollama

#### Utilities
- **ConfigurationManager**: Manages application configuration
- **SecureTokenStorage**: Securely stores OAuth tokens
- **LoggingService**: Provides consistent logging

### Frontend Components

#### User Interface
- **MainWindowController**: Primary application window controller
- **ComposeController**: Email composition functionality
- **SettingsController**: Application configuration UI

#### Services Layer
- **ApiClient**: Communicates with the backend REST API
- **EmailContentRenderer**: Processes and formats email content
- **WebViewHelper**: Configures WebView components for email display

#### Models
- **EmailDetails**: Represents a full email with content
- **EmailMetadata**: Represents email list metadata
- **ApiResult**: Wrapper for API responses with error handling

#### Plugin System
- **PluginManager**: Loads and manages email plugins
- **EmailPlugin**: Interface for all email plugins
- **Plugin implementations**: LinkSanitizerPlugin, ImageBlockerPlugin, etc.

## Data Flow

1. **Authentication Flow**
   - User initiates login via the UI
   - Frontend calls backend auth endpoint
   - Backend initiates OAuth flow with Google
   - Tokens stored securely for future API calls

2. **Email Retrieval Flow**
   - Frontend requests emails from backend API
   - Backend authenticates with Gmail API
   - Backend processes and returns email metadata
   - Frontend displays email list

3. **Email Content Flow**
   - User selects an email
   - Frontend requests full email details
   - Backend retrieves and processes email content
   - Frontend renders email with appropriate styling
   - Plugins modify content for privacy/security

4. **AI Suggestion Flow**
   - User requests reply suggestions
   - Frontend calls suggestions endpoint
   - Backend extracts context from email/thread
   - Backend sends prompt to Ollama
   - Generated suggestions returned to frontend
   - Frontend displays suggestion options

## Design Patterns

- **MVC Pattern**: Separation of UI (views), logic (controllers), and data (models)
- **Singleton Pattern**: Used for configuration, preferences, and service instances
- **Factory Pattern**: Used for plugin creation and management
- **Strategy Pattern**: Used in email content processing pipelines
- **Observer Pattern**: Used for theme changes and content updates
- **Adapter Pattern**: Used to normalize data between backend and frontend

## Error Handling Strategy

- Custom exception types for specific error scenarios
- Global exception handlers in both backend and frontend
- Consistent error response format from API endpoints
- User-friendly error messages in the UI

## Security Considerations

- OAuth 2.0 for secure Gmail authentication
- Secure storage of tokens and credentials
- User control over image loading and external content
- Local processing of AI functions for privacy

## Extension Points

The application is designed for extensibility in several areas:

1. **Plugin System**: New email processing plugins can be added
2. **LLM Providers**: Additional AI backends can be integrated
3. **Email Providers**: Support for providers beyond Gmail can be added
4. **UI Themes**: Custom themes can be developed
5. **Content Processors**: New content processing strategies can be implemented

## Future Architecture Considerations

- Containerization for easier deployment
- Configuration management for different environments
- CI/CD pipeline for automated testing and deployment
- Metrics collection for performance monitoring
- Enhanced security for sensitive data
