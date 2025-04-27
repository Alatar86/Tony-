import os
import subprocess
import sys

# Clear any environment variables that might be overriding settings
if 'APP_API_PORT' in os.environ:
    del os.environ['APP_API_PORT']
    print("Cleared APP_API_PORT environment variable")
    
if 'APP_OLLAMA_MODEL_NAME' in os.environ:
    del os.environ['APP_OLLAMA_MODEL_NAME']
    print("Cleared APP_OLLAMA_MODEL_NAME environment variable")

# Now run the backend
print("\nStarting backend server with default settings...")
try:
    subprocess.run([sys.executable, "run_backend.py"], check=True)
except KeyboardInterrupt:
    print("\nBackend server stopped") 