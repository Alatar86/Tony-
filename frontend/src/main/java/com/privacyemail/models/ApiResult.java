package com.privacyemail.models;

/**
 * Generic wrapper class for API responses.
 * Allows handling of both success and error responses in a unified way.
 *
 * @param <T> The type of data returned on success
 */
public class ApiResult<T> {
    private final boolean success;
    private final T data;
    private final ApiError error;

    /**
     * Creates a successful result with data.
     *
     * @param data The result data
     */
    private ApiResult(T data) {
        this.success = true;
        this.data = data;
        this.error = null;
    }

    /**
     * Creates a failed result with an error.
     *
     * @param error The error details
     */
    private ApiResult(ApiError error) {
        this.success = false;
        this.data = null;
        this.error = error;
    }

    /**
     * Create a successful result with data.
     *
     * @param <T> The type of data
     * @param data The data
     * @return A successful ApiResult
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(data);
    }

    /**
     * Create a failed result with an error.
     *
     * @param <T> The type of data (unused in error case)
     * @param error The error
     * @return A failed ApiResult
     */
    public static <T> ApiResult<T> failure(ApiError error) {
        return new ApiResult<>(error);
    }

    /**
     * Checks if the operation was successful.
     *
     * @return true if successful, false on error
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the result data.
     * Should only be called if isSuccess() returns true.
     *
     * @return The result data, or null if the operation failed
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the error details.
     * Should only be called if isSuccess() returns false.
     *
     * @return The error details, or null if the operation was successful
     */
    public ApiError getError() {
        return error;
    }

    /**
     * Get a user-friendly error message (if not successful).
     *
     * @return User-friendly error message or null if successful
     */
    public String getErrorMessage() {
        return error != null ? error.getUserFriendlyMessage() : null;
    }
}
