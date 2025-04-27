package com.privacyemail.models;

/**
 * Represents the basic structure of an email message.
 * Needs further definition based on actual data requirements.
 */
public class Email {
    private String id;
    private String subject;
    private String fromAddress;
    private String date;

    // Constructors (Default and potentially parameterized)
    public Email() {}

    // Getters and Setters for all fields (omitted for brevity)
    // TODO: Implement getters and setters

    @Override
    public String toString() {
        return "Email{" +
                "id='" + id + '\'' +
                ", subject='" + subject + '\'' +
                ", from='" + fromAddress + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
