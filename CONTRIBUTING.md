# Contributing to Privacy-Focused Email Agent

Thank you for considering contributing to this project! This document outlines the guidelines and workflows for contributions.

## Development Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd <project-directory>
   ```

2. Set up a virtual environment:
   ```bash
   python -m venv venv
   # Windows
   venv\Scripts\activate
   # Linux/Mac
   source venv/bin/activate
   ```

3. Install dependencies:
   ```bash
   # Install main requirements
   pip install -r requirements.txt
   # Install development requirements
   pip install -r requirements-dev.txt
   ```

4. Install pre-commit hooks:
   ```bash
   pre-commit install
   ```

5. Set up required environment variables for secrets:
   ```bash
   # Linux/Mac
   export GOOGLE_CLIENT_SECRET_JSON_CONTENT='{"installed":{"client_id":"..."}}'
   # Or point to a file outside the repository
   export GOOGLE_CLIENT_SECRET_JSON_PATH="/path/to/client_secret.json"
   
   # Windows PowerShell
   $env:GOOGLE_CLIENT_SECRET_JSON_CONTENT='{"installed":{"client_id":"..."}}'
   # Or point to a file outside the repository
   $env:GOOGLE_CLIENT_SECRET_JSON_PATH="C:\path\to\client_secret.json"
   ```

## Configuration Management

### INI Configuration File

The application uses an INI file (`config.ini`) for configuration. This file is automatically searched for in:

1. Current directory
2. Project root directory
3. `backend/config/` directory

### Environment Variable Overrides

Any configuration setting can be overridden with environment variables following this naming convention:

```APP_<SECTION>_<KEY>
```

For example, to override the Ollama model name:

```bash
# Linux/Mac
export APP_OLLAMA_MODEL_NAME="llama3:8b-instruct-q8_0"

# Windows PowerShell
$env:APP_OLLAMA_MODEL_NAME="llama3:8b-instruct-q8_0"
```

This approach is useful for:
- Configuring development environments without modifying the shared config file
- Testing different configurations
- Integration testing with specific settings

The application handles type conversion automatically. See the README for more details.

### When to Use Each Approach

- **INI File**: For default or shared configuration that doesn't contain secrets
- **Environment Variables for Config**: For per-environment or per-developer settings
- **Environment Variables for Secrets**: Always for sensitive values like API keys and credentials

## Handling Secrets Securely

This project follows these security best practices for handling secrets:

1. **No secrets in code or config files**: Never commit API keys, credentials, or other secrets to the repository.

2. **Environment variables for secrets**: Use environment variables to provide sensitive information to the application:
   - `GOOGLE_CLIENT_SECRET_JSON_CONTENT`: For storing the entire JSON content of OAuth credentials
   - `GOOGLE_CLIENT_SECRET_JSON_PATH`: For pointing to an external file containing OAuth credentials

3. **Local development configuration**:
   - For local development, use a `.env` file that is NOT committed to the repository 
   - Add `.env` to your personal `.git/info/exclude` file to ensure it's ignored
   - Consider using Python `python-dotenv` or similar tools to load this file in development

4. **Credential rotation**:
   - Regularly rotate API keys and credentials
   - Update your local environment variables when credentials change

5. **Reporting security issues**:
   - If you discover a security vulnerability, please report it privately rather than creating a public issue

## Logging

The application uses a container-friendly structured logging system:

1. **Default Configuration**:
   - Logs are output to `stdout` in JSON format
   - Log level defaults to 'INFO' if not specified
   - No file logging by default (container-friendly)

2. **Environment Variables**:
   - `LOG_LEVEL`: Controls the logging level ('DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL')
   - `LOG_FILE`: Optional path to enable file logging alongside stdout logging

3. **Adding Context to Logs**:
   When logging, add extra context using the `extra` parameter:
   ```python
   logger.info("User authenticated", extra={"user_id": user.id, "auth_method": "oauth"})
   ```

4. **Best Practices**:
   - Always use structured logging with the `extra` parameter for context
   - Don't log sensitive information (tokens, passwords, etc.)
   - Use appropriate log levels (DEBUG for detailed diagnostics, INFO for general information, etc.)
   - For errors that need attention, use ERROR level and include enough context to troubleshoot

## Code Quality Standards

### Python Code Style

This project follows these Python coding standards:
- PEP 8 style guide
- Google docstring format
- Type annotations for all function definitions

### Code Quality Tools

The project uses the following tools for code quality:

1. **Ruff**: Linting and formatting
   - Enabled rules: E, F, W, I, B, COM, C4
   - Line length: 88 characters (same as Black)
   - Configuration in `pyproject.toml`
   - Run manually: `ruff check --fix .` and `ruff format .`

2. **Mypy**: Type checking
   - Strict mode with `disallow_untyped_defs`
   - Configuration in `pyproject.toml`
   - Run manually: `mypy backend`

### Pre-commit Hooks

Pre-commit hooks run automatically before each commit to enforce code quality:
- Basic file checks (trailing whitespace, YAML validity, etc.)
- Ruff linting and formatting
- Mypy type checking

If pre-commit hooks fail, fix the issues and try committing again.

To run pre-commit checks manually:
```bash
pre-commit run --all-files
```

## Pull Request Process

1. Create a new branch for your feature or bugfix:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes, following the code quality standards.

3. Ensure all tests pass and pre-commit hooks run successfully.

4. Push your branch and create a pull request.

5. Update the project documentation as needed.

## Running Tests

### Backend Tests

Run the backend test suite using pytest:
```bash
# On Windows
run_backend_tests.bat

# Alternative using Python directly
python -m pytest backend/tests
```

To run tests with more verbose output:
```bash
# On Windows
run_backend_tests.bat -v

# Alternative using Python directly
python -m pytest backend/tests -v
```

For test coverage reports:
```bash
python -m pytest backend/tests --cov=backend
```

To generate an HTML coverage report:
```bash
python -m pytest backend/tests --cov=backend --cov-report=html
```
This will create a directory named `htmlcov` where you can open `index.html` to view the coverage report.

### Adding New Tests

When adding new functionality, please add corresponding tests:

1. Create test files in the `backend/tests` directory following the naming convention `test_*.py`
2. Test classes should be named `Test*`
3. Test methods should be named `test_*`
4. Use pytest fixtures for setup and teardown when appropriate
5. Follow the example of existing tests for style and structure
