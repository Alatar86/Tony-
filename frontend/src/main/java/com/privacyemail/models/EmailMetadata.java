package com.privacyemail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents email message metadata.
 * Maps to the backend API response for email list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailMetadata {

    private String id;
    private String subject;

    @JsonProperty("from")
    private String fromAddress;

    private String date;

    @JsonProperty("labelIds")
    private List<String> labelIds;

    /**
     * Default constructor for JSON deserialization.
     */
    public EmailMetadata() {
    }

    /**
     * Create a new EmailMetadata instance.
     *
     * @param id Message ID
     * @param subject Message subject
     * @param fromAddress Sender address
     * @param date Message date
     * @param labelIds List of label IDs assigned to this message
     */
    public EmailMetadata(String id, String subject, String fromAddress, String date, List<String> labelIds) {
        this.id = id;
        this.subject = subject;
        this.fromAddress = fromAddress;
        this.date = date;
        this.labelIds = labelIds;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getLabelIds() {
        return labelIds;
    }

    public void setLabelIds(List<String> labelIds) {
        this.labelIds = labelIds;
    }

    /**
     * Check if this email is unread (has the UNREAD label).
     *
     * @return true if the email is unread, false otherwise
     */
    public boolean isUnread() {
        return labelIds != null && labelIds.contains("UNREAD");
    }

    /**
     * Update the unread status by adding or removing the UNREAD label.
     *
     * @param unread Whether the email should be marked as unread
     */
    public void setUnread(boolean unread) {
        if (labelIds == null) {
            return;
        }

        if (unread && !labelIds.contains("UNREAD")) {
            labelIds.add("UNREAD");
        } else if (!unread && labelIds.contains("UNREAD")) {
            labelIds.remove("UNREAD");
        }
    }

    @Override
    public String toString() {
        return subject + " - " + fromAddress + " (" + date + ")";
    }
}
