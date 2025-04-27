package com.privacyemail.api;

import com.privacyemail.models.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface abstraction for {@link ApiClient} to facilitate dependency inversion
 * and easier testing/mocking.
 */
public interface IApiClient {
    ApiResult<Boolean> checkAuthStatus() throws IOException, InterruptedException;
    ApiResult<Boolean> initiateLogin() throws IOException, InterruptedException;
    ApiResult<StatusResponse> getBackendStatus() throws IOException, InterruptedException;
    ApiResult<List<EmailMetadata>> getEmailList(String labelId) throws IOException, InterruptedException;
    ApiResult<List<EmailMetadata>> getEmailList() throws IOException, InterruptedException;
    ApiResult<EmailDetails> getEmailDetails(String messageId) throws IOException, InterruptedException;
    ApiResult<SuggestionResponse> getSuggestions(String messageId);
    ApiResult<Map<String, Object>> archiveEmail(String messageId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> deleteEmail(String messageId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> sendEmail(String to, String subject, String body, String replyToId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> sendEmail(String to, String subject, String body) throws IOException, InterruptedException;
    ApiResult<ConfigData> getConfig() throws IOException, InterruptedException;
    ApiResult<Boolean> saveConfig(ConfigData configData) throws IOException, InterruptedException;
    ApiResult<String> getUserSignature() throws IOException, InterruptedException;
    ApiResult<List<String>> getMessagesInThread(String threadId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> modifyEmailLabels(String messageId, List<String> addLabelIds, List<String> removeLabelIds) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> markEmailAsRead(String messageId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> markEmailAsUnread(String messageId) throws IOException, InterruptedException;
    ApiResult<Boolean> verifyAuth();
    ApiResult<ConfigData> fetchConfiguration();
    ApiResult<Boolean> saveUserSignature(String signature) throws IOException, InterruptedException;
}
