package com.privacyemail.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Model class representing email details.
 * Contains all information about an email that plugins might need.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailDetails {

    @JsonProperty("id")
    private String messageId;

    @JsonProperty("thread_id")
    private String threadId;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("from")
    private String sender;

    private List<String> recipients;

    private LocalDateTime timestamp;

    @JsonProperty("plain_content")
    private String plainContent;

    @JsonProperty("html_content")
    private String htmlContent;

    // For storing the body content from the API
    @JsonProperty("body")
    private String body;

    // Flag to indicate if the body content is HTML
    @JsonProperty("is_html")
    private boolean isHtml;

    private List<String> attachmentIds;

    @JsonProperty("labels")
    private List<String> labels;

    private Map<String, Object> metadata;

    /**
     * Creates a new EmailDetails instance.
     */
    public EmailDetails() {
        this.recipients = new ArrayList<>();
        this.attachmentIds = new ArrayList<>();
        this.labels = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Custom setter for the body field from the API.
     * This method sets either htmlContent or plainContent based on the isHtml flag.
     *
     * @param body The email body content from the API
     */
    @JsonSetter("body")
    public void setBodyFromApi(String body) {
        // Store in the body field for backward compatibility
        this.body = body;

        // Set the appropriate content field based on the isHtml flag
        if (this.isHtml) {
            this.htmlContent = body;
            this.plainContent = ""; // Clear plainContent if we're setting HTML
        } else {
            this.plainContent = body;
            this.htmlContent = ""; // Clear htmlContent if we're setting plain text
        }
    }

    /**
     * Gets the unique message ID.
     *
     * @return The message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the unique message ID.
     *
     * @param messageId The message ID
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Gets the thread ID.
     *
     * @return The thread ID
     */
    public String getThreadId() {
        return threadId;
    }

    /**
     * Sets the thread ID.
     *
     * @param threadId The thread ID
     */
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    /**
     * Gets the email subject.
     *
     * @return The subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the email subject.
     *
     * @param subject The subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Gets the sender email address.
     *
     * @return The sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * Sets the sender email address.
     *
     * @param sender The sender
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * Gets the list of recipient email addresses.
     *
     * @return An unmodifiable list of recipients
     */
    public List<String> getRecipients() {
        return Collections.unmodifiableList(recipients);
    }

    /**
     * Adds a recipient email address.
     *
     * @param recipient The recipient to add
     */
    public void addRecipient(String recipient) {
        if (recipient != null && !recipient.isEmpty()) {
            this.recipients.add(recipient);
        }
    }

    /**
     * Sets the list of recipient email addresses.
     *
     * @param recipients The list of recipients
     */
    public void setRecipients(List<String> recipients) {
        this.recipients.clear();
        if (recipients != null) {
            this.recipients.addAll(recipients);
        }
    }

    /**
     * Gets the email timestamp.
     *
     * @return The timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the email timestamp.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the plain text content.
     *
     * @return The plain text content
     */
    public String getPlainContent() {
        return plainContent;
    }

    /**
     * Sets the plain text content.
     *
     * @param plainContent The plain text content
     */
    public void setPlainContent(String plainContent) {
        this.plainContent = plainContent;
    }

    /**
     * Gets the HTML content.
     *
     * @return The HTML content
     */
    public String getHtmlContent() {
        return htmlContent;
    }

    /**
     * Sets the HTML content.
     *
     * @param htmlContent The HTML content
     */
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    /**
     * Gets the email body content.
     * This method first tries to return HTML content, and falls back to plain content if HTML is not available.
     * This is provided for compatibility with existing code.
     *
     * @return The email body content (HTML or plain)
     */
    public String getBody() {
        // If we got a body directly from the API, use that
        if (body != null && !body.isEmpty()) {
            return body;
        }

        // Otherwise, try HTML content first, then plain text
        if (htmlContent != null && !htmlContent.isEmpty()) {
            return htmlContent;
        }
        return plainContent != null ? plainContent : "";
    }

    /**
     * Checks if this email has HTML content.
     * This is an alias for hasHtmlContent() to maintain compatibility with existing code.
     *
     * @return true if the email has HTML content, false otherwise
     */
    public boolean isHtml() {
        // If the API explicitly told us if the content is HTML, use that
        if (body != null && !body.isEmpty()) {
            return isHtml;
        }

        // Otherwise, check if we have HTML content
        return hasHtmlContent();
    }

    /**
     * Gets the formatted 'from' address for display.
     *
     * @return The from address
     */
    public String getFromAddress() {
        return sender != null ? sender : "";
    }

    /**
     * Gets the ID of this email.
     * This is an alias for getMessageId() to maintain compatibility with existing code.
     *
     * @return The message ID
     */
    @JsonIgnore
    public String getId() {
        return messageId;
    }

    /**
     * Checks if this email has HTML content.
     *
     * @return true if HTML content is present and not empty
     */
    public boolean hasHtmlContent() {
        return htmlContent != null && !htmlContent.isEmpty();
    }

    /**
     * Checks if this email has plain text content.
     *
     * @return true if plain text content is present and not empty
     */
    public boolean hasPlainContent() {
        return plainContent != null && !plainContent.isEmpty();
    }

    /**
     * Checks if this email has attachments.
     *
     * @return true if attachments are present
     */
    public boolean hasAttachments() {
        return attachmentIds != null && !attachmentIds.isEmpty();
    }

    /**
     * Gets a formatted date string for display.
     *
     * @return The formatted date
     */
    public String getDate() {
        // Simple date format for display
        return timestamp != null ? timestamp.toString().replace('T', ' ') : "";
    }

    /**
     * Gets the list of attachment IDs.
     *
     * @return An unmodifiable list of attachment IDs
     */
    public List<String> getAttachmentIds() {
        return Collections.unmodifiableList(attachmentIds);
    }

    /**
     * Adds an attachment ID.
     *
     * @param attachmentId The attachment ID to add
     */
    public void addAttachmentId(String attachmentId) {
        if (attachmentId != null && !attachmentId.isEmpty()) {
            this.attachmentIds.add(attachmentId);
        }
    }

    /**
     * Sets the list of attachment IDs.
     *
     * @param attachmentIds The list of attachment IDs
     */
    public void setAttachmentIds(List<String> attachmentIds) {
        this.attachmentIds.clear();
        if (attachmentIds != null) {
            this.attachmentIds.addAll(attachmentIds);
        }
    }

    /**
     * Gets the list of labels.
     *
     * @return An unmodifiable list of labels
     */
    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    /**
     * Adds a label.
     *
     * @param label The label to add
     */
    public void addLabel(String label) {
        if (label != null && !label.isEmpty()) {
            this.labels.add(label);
        }
    }

    /**
     * Sets the list of labels.
     *
     * @param labels The list of labels
     */
    public void setLabels(List<String> labels) {
        this.labels.clear();
        if (labels != null) {
            this.labels.addAll(labels);
        }
    }

    /**
     * Gets a metadata value.
     *
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Sets a metadata value.
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            this.metadata.put(key, value);
        }
    }

    /**
     * Gets all metadata.
     *
     * @return An unmodifiable map of all metadata
     */
    public Map<String, Object> getAllMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
}
