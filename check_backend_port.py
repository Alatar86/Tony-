import requests
import socket
import sys

def check_port_open(host, port):
    """Check if a port is open on a host"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(2)  # 2 second timeout
    result = sock.connect_ex((host, port))
    sock.close()
    return result == 0

def check_api(url):
    """Check if the API endpoint responds"""
    try:
        response = requests.get(url, timeout=5)
        return {
            "status_code": response.status_code,
            "content": response.text[:100] if response.text else "No content"
        }
    except requests.exceptions.RequestException as e:
        return {"error": str(e)}

# Check if port 5000 is open
port_5000_open = check_port_open("localhost", 5000)
port_8080_open = check_port_open("localhost", 8080)

print(f"Port 5000 is {'OPEN' if port_5000_open else 'CLOSED'}")
print(f"Port 8080 is {'OPEN' if port_8080_open else 'CLOSED'}")

# Try to access the API on both ports
if port_5000_open:
    print("\nTrying to access http://localhost:5000/status:")
    result = check_api("http://localhost:5000/status")
    print(result)

if port_8080_open:
    print("\nTrying to access http://localhost:8080/status:")
    result = check_api("http://localhost:8080/status")
    print(result) 