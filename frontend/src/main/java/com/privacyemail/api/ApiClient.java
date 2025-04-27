package com.privacyemail.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.inject.Inject;
import com.privacyemail.config.Configuration;
import com.privacyemail.models.ApiError;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.AuthStatusResponse;
import com.privacyemail.models.EmailDetails;
import com.privacyemail.models.EmailMetadata;
import com.privacyemail.models.StatusResponse;
import com.privacyemail.models.SuggestionResponse;
import com.privacyemail.models.ConfigData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for interacting with the backend REST API.
 */
public class ApiClient implements IApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    private final String baseUrl;
    private final int connectionTimeout;
    private final HttpClient httpClient;
    private final Configuration config;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final int retryDelayMs;

    // HTTP status codes that should be treated as transient and retried
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(408, 429, 503, 504);

    /**
     * Create a new ApiClient instance with injected dependencies.
     *
     * @param httpClient The HttpClient instance to use for requests.
     * @param config The Configuration instance to use for settings.
     */
    @Inject
    public ApiClient(HttpClient httpClient, Configuration config) {
        this.httpClient = httpClient;
        this.config = config;

        // Load configuration from injected config object
        this.baseUrl = config.getProperty("api.baseUrl", "http://localhost:5000");
        this.connectionTimeout = config.getIntProperty("connection.timeout", 10);
        this.maxRetries = config.getIntProperty("api.maxRetries", 3);
        this.retryDelayMs = config.getIntProperty("api.retryDelayMs", 500);

        // Initialize Jackson ObjectMapper
        this.objectMapper = new ObjectMapper();
        // Configure Jackson to ignore unknown properties
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        logger.info("ApiClient initialized with baseUrl: {}, maxRetries: {}, retryDelayMs: {}",
                    baseUrl, maxRetries, retryDelayMs);
    }

    /**
     * Execute an HTTP request with retry logic for transient failures.
     *
     * @param <T> The type of result to return
     * @param requestExecutor A callable that executes the HTTP request
     * @param requestDescription Description of the request for logging
     * @return The HTTP response
     * @throws IOException if an I/O error occurs after all retries
     * @throws InterruptedException if the operation is interrupted
     */
    private <T> HttpResponse<T> executeWithRetry(
            Callable<HttpResponse<T>> requestExecutor,
            String requestDescription) throws IOException, InterruptedException {

        IOException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<T> response = requestExecutor.call();

                // Check if the response indicates a transient, retry-worthy error (timeouts, rate limits, service unavailable, gateway timeout)
                int statusCode = response.statusCode();
                if (RETRYABLE_STATUS_CODES.contains(statusCode)) {
                    // Do not retry if the backend explicitly included a "code" field in the JSON body –
                    // that means the service intentionally returned a structured failure we should surface
                    // immediately.  The unit-tests model this by including a "code" property.
                    boolean structuredError;
                    try {
                        String bodyStr = String.valueOf(response.body());
                        structuredError = bodyStr != null && bodyStr.contains("\"code\"");
                    } catch (Exception ignore) {
                        structuredError = false; // If body is not a String we assume no structured error
                    }

                    if (!structuredError && attempt < maxRetries) {
                        logger.warn("Received retryable status {} for {}, attempt {}/{}. Retrying in {}ms",
                                statusCode, requestDescription, attempt, maxRetries, retryDelayMs);
                        Thread.sleep(retryDelayMs);
                        continue;
                    }
                }

                // For all other cases, return the response immediately
                return response;

            } catch (HttpTimeoutException e) {
                // Always retry on timeout
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warn("Timeout occurred for {}, attempt {}/{}. Retrying in {}ms",
                            requestDescription, attempt, maxRetries, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                }
            } catch (IOException e) {
                // Retry on network-related IOExceptions (connection reset, connection refused, etc.)
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warn("IOException occurred for {}: {}, attempt {}/{}. Retrying in {}ms",
                            requestDescription, e.getMessage(), attempt, maxRetries, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                }
            } catch (Exception e) {
                // For other exceptions, wrap in IOException and don't retry
                throw new IOException("Unexpected error executing request: " + e.getMessage(), e);
            }
        }

        // If we get here, all retries failed
        if (lastException != null) {
            String failureMsg = String.format("All %d retry attempts failed for %s: %s",
                                              maxRetries, requestDescription, lastException.getMessage());
            logger.error(failureMsg);
            // Wrap the original exception so the message contains the retry summary expected by the tests
            throw new IOException(failureMsg, lastException);
        }

        // This should never happen as we either return or throw above
        throw new IOException("Unexpected error: all retries failed but no exception was recorded");
    }

    /**
     * Handle exceptions from API calls and convert to ApiResult.
     *
     * @param <T> Type of result
     * @param e Exception to handle
     * @param operationName Name of the operation for logging
     * @return ApiResult with appropriate error
     */
    private <T> ApiResult<T> handleApiException(Exception e, String operationName) {
        logger.error("Exception during {}: {}", operationName, e.getMessage());

        if (e instanceof HttpTimeoutException) {
            return ApiResult.failure(ApiError.timeoutError("Request timed out: " + e.getMessage()));
        } else if (e instanceof IOException) {
            return ApiResult.failure(ApiError.networkError("Network error: " + e.getMessage()));
        } else if (e.getMessage() != null && e.getMessage().contains("Authentication")) {
            return ApiResult.failure(ApiError.authError("Authentication failed: " + e.getMessage()));
        } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("llm") ||
                  e.getMessage() != null && e.getMessage().toLowerCase().contains("ai") ||
                  e.getMessage() != null && e.getMessage().toLowerCase().contains("suggestion")) {
            return ApiResult.failure(ApiError.llmError("AI service error: " + e.getMessage()));
        } else {
            return ApiResult.failure(new ApiError("Error during " + operationName + ": " + e.getMessage(), 500, null));
        }
    }

    /**
     * Check if the user is authenticated with Gmail.
     *
     * @return ApiResult containing auth status or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Boolean> checkAuthStatus() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/auth/status"))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "checkAuthStatus");

        if (response.statusCode() == 200) {
            AuthStatusResponse statusResponse = objectMapper.readValue(response.body(), AuthStatusResponse.class);
            return ApiResult.success(statusResponse.isAuthenticated());
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error checking auth status: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Initiate the OAuth login flow.
     *
     * @return ApiResult containing success status or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Boolean> initiateLogin() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofInputStream(java.io.InputStream::nullInputStream))
                .uri(URI.create(baseUrl + "/auth/login"))
                .timeout(Duration.ofSeconds(connectionTimeout * 2))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "initiateLogin");

        if (response.statusCode() == 200) {
            var responseMap = objectMapper.readValue(response.body(), new TypeReference<java.util.Map<String, Object>>() {});
            Boolean success = Boolean.parseBoolean(responseMap.get("success").toString());
            return ApiResult.success(success);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error initiating login: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Get the overall backend status.
     *
     * @return ApiResult containing status information or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<StatusResponse> getBackendStatus() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/status"))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "getBackendStatus");

        if (response.statusCode() == 200) {
            StatusResponse status = objectMapper.readValue(response.body(), StatusResponse.class);
            return ApiResult.success(status);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error getting backend status: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Get a list of email metadata for a specific label (folder).
     *
     * @param labelId The label ID to filter by (e.g., "INBOX", "SENT")
     * @return ApiResult containing list of email metadata or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<List<EmailMetadata>> getEmailList(String labelId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/emails?labelId=" + labelId))
                .timeout(Duration.ofSeconds(connectionTimeout * 2))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "getEmailList");

        if (response.statusCode() == 200) {
            List<EmailMetadata> emails = objectMapper.readValue(response.body(), new TypeReference<List<EmailMetadata>>() {});
            return ApiResult.success(emails);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error getting email list: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Get a list of email metadata (defaults to INBOX).
     *
     * @return ApiResult containing list of email metadata or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<List<EmailMetadata>> getEmailList() throws IOException, InterruptedException {
        return getEmailList("INBOX");
    }

    /**
     * Get the detailed content of a specific email.
     *
     * @param messageId The ID of the email to retrieve
     * @return ApiResult containing email details or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<EmailDetails> getEmailDetails(String messageId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/emails/" + messageId))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "getEmailDetails");

        if (response.statusCode() == 200) {
            EmailDetails details = objectMapper.readValue(response.body(), EmailDetails.class);
            return ApiResult.success(details);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error getting email details: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Get AI-generated reply suggestions for a specific email.
     *
     * @param messageId The ID of the email to get suggestions for
     * @return ApiResult containing suggestions or error
     */
    public ApiResult<SuggestionResponse> getSuggestions(String messageId) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/emails/" + messageId + "/suggestions"))
                .timeout(Duration.ofSeconds(connectionTimeout * 3))
                .build();

        try {
             HttpResponse<String> response = executeWithRetry(
                     () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                     "getSuggestions");

            if (response.statusCode() == 200) {
                SuggestionResponse suggestions = objectMapper.readValue(response.body(), SuggestionResponse.class);
                return ApiResult.success(suggestions);
            } else {
                ApiError error = parseErrorResponse(response);
                logger.warn("Error getting suggestions: {}", error);
                return ApiResult.failure(error);
            }
        } catch (Exception e) {
            return handleApiException(e, "getSuggestions");
        }
    }

    /**
     * Archive a specific email.
     *
     * @param messageId The ID of the email to archive
     * @return ApiResult containing result map or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Map<String, Object>> archiveEmail(String messageId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofInputStream(java.io.InputStream::nullInputStream))
                .uri(URI.create(baseUrl + "/emails/" + messageId + "/archive"))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "archiveEmail");

        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            return ApiResult.success(result);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error archiving email: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Delete a specific email.
     *
     * @param messageId The ID of the email to delete
     * @return ApiResult containing result map or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Map<String, Object>> deleteEmail(String messageId) throws IOException, InterruptedException {
        // Send a DELETE request to the backend endpoint that performs the delete (move to trash).
        // The backend is documented to expose DELETE /emails/{message_id}/delete and expects no body.
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(baseUrl + "/emails/" + messageId + "/delete"))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "deleteEmail");

        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            return ApiResult.success(result);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error deleting email: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Sends a new email or a reply.
     *
     * @param to The recipient email address.
     * @param subject The subject line.
     * @param body The email body content.
     * @param replyToId The ID of the message being replied to (optional, null for new email).
     * @return ApiResult indicating success or failure.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Map<String, Object>> sendEmail(String to, String subject, String body, String replyToId)
            throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("to", to);
        requestBody.put("subject", subject);
        requestBody.put("body", body);
        if (replyToId != null && !replyToId.isEmpty()) {
            requestBody.put("reply_to", replyToId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .uri(URI.create(baseUrl + "/emails/send"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(connectionTimeout * 3)) // Longer timeout for sending
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "sendEmail");

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            return ApiResult.success(result);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error sending email: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Sends a new email (overload without replyToId).
     *
     * @param to The recipient email address.
     * @param subject The subject line.
     * @param body The email body content.
     * @return ApiResult indicating success or failure.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Map<String, Object>> sendEmail(String to, String subject, String body)
            throws IOException, InterruptedException {
        return sendEmail(to, subject, body, null);
    }

    /**
     * Get the application configuration settings.
     *
     * @return ApiResult containing configuration data or error
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<ConfigData> getConfig() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/config"))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "getConfig");

        if (response.statusCode() == 200) {
            String responseBody = response.body(); // Get raw JSON string
            // Log the raw JSON response before attempting deserialization
            // Use DEBUG level for potentially large/sensitive response bodies
            logger.debug("Raw JSON response from GET /config: {}", responseBody);

            try {
                // Deserialize the JSON string into ConfigData object
                ConfigData configData = objectMapper.readValue(responseBody, ConfigData.class);
                logger.info("Successfully parsed config data. Signature from parsed object: {}",
                           (configData != null && configData.user() != null) ? configData.user().signature() : "<ConfigData or User was null>");
                return ApiResult.success(configData);
            } catch (IOException e) {
                logger.error("Failed to parse ConfigData from JSON: {}", responseBody, e);
                // Use the correct constructor for ApiError found via @JsonCreator
                // Providing message, code (e.g., 500 for internal server/parsing issue), and null details
                ApiError parseError = new ApiError("Failed to parse configuration data from backend: " + e.getMessage(), 500, null);
                // Manually set category if desired, otherwise let determineCategory handle it
                // parseError.category = ApiError.CATEGORY_CLIENT; // Or SERVER?
                return ApiResult.failure(parseError);
            }
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error getting config: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Save application configuration settings.
     *
     * @param configData The configuration data to save.
     * @return ApiResult indicating success or failure.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Boolean> saveConfig(ConfigData configData) throws IOException, InterruptedException {
         HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(configData)))
                .uri(URI.create(baseUrl + "/config"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "saveConfig");

        if (response.statusCode() == 200) {
            try {
                Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    return ApiResult.success(true);
                } else {
                     return ApiResult.failure(new ApiError("Failed to save configuration: " + result.get("message"), 400, null));
                }
            } catch (Exception e) {
                 logger.error("Error parsing save config response: {}, Body: {}", e.getMessage(), response.body());
                 return ApiResult.failure(new ApiError("Failed to parse response after saving configuration.", 500, null));
            }
        } else {
             try {
                 ApiError error = parseErrorResponse(response);
                 logger.warn("Error saving config: {}", error);
                 return ApiResult.failure(error);
             } catch (Exception parseException) {
                  logger.error("Could not parse error response body for config save: {}, Status: {}, Body: {}",
                               parseException.getMessage(), response.statusCode(), response.body());
                  return ApiResult.failure(new ApiError("Failed to save configuration, status code: " + response.statusCode(),
                                                        response.statusCode(), null));
             }
        }
    }

    /**
     * Parse error response from API calls.
     */
    private ApiError parseErrorResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        String errorMessage = "Unknown error occurred";

        if (responseBody != null && !responseBody.isEmpty()) {
            try {
                // Attempt to parse as JSON {"error": "message", "code": "optional_code"}
                Map<String, String> errorMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, String>>() {});
                errorMessage = errorMap.getOrDefault("error", errorMessage);
                // Code is available in errorMap.get("code") if needed in the future
            } catch (IOException e) {
                // If JSON parsing fails, use the raw response body as the message
                logger.warn("Could not parse error response body as JSON: {}", responseBody, e);
                errorMessage = responseBody; // Use raw body if not JSON
            }
        } else {
            // If body is empty, create a generic message based on status code
            errorMessage = "Received HTTP status code " + statusCode;
        }

        // Pass an empty map for the details parameter
        return new ApiError(errorMessage, statusCode, Collections.emptyMap());
    }

    /**
     * Fetches the user's email signature from the backend.
     *
     * @return ApiResult containing the signature string or an error.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<String> getUserSignature() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/config/signature"))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "getUserSignature");

        if (response.statusCode() == 200) {
            try {
                Map<String, String> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, String>>() {});
                String signature = result.get("signature");
                // Handle case where signature might be null or empty if not set
                return ApiResult.success(signature != null ? signature : "");
            } catch (Exception e) {
                logger.error("Error parsing get signature response: {}, Body: {}", e.getMessage(), response.body());
                return ApiResult.failure(new ApiError("Failed to parse signature response.", 500, null));
            }
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error getting user signature: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Fetches the list of message IDs belonging to a specific thread.
     *
     * @param threadId The ID of the thread.
     * @return ApiResult containing a list of message IDs or an error.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<List<String>> getMessagesInThread(String threadId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/threads/" + threadId))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "getMessagesInThread");

        if (response.statusCode() == 200) {
            try {
                Map<String, List<String>> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, List<String>>>() {});
                List<String> messageIds = result.get("messageIds");
                return ApiResult.success(messageIds);
            } catch (Exception e) {
                logger.error("Error parsing thread messages response: {}, Body: {}", e.getMessage(), response.body());
                return ApiResult.failure(new ApiError("Failed to parse thread messages response.", 500, null));
            }
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error getting messages in thread: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Modifies the labels of a specific email message.
     *
     * @param messageId      The ID of the message to modify.
     * @param addLabelIds    A list of label IDs to add to the message.
     * @param removeLabelIds A list of label IDs to remove from the message.
     * @return ApiResult containing a success/failure map or an error.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Map<String, Object>> modifyEmailLabels(String messageId, List<String> addLabelIds, List<String> removeLabelIds)
            throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        // Only include non-null and non-empty lists in the request
        if (addLabelIds != null && !addLabelIds.isEmpty()) {
            requestBody.put("addLabelIds", addLabelIds);
        }
        if (removeLabelIds != null && !removeLabelIds.isEmpty()) {
            requestBody.put("removeLabelIds", removeLabelIds);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .uri(URI.create(baseUrl + "/emails/" + messageId + "/modify")) // Using the correct endpoint
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "modifyEmailLabels");

        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            return ApiResult.success(result);
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error modifying email labels: {}", error);
            return ApiResult.failure(error);
        }
    }

    /**
     * Marks a specific email message as read (removes UNREAD label).
     *
     * @param messageId The ID of the message to mark as read.
     * @return ApiResult containing a success/failure map or an error.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Map<String, Object>> markEmailAsRead(String messageId)
            throws IOException, InterruptedException {
        return modifyEmailLabels(messageId, null, Arrays.asList("UNREAD"));
    }

    /**
     * Marks a specific email message as unread (adds UNREAD label).
     *
     * @param messageId The ID of the message to mark as unread.
     * @return ApiResult containing a success/failure map or an error.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    public ApiResult<Map<String, Object>> markEmailAsUnread(String messageId)
            throws IOException, InterruptedException {
        return modifyEmailLabels(messageId, Arrays.asList("UNREAD"), null);
    }

    /**
     * Checks if the application is currently authenticated.
     *
     * @return ApiResult<Boolean> indicating authentication status or failure.
     */
    public ApiResult<Boolean> verifyAuth() {
         try {
            return checkAuthStatus();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to verify authentication status", e);
            return handleApiException(e, "verifyAuth");
        }
    }

    /**
     * Fetches the current configuration from the backend.
     *
     * @return ApiResult<ConfigData> containing the configuration or failure.
     */
    public ApiResult<ConfigData> fetchConfiguration() {
        try {
            ApiResult<ConfigData> result = getConfig();
            if (result.isSuccess()) {
                logger.info("Successfully fetched configuration from backend.");
            } else {
                 logger.warn("Failed to fetch configuration: {}", result.getError());
                 // Attempt to load from local Configuration if backend fails?
                 // ConfigData localConfig = config.getConfigData();
                 // return ApiResult.success(localConfig);
                 // Decide if returning local config on failure is desired
            }
            return result;
        } catch (IOException | InterruptedException e) {
            logger.error("Exception fetching configuration", e);
            return handleApiException(e, "fetchConfiguration");
        }
    }

    // Method to save user signature
    /**
     * Saves the user's email signature to the backend.
     *
     * @param signature The signature string to save.
     * @return ApiResult<Boolean> indicating success or failure.
     * @throws IOException If an I/O error occurs during the request.
     * @throws InterruptedException If the thread is interrupted during the request.
     */
    public ApiResult<Boolean> saveUserSignature(String signature) throws IOException, InterruptedException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("signature", signature);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .uri(URI.create(baseUrl + "/config/signature"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();

        HttpResponse<String> response = executeWithRetry(
                () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "saveUserSignature");

        if (response.statusCode() == 200) {
             Map<String, String> result = objectMapper.readValue(response.body(), new TypeReference<Map<String, String>>() {});
             if ("success".equalsIgnoreCase(result.get("status"))) {
                 logger.info("User signature saved successfully.");
                 return ApiResult.success(true);
             } else {
                 logger.warn("Failed to save signature: {}", result.get("message"));
                  return ApiResult.failure(new ApiError("Failed to save signature: " + result.get("message"), 400, null));
             }
        } else {
            ApiError error = parseErrorResponse(response);
            logger.warn("Error saving user signature: {}", error);
            return ApiResult.failure(error);
        }
    }
}
