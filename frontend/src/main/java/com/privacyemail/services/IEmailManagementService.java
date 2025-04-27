package com.privacyemail.services;

import com.privacyemail.models.*;
import javafx.concurrent.Task;
import java.util.List;
import java.util.Map;

public interface IEmailManagementService {
    Task<ApiResult<List<EmailMetadata>>> fetchEmails(String labelId);
    Task<ApiResult<EmailDetails>> loadEmailDetails(String emailId);
    Task<ApiResult<Map<String, Object>>> archiveEmail(String emailId);
    Task<ApiResult<Map<String, Object>>> deleteEmail(String emailId);
    Task<ApiResult<Map<String, Object>>> markEmailAsRead(String emailId);
    Task<ApiResult<Map<String, Object>>> markEmailAsUnread(String emailId);
    Task<ApiResult<Map<String, Object>>> modifyEmailLabels(String emailId, List<String> addLabelIds, List<String> removeLabelIds);
    Task<ApiResult<SuggestionResponse>> suggestReply(String emailId);
    Task<ApiResult<Map<String, Object>>> sendEmail(String to, String subject, String body, String replyToId);
}
