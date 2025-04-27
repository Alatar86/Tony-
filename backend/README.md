# Backend for Privacy-Focused Email Agent

This directory contains the Python backend service that handles:
- OAuth 2.0 authentication with Gmail
- Gmail API interactions
- Local LLM integration via Ollama
- REST API endpoints

## Structure
- `/api`: REST API layer using Flask/FastAPI
- `/services`: Service components (Authentication, Gmail, LLM)
- `/util`: Utility modules (Configuration, Logging, SecureStorage)
- `/models`: Data models
- `/config`: Configuration files
