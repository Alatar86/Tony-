#!/usr/bin/env python
"""
Script to run backend tests using pytest.
"""
import os
import subprocess
import sys


def run_tests():
    """Run pytest for the backend application."""
    # Get the directory of this script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Change to the script directory
    os.chdir(script_dir)
    
    # Run pytest with all arguments passed to this script
    cmd = ["pytest", "backend/tests"] + sys.argv[1:]
    return subprocess.call(cmd)


if __name__ == "__main__":
    sys.exit(run_tests()) 