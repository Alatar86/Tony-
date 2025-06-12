package com.privacyemail.services;

import com.google.inject.Inject;
import com.privacyemail.api.IApiClient;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.EmailDetails;
import com.privacyemail.models.EmailMetadata;
import com.privacyemail.models.SuggestionResponse;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for managing email-related operations.
 */
public class EmailManagementService implements IEmailManagementService {
    private static final Logger logger = LoggerFactory.getLogger(EmailManagementService.class);

    private final IApiClient apiClient;
    private final ExecutorService executorService;

    /**
     * Constructs an EmailManagementService with the required dependencies.
     *
     * @param apiClient The API client for backend communication
     * @param executorService The executor service for asynchronous tasks
     */
    @Inject
    public EmailManagementService(IApiClient apiClient, ExecutorService executorService) {
        this.apiClient = apiClient;
        this.executorService = executorService;
    }

    /**
     * Fetches emails for a specific folder.
     *
     * @param labelId The label ID of the folder to fetch emails from
     * @return A Task that returns an ApiResult<List<EmailMetadata>> with the list of emails
     */
    public Task<ApiResult<List<EmailMetadata>>> fetchEmails(String labelId) {
        logger.info("Fetching emails for label: {}", labelId);

        Task<ApiResult<List<EmailMetadata>>> task = new Task<>() {
            @Override
            protected ApiResult<List<EmailMetadata>> call() throws Exception {
                return apiClient.getEmailList(labelId);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Loads details for a specific email.
     *
     * @param emailId The ID of the email to load details for
     * @return A Task that returns an ApiResult<EmailDetails> with the email details
     */
    public Task<ApiResult<EmailDetails>> loadEmailDetails(String emailId) {
        logger.info("Loading details for email: {}", emailId);

        Task<ApiResult<EmailDetails>> task = new Task<>() {
            @Override
            protected ApiResult<EmailDetails> call() throws Exception {
                return apiClient.getEmailDetails(emailId);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Archives a specific email.
     *
     * @param emailId The ID of the email to archive
     * @return A Task that returns an ApiResult<Map<String, Object>> indicating success or failure
     */
    public Task<ApiResult<Map<String, Object>>> archiveEmail(String emailId) {
        logger.info("Archiving email: {}", emailId);

        Task<ApiResult<Map<String, Object>>> task = new Task<>() {
            @Override
            protected ApiResult<Map<String, Object>> call() throws Exception {
                return apiClient.archiveEmail(emailId);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Deletes a specific email.
     *
     * @param emailId The ID of the email to delete
     * @return A Task that returns an ApiResult<Map<String, Object>> indicating success or failure
     */
    public Task<ApiResult<Map<String, Object>>> deleteEmail(String emailId) {
        logger.info("Deleting email: {}", emailId);

        Task<ApiResult<Map<String, Object>>> task = new Task<>() {
            @Override
            protected ApiResult<Map<String, Object>> call() throws Exception {
                return apiClient.deleteEmail(emailId);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Marks a specific email as read.
     *
     * @param emailId The ID of the email to mark as read
     * @return A Task that returns an ApiResult<Map<String, Object>> indicating success or failure
     */
    public Task<ApiResult<Map<String, Object>>> markEmailAsRead(String emailId) {
        logger.info("Marking email as read: {}", emailId);

        Task<ApiResult<Map<String, Object>>> task = new Task<>() {
            @Override
            protected ApiResult<Map<String, Object>> call() throws Exception {
                return apiClient.markEmailAsRead(emailId);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Marks a specific email as unread.
     *
     * @param emailId The ID of the email to mark as unread
     * @return A Task that returns an ApiResult<Map<String, Object>> indicating success or failure
     */
    public Task<ApiResult<Map<String, Object>>> markEmailAsUnread(String emailId) {
        logger.info("Marking email as unread: {}", emailId);

        Task<ApiResult<Map<String, Object>>> task = new Task<>() {
            @Override
            protected ApiResult<Map<String, Object>> call() throws Exception {
                return apiClient.markEmailAsUnread(emailId);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Modifies labels for a specific email.
     *
     * @param emailId The ID of the email to modify labels for
     * @param addLabelIds The list of label IDs to add
     * @param removeLabelIds The list of label IDs to remove
     * @return A Task that returns an ApiResult<Map<String, Object>> indicating success or failure
     */
    public Task<ApiResult<Map<String, Object>>> modifyEmailLabels(String emailId, List<String> addLabelIds, List<String> removeLabelIds) {
        logger.info("Modifying labels for email: {}", emailId);

        Task<ApiResult<Map<String, Object>>> task = new Task<>() {
            @Override
            protected ApiResult<Map<String, Object>> call() throws Exception {
                return apiClient.modifyEmailLabels(emailId, addLabelIds, removeLabelIds);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Gets AI-generated reply suggestions for a specific email.
     *
     * @param emailId The ID of the email to get suggestions for
     * @return A Task that returns an ApiResult<SuggestionResponse> with the suggestions
     */
    public Task<ApiResult<SuggestionResponse>> suggestReply(String emailId) {
        logger.info("Getting reply suggestions for email: {}", emailId);

        Task<ApiResult<SuggestionResponse>> task = new Task<>() {
            @Override
            protected ApiResult<SuggestionResponse> call() throws Exception {
                return apiClient.getSuggestions(emailId);
            }
        };

        executorService.submit(task);
        return task;
    }

    /**
     * Sends an email.
     *
     * @param to The recipient email address
     * @param subject The subject line
     * @param body The email body content
     * @param replyToId The ID of the message being replied to (null for new email)
     * @return A Task that returns an ApiResult<Map<String, Object>> indicating success or failure
     */
    public Task<ApiResult<Map<String, Object>>> sendEmail(String to, String subject, String body, String replyToId) {
        logger.info("Sending email to: {}", to);

        Task<ApiResult<Map<String, Object>>> task = new Task<>() {
            @Override
            protected ApiResult<Map<String, Object>> call() throws Exception {
                return apiClient.sendEmail(to, subject, body, replyToId);
            }
        };

        executorService.submit(task);
        return task;
    }
}
