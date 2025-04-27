package com.privacyemail.services;

import com.privacyemail.models.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IEmailService {
    ApiResult<List<EmailMetadata>> getEmailList(String labelId) throws IOException, InterruptedException;
    ApiResult<EmailDetails> getEmailDetails(String messageId) throws IOException, InterruptedException;
    ApiResult<SuggestionResponse> getSuggestions(String messageId);
    ApiResult<Map<String, Object>> archiveEmail(String messageId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> deleteEmail(String messageId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> markEmailAsRead(String messageId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> markEmailAsUnread(String messageId) throws IOException, InterruptedException;
    ApiResult<Map<String, Object>> sendEmail(String to, String subject, String body, String replyToId) throws IOException, InterruptedException;
}
