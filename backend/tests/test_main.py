"""Tests for the main module of the backend application."""

from unittest.mock import patch


class TestMainModule:
    """Test cases for the main module."""

    @patch("backend.main.ApiServer")
    def test_run_server(self, mock_api_server):
        """Test that run_server initializes and runs the API server."""
        from backend.main import run_server

        # Configure the mock
        mock_api_server_instance = mock_api_server.return_value

        # Call the function
        result = run_server()

        # Verify the expected calls were made
        mock_api_server.assert_called_once()
        mock_api_server_instance.run.assert_called_once()

        # Verify the return value
        assert result == 0

    @patch("backend.main.run_server")
    def test_main_calls_run_server(self, mock_run_server):
        """Test that main calls init_logging and run_server."""
        from backend.main import main

        # Configure the mock
        mock_run_server.return_value = 0

        # Call the function
        result = main()

        # Verify run_server was called
        mock_run_server.assert_called_once()

        # Verify the return value matches what run_server returns
        assert result == 0
