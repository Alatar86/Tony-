"""Test script to verify python-json-logger import."""

try:
    from pythonjsonlogger.jsonlogger import JsonFormatter
    print('Import successful from jsonlogger')
except ImportError as e:
    print(f'Import error from jsonlogger: {e}')

try:
    from pythonjsonlogger.json import JsonFormatter
    print('Import successful from json')
except ImportError as e:
    print(f'Import error from json: {e}')

print("Test completed") 