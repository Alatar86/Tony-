package com.privacyemail.models;

import java.util.Collections;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an error returned from the API.
 * Contains an error code, message, and optional details.
 */
public class ApiError {

    // Error categories for consistent error handling
    public static final String CATEGORY_NETWORK = "NETWORK";
    public static final String CATEGORY_AUTH = "AUTH";
    public static final String CATEGORY_SERVER = "SERVER";
    public static final String CATEGORY_CLIENT = "CLIENT";
    public static final String CATEGORY_TIMEOUT = "TIMEOUT";
    public static final String CATEGORY_VALIDATION = "VALIDATION";
    public static final String CATEGORY_LLM = "LLM";
    public static final String CATEGORY_UNKNOWN = "UNKNOWN";

    private final int code;
    private final String message;
    private final Map<String, Object> details;
    private String category;

    /**
     * Create a new ApiError.
     *
     * @param message The error message
     * @param code The HTTP status code
     * @param details Additional error details
     */
    @JsonCreator
    public ApiError(@JsonProperty("message") String message, @JsonProperty("code") int code, @JsonProperty("details") Map<String, Object> details) {
        this.message = message;
        this.code = code;
        this.details = details;
        this.category = determineCategory(code, message);
    }

    /**
     * Create a network-related error.
     *
     * @param message The error message
     * @return A new ApiError with NETWORK category
     */
    public static ApiError networkError(String message) {
        ApiError error = new ApiError(message, 0, null);
        error.category = CATEGORY_NETWORK;
        return error;
    }

    /**
     * Create a timeout error.
     *
     * @param message The error message
     * @return A new ApiError with TIMEOUT category
     */
    public static ApiError timeoutError(String message) {
        ApiError error = new ApiError(message, 408, null);
        error.category = CATEGORY_TIMEOUT;
        return error;
    }

    /**
     * Create an authentication error.
     *
     * @param message The error message
     * @return A new ApiError with AUTH category
     */
    public static ApiError authError(String message) {
        ApiError error = new ApiError(message, 401, null);
        error.category = CATEGORY_AUTH;
        return error;
    }

    /**
     * Create an LLM service error.
     *
     * @param message The error message
     * @return A new ApiError with LLM category
     */
    public static ApiError llmError(String message) {
        ApiError error = new ApiError(message, 503, null);
        error.category = CATEGORY_LLM;
        return error;
    }

    /**
     * Creates a standard "Not Found" error.
     *
     * @param resourceType The type of resource not found
     * @return An ApiError for the not found condition
     */
    public static ApiError notFound(String resourceType) {
        return new ApiError(resourceType + " not found", 404, null);
    }

    /**
     * Creates a standard "Unauthorized" error with no message.
     *
     * @return An ApiError for the unauthorized condition
     */
    public static ApiError unauthorized() {
        return authError("Unauthorized access");
    }

    /**
     * Creates a standard "Network Error" error with no message.
     *
     * @return An ApiError for network problems
     */
    public static ApiError networkError() {
        return networkError("Network error");
    }

    /**
     * Creates a standard "Server Error" error.
     *
     * @return An ApiError for server problems
     */
    public static ApiError serverError() {
        return new ApiError("Server error", 500, null);
    }

    /**
     * Creates a standard "Timeout" error with no message.
     *
     * @return An ApiError for timeout problems
     */
    public static ApiError timeout() {
        return timeoutError("Request timed out");
    }

    /**
     * Get the error message.
     *
     * @return The error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the HTTP status code.
     *
     * @return The HTTP status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get additional error details.
     *
     * @return Additional error details or an empty map if none
     */
    public Map<String, Object> getDetails() {
        return details != null ? details : Collections.emptyMap();
    }

    /**
     * Get the error category.
     *
     * @return The error category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get a user-friendly error message.
     *
     * @return A user-friendly error message
     */
    public String getUserFriendlyMessage() {
        switch (category) {
            case CATEGORY_NETWORK:
                return "Network connection issue. Please check your internet connection.";
            case CATEGORY_AUTH:
                return "Authentication error. Please log in again.";
            case CATEGORY_TIMEOUT:
                return "Request timed out. The server took too long to respond.";
            case CATEGORY_LLM:
                return "AI service issue. There was a problem with the AI suggestion service.";
            case CATEGORY_SERVER:
                return "Server error. Please try again later.";
            case CATEGORY_VALIDATION:
                return "Invalid data. " + (message != null ? message : "Please check your input.");
            case CATEGORY_CLIENT:
                return "Application error. " + (message != null ? message : "");
            default:
                return message != null ? message : "An unknown error occurred.";
        }
    }

    /**
     * Determine the error category based on HTTP status code and message.
     *
     * @param code HTTP status code
     * @param message Error message
     * @return Error category
     */
    private String determineCategory(int code, String message) {
        if (code == 0 && message != null &&
            (message.contains("connect") || message.contains("host") ||
             message.contains("network") || message.contains("address"))) {
            return CATEGORY_NETWORK;
        }

        if (code == 401 || code == 403) {
            return CATEGORY_AUTH;
        }

        if (code == 408 || (message != null && message.toLowerCase().contains("timeout"))) {
            return CATEGORY_TIMEOUT;
        }

        if (code >= 500) {
            return CATEGORY_SERVER;
        }

        if (code >= 400 && code < 500) {
            return CATEGORY_CLIENT;
        }

        if (message != null && (message.contains("validation") ||
                               message.contains("invalid") ||
                               message.contains("required"))) {
            return CATEGORY_VALIDATION;
        }

        if (message != null && message.toLowerCase().contains("llm")) {
            return CATEGORY_LLM;
        }

        return CATEGORY_UNKNOWN;
    }

    @Override
    public String toString() {
        return "ApiError{" +
                "category='" + category + '\'' +
                ", message='" + message + '\'' +
                ", code=" + code +
                ", details=" + details +
                '}';
    }
}
