package com.privacyemail.models;

/**
 * Represents key email headers.
 * Needs further definition based on actual data requirements.
 */
public class EmailHeaders {
    private String messageId; // Often different from the internal email ID
    private String subject;
    private String fromAddress;
    private String dateSent;
    // Add other relevant headers as needed

    // Constructors
    public EmailHeaders() {}

    // Getters and Setters for all fields (omitted for brevity)
    // TODO: Implement getters and setters

    @Override
    public String toString() {
        return "EmailHeaders{" +
                "messageId='" + messageId + '\'' +
                ", subject='" + subject + '\'' +
                ", from='" + fromAddress + '\'' +
                ", dateSent='" + dateSent + '\'' +
                '}';
    }
}
